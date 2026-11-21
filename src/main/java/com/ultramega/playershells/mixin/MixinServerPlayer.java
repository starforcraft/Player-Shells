package com.ultramega.playershells.mixin;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.packet.s2c.AfterDeathPacket;
import com.ultramega.playershells.packet.s2c.FinishedSyncPacket;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.ShellPlayer;

import java.util.Set;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements ShellPlayer {
    protected MixinServerPlayer(final Level level, final GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "die", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;markClientUnloadedAfterDeath()V"), cancellable = true)
    private void playershells$transferAfterDeath(final DamageSource cause, final CallbackInfo ci) {
        if (!Config.TRANSFER_INTO_SHELL_AFTER_DEATH.get()) {
            return;
        }

        final ServerPlayer player = (ServerPlayer) (Object) this;
        final ServerLevel source = player.level();
        final ShellState shell = ShellSavedData.getShellData(source)
            .getNearestActive(this.getUUID(), source.dimension().identifier(), this.blockPosition());
        if (shell == null) {
            return;
        }

        final PositionReference destination = shell.shellForgePos();
        final ServerLevel target = findTargetLevel(source.getServer(), destination);
        final BlockPos pos = destination.pos();
        if (target == null
            || !(target.getChunkAt(pos).getBlockEntity(pos) instanceof ShellForgeBlockEntity forge)
            || !forge.getShellUuid().equals(shell.shellUUID())) {
            return;
        }

        final boolean keepInventory = source.getGameRules().get(GameRules.KEEP_INVENTORY);
        if (forge.transferPlayerTo(player, () -> {
            if (keepInventory) {
                for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                    final ItemStack stack = this.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        target.addFreshEntity(new ItemEntity(target, pos.getX() + 0.5, pos.getY() + 0.0625, pos.getZ() + 0.5, stack.copy()));
                    }
                }
                ExperienceOrb.award(target, pos.getCenter(), this.totalExperience);
            }
        })) {
            this.dead = false;
            this.deathTime = 0;
            PacketDistributor.sendToPlayer(player, new AfterDeathPacket(destination));
            EventHooks.firePlayerRespawnEvent(player, false);
            ci.cancel();
        }
    }

    @Override
    public boolean playershells$applyData(final CompoundTag tag, final PositionReference destination, final Runnable afterTeleport) {
        final ServerPlayer player = (ServerPlayer) (Object) this;
        final ServerLevel target = findTargetLevel(player.level().getServer(), destination);
        if (target == null) {
            return false;
        }

        final BlockPos pos = destination.pos();
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.0625;
        final double z = pos.getZ() + 0.5;
        final float yaw = destination.facing().toYRot();
        if (!player.teleportTo(target, x, y, z, Set.of(), yaw, 0, true)) {
            return false;
        }

        afterTeleport.run();
        this.removeAllEffects();
        final CompoundTag body = tag.copy();
        body.remove(Entity.TAG_UUID);
        body.remove("respawn");
        body.remove("LastDeathLocation");
        body.remove("sleeping_pos");
        body.remove("fall_distance");
        body.remove("FallFlying");
        final TagValueOutput pose = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        pose.store("Pos", Vec3.CODEC, new Vec3(x, y, z));
        pose.store("Motion", Vec3.CODEC, Vec3.ZERO);
        pose.store("Rotation", Vec2.CODEC, new Vec2(yaw, 0));
        pose.storeNullable("respawn", ServerPlayer.RespawnConfig.CODEC, player.getRespawnConfig());
        this.getLastDeathLocation().ifPresent(location -> pose.store("LastDeathLocation", GlobalPos.CODEC, location));
        body.merge(pose.buildResult());
        this.load(TagValueInput.create(ProblemReporter.DISCARDING, target.registryAccess(), body));
        player.connection.resetPosition();

        final var playerList = target.getServer().getPlayerList();
        player.onUpdateAbilities();
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, player));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, player.gameMode().getId()));
        player.getStats().markAllDirty();
        player.resetSentInfo();
        player.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
        playerList.sendActivePlayerEffects(player);
        playerList.sendAllPlayerInfo(player);
        PacketDistributor.sendToPlayer(player, new FinishedSyncPacket());
        return true;
    }

    @Override
    public CompoundTag playershells$getData() {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.registryAccess());
        this.saveWithoutId(output);
        return output.buildResult();
    }
}
