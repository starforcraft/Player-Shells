package com.ultramega.playershells.commands;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.MathUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class ShellRecoveryCommands {
    private static final SimpleCommandExceptionType SINGLE_OWNER = new SimpleCommandExceptionType(Component.literal("Select exactly one shell owner."));
    private static final SimpleCommandExceptionType SHELL_NOT_FOUND = new SimpleCommandExceptionType(Component.literal("No archived shell with that UUID belongs to this player."));
    private static final SimpleCommandExceptionType FORGE_UNAVAILABLE = new SimpleCommandExceptionType(Component.literal("Choose an empty, unoccupied Shell Forge."));

    private ShellRecoveryCommands() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playershells").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("archive")
                .then(Commands.argument("owner", GameProfileArgument.gameProfile()).executes(ShellRecoveryCommands::list)))
            .then(Commands.literal("restore")
                .then(Commands.argument("owner", GameProfileArgument.gameProfile())
                    .then(Commands.argument("shell", UuidArgument.uuid())
                        .suggests(ShellRecoveryCommands::suggestShells)
                        .executes(context -> restore(context, false))
                        .then(Commands.argument("forge", BlockPosArgument.blockPos()).executes(context -> restore(context, true)))))));
    }

    private static int list(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final UUID owner = getOwner(context);
        final var shells = ShellSavedData.getShellData(context.getSource().getLevel()).getArchived(owner);
        context.getSource().sendSuccess(() -> Component.literal("Archived shells: " + shells.size()), false);
        for (final ShellState shell : shells) {
            context.getSource().sendSuccess(() -> Component.literal(shell.shellUUID() + " — " + archivedLocation(shell)), false);
        }
        return shells.size();
    }

    private static CompletableFuture<Suggestions> suggestShells(final CommandContext<CommandSourceStack> context,
                                                               final SuggestionsBuilder builder) {
        final UUID owner;
        try {
            owner = getOwner(context);
        } catch (CommandSyntaxException ignored) {
            return builder.buildFuture();
        }

        final String prefix = builder.getRemainingLowerCase();
        for (final ShellState shell : ShellSavedData.getShellData(context.getSource().getLevel()).getArchived(owner)) {
            final String uuid = shell.shellUUID().toString();
            if (uuid.startsWith(prefix)) {
                builder.suggest(uuid, Component.literal(archivedLocation(shell)));
            }
        }
        return builder.buildFuture();
    }

    private static String archivedLocation(final ShellState shell) {
        final var position = shell.shellForgePos();
        return position.dimension() + " at " + position.pos().toShortString();
    }

    private static int restore(final CommandContext<CommandSourceStack> context, final boolean alternateForge) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final UUID owner = getOwner(context);
        final UUID shellUuid = UuidArgument.getUuid(context, "shell");
        final ShellState archived = ShellSavedData.getShellData(source.getLevel()).getArchived(owner).stream()
            .filter(shell -> shell.shellUUID().equals(shellUuid)).findFirst().orElseThrow(SHELL_NOT_FOUND::create);

        final ServerLevel level = alternateForge ? source.getLevel() : MathUtils.findTargetLevel(source.getServer(), archived.shellForgePos());
        final BlockPos position = alternateForge ? BlockPosArgument.getBlockPos(context, "forge") : archived.shellForgePos().pos();
        if (level == null || !level.isInWorldBounds(position)
            || !(level.getChunkAt(position).getBlockEntity(position) instanceof ShellForgeBlockEntity forge)
            || !forge.restoreShell(owner, shellUuid)) {
            throw FORGE_UNAVAILABLE.create();
        }

        source.sendSuccess(() -> Component.literal("Restored shell " + shellUuid + " at " + position.toShortString()), true);
        return 1;
    }

    private static UUID getOwner(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var owners = GameProfileArgument.getGameProfiles(context, "owner");
        if (owners.size() != 1) {
            throw SINGLE_OWNER.create();
        }
        return owners.iterator().next().id();
    }
}
