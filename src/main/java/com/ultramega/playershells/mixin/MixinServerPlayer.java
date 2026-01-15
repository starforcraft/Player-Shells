package com.ultramega.playershells.mixin;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.network.ModNetworking;
import com.ultramega.playershells.packet.c2s.TransferPlayerPacket;
import com.ultramega.playershells.packet.s2c.AfterDeathPacket;
import com.ultramega.playershells.packet.s2c.FinishedSyncPacket;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.ShellPlayer;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.FACING;
import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements ShellPlayer {
    @Shadow
    @Final
    public MinecraftServer server;

    @Shadow
    public ServerGamePacketListenerImpl connection;
    @Shadow
    @Final
    public ServerPlayerGameMode gameMode;

    public MixinServerPlayer(final Level level, final BlockPos pos, final float rotY, final GameProfile gameProfile) {
        super(level, pos, rotY, gameProfile);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void die(final DamageSource cause, final CallbackInfo ci) {
        if (!Config.TRANSFER_INTO_SHELL_AFTER_DEATH.get()) {
            return;
        }

        final ShellState shellState = ShellSavedData.getShellData(this.serverLevel())
            .getNearestActive(this.getUUID(), this.level().dimension().location(), this.blockPosition());
        if (shellState == null) {
            return;
        }

        // A shell was found so transfer into it
        this.playershells$sendDeathMessageInChat();
        this.removeEntitiesOnShoulder();
        this.stopRiding();
        this.ejectPassengers();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            this.dropAllDeathLoot(cause);
        }

        this.dead = false;
        this.setHealth(0.01F);
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);

        // If keep inventory on, drop inventory and xp inside the new shell
        if (this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.playershells$dropItemsAndExp(shellState.shellForgePos());
        }

        final ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        TransferPlayerPacket.transfer(this.server, serverPlayer, null, shellState.shellForgePos());
        ModNetworking.sendToPlayer(serverPlayer, new AfterDeathPacket(shellState.shellForgePos()));
        ci.cancel();
    }

    @Override
    public void playershells$applyData(final CompoundTag tag, final PositionReference posReference) {
        if (this.getServer() == null) {
            return;
        }
        final ServerLevel targetLevel = findTargetLevel(this.getServer(), posReference);
        if (targetLevel == null) {
            return;
        }

        final BlockPos pos = posReference.pos().immutable();
        final LevelChunk chunk = targetLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.06250;
        final double z = pos.getZ() + 0.5;
        final BlockState state = chunk.getBlockState(pos);
        final float yaw = state.hasProperty(FACING) ? state.getValue(FACING).toYRot() : 0f;

        this.removeAllEffects();
        this.stopRiding();
        this.ejectPassengers();
        playershells$sanitizePlayerData(tag);
        this.load(tag);
        this.loadGameTypes(tag);
        if (this.getHealth() <= 0.0F) {
            this.setHealth(1.0F);
        }
        this.dead = false;

        this.teleportTo(targetLevel, x, y, z, yaw, 0);
        this.setDeltaMovement(0.0, 0.0, 0.0);

        final ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        final PlayerList playerList = this.server.getPlayerList();

        this.onUpdateAbilities();
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(Action.UPDATE_GAME_MODE, serverPlayer));
        this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float) this.gameMode.getGameModeForPlayer().getId()));
        this.getStats().markAllDirty();
        this.updateEffectVisibility();
        if (this.level() == targetLevel) {
            playerList.sendAllPlayerInfo(serverPlayer);
            ForgeEventFactory.firePlayerRespawnEvent(serverPlayer, targetLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
        }

        ModNetworking.sendToPlayer(serverPlayer, new FinishedSyncPacket());
    }

    @Override
    public CompoundTag playershells$getData() {
        final CompoundTag tag = new CompoundTag();

        this.saveWithoutId(tag);
        this.storeGameTypes(tag);
        playershells$sanitizePlayerData(tag);
        if (tag.contains("Health") && tag.getFloat("Health") <= 0.0F) {
            tag.putFloat("Health", 1.0F);
        }

        return tag;
    }

    @Unique
    private void playershells$sendDeathMessageInChat() {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            final Component deathMessage = this.getCombatTracker().getDeathMessage();
            final Component transferredMessage = Component.translatable("death.playershells.transferred_to_shell_" + this.random.nextInt(0, 4));
            final Component betterDeathMessage = Component.literal(deathMessage.getString() + ". " + transferredMessage.getString());
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), betterDeathMessage), PacketSendListener.exceptionallySend(() -> {
                final Component component1 = Component.translatable("death.attack.message_too_long", Component.literal(betterDeathMessage.getString(256))
                    .withStyle(ChatFormatting.YELLOW));
                final Component component2 = Component.translatable("death.attack.even_more_magic", this.getDisplayName()).withStyle((style) -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
                return new ClientboundPlayerCombatKillPacket(this.getId(), component2);
            }));
            final Team team = this.getTeam();
            if (team != null && team.getDeathMessageVisibility() != Visibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastSystemToTeam(this, betterDeathMessage);
                } else if (team.getDeathMessageVisibility() == Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, betterDeathMessage);
                }
            } else {
                this.server.getPlayerList().broadcastSystemMessage(betterDeathMessage, false);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }
    }

    @Unique
    private void playershells$dropItemsAndExp(final PositionReference posReference) {
        final ServerLevel targetLevel = findTargetLevel(this.server, posReference);
        if (targetLevel == null) {
            return;
        }

        final BlockPos pos = posReference.pos();

        for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
            final ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                final ItemEntity itementity = new ItemEntity(this.level(), pos.getX(), pos.getY(), pos.getZ(), stack);
                targetLevel.addFreshEntity(itementity);
            }
        }

        ExperienceOrb.award((ServerLevel) this.level(), pos.getCenter(), this.totalExperience);
    }

    @Unique
    private static void playershells$sanitizePlayerData(final CompoundTag tag) {
        // Prevent stale vehicle/passenger links (e.g. "Received passengers for unknown entity") after body transfer.
        tag.remove("RootVehicle");
        tag.remove("Vehicle");
        tag.remove("Passengers");

        // Prevent carrying over transient death/damage/physics state between bodies.
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("FallDistance");
        tag.remove("OnGround");
        tag.remove("HurtTime");
        tag.remove("HurtByTimestamp");
        tag.remove("DeathTime");
        tag.remove("Fire");
        tag.remove("Air");
        tag.remove("PortalCooldown");
        tag.remove("TicksFrozen");
    }

    @Shadow
    public abstract void teleportTo(ServerLevel newLevel, double x, double y, double z, float yaw, float pitch);

    @Shadow
    protected abstract void tellNeutralMobsThatIDied();

    @Shadow
    public abstract ServerLevel serverLevel();

    @Shadow
    public abstract ServerStatsCounter getStats();

    @Shadow
    public abstract void loadGameTypes(@Nullable CompoundTag tag);

    @Shadow
    protected abstract void storeGameTypes(CompoundTag tag);
}
