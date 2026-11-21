package com.ultramega.playershells.utils;


import javax.annotation.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import static com.ultramega.playershells.utils.MathUtils.lerpAngle;
import static com.ultramega.playershells.utils.MathUtils.lerpVec3;

public final class CameraHandler {
    private static final long PHASE1_MS = 700L; // 0.7 seconds: forward + 180° turn
    private static final long PHASE2_MS = 2_000L; // 2 seconds: vertical rise
    private static final long PITCH_MS = 400L; // 0.4 second: pitch 0 → 90
    private static final long ARRIVAL_TIMEOUT_MS = 10_000L;
    private static final double BLOCKS_UP = 400.0;

    @Nullable
    private static Vec3 startPosition;
    @Nullable
    private static Direction startFacing;
    @Nullable
    private static Vec3 endPosition;
    @Nullable
    private static PositionReference endForge;

    @Nullable
    private static Runnable onFinished;

    private static boolean animating = false;
    private static boolean reversing = false;
    private static long animClockMs = 0L;
    private static long lastNowMs = -1L;
    private static long arrivalWaitStartedMs = -1L;

    @Nullable
    private static Vec3 p0Start;
    @Nullable
    private static Vec3 p1Forward;
    @Nullable
    private static Vec3 p2Up;
    @Nullable
    private static Vec3 endStepDir;

    private static float startYaw;
    private static float startMidYaw;
    private static float endYaw;
    private static float endMidYaw;

    public static void cameraTick(final Camera camera) {
        final Minecraft mc = Minecraft.getInstance();
        // Wait for teleport transition screen to end
        if (mc.screen instanceof LevelLoadingScreen) {
            lastNowMs = -1L;
            return;
        }

        final long now = Util.getMillis();

        if (!animating) {
            beginAnimation(now);
        }
        if (animating) {
            final CameraAccess access = (CameraAccess) camera;
            if (arrivalWaitStartedMs >= 0L) {
                if (!hasArrived(mc)) {
                    if (now - arrivalWaitStartedMs >= ARRIVAL_TIMEOUT_MS) {
                        resetPosition();
                    } else if (p2Up != null) {
                        access.playershells$setDetached(true);
                        access.playershells$setPosition(p2Up);
                        access.playershells$setRotation(startMidYaw, 90.0f, 0.0f);
                    }
                    return;
                }
                arrivalWaitStartedMs = -1L;
                reversing = true;
                animClockMs = 0L;
                lastNowMs = now;
            }
            if (lastNowMs < 0L) {
                lastNowMs = now;
            }

            // Only advance the animation clock if we're not frozen
            if (!mc.isPaused()) {
                animClockMs += Math.max(0L, now - lastNowMs);
            }
            lastNowMs = now;

            access.playershells$setDetached(true);
            applyAnimation(access, animClockMs, now);
        }
    }

    public static void setMovingAnimation(@Nullable final BlockPos startPosition,
                                          @Nullable final Direction startFacing,
                                          final PositionReference endForge,
                                          final Runnable onFinished) {
        resetPosition();
        CameraHandler.startPosition = startPosition != null ? startPosition.getCenter().add(0, 0.2, 0) : null;
        CameraHandler.startFacing = startFacing;
        CameraHandler.endPosition = endForge.pos().above().getCenter().add(0, 0.2, 0);
        CameraHandler.endForge = endForge;
        CameraHandler.onFinished = onFinished;
    }

    public static void resetPosition() {
        startPosition = null;
        startFacing = null;
        endPosition = null;
        endForge = null;
        onFinished = null;
        animating = false;
        reversing = false;
        animClockMs = 0L;
        lastNowMs = -1L;
        arrivalWaitStartedMs = -1L;
        p0Start = null;
        p1Forward = null;
        p2Up = null;
        endStepDir = null;
    }

    public static boolean isCameraOutsideOfPlayer() {
        return endForge != null;
    }

    public static boolean isMovingTo(final Identifier dimension, final BlockPos forgePos) {
        return endForge != null && endForge.dimension().equals(dimension) && endForge.pos().equals(forgePos);
    }

    private static boolean hasArrived(final Minecraft client) {
        if (endForge == null || client.player == null || client.level == null || !client.level.dimension().identifier().equals(endForge.dimension())) {
            return false;
        }
        final BlockPos pos = endForge.pos();
        final ChunkPos chunk = ChunkPos.containing(pos);
        return client.player.position().distanceToSqr(Vec3.atBottomCenterOf(pos)) < 1.0 && client.level.getChunkSource().hasChunk(chunk.x(), chunk.z());
    }

    private static void beginAnimation(final long now) {
        if (endPosition == null || endForge == null) {
            return;
        }
        animating = true;
        reversing = false;
        animClockMs = 0L;
        lastNowMs = now;

        if (startFacing != null && startPosition != null) {
            p0Start = startPosition;
            final Vec3 startStepDir = new Vec3(startFacing.getStepX(), startFacing.getStepY(), startFacing.getStepZ());
            p1Forward = p0Start.add(startStepDir);
            p2Up = p1Forward.add(0.0, BLOCKS_UP, 0.0);

            startYaw = startFacing.toYRot();
            startMidYaw = Mth.wrapDegrees(startYaw + 180.0f);
        } else {
            arrivalWaitStartedMs = now;
        }

        final Direction endFacing = endForge.facing();
        endStepDir = new Vec3(endFacing.getStepX(), endFacing.getStepY(), endFacing.getStepZ());

        endYaw = endFacing.toYRot();
        endMidYaw = Mth.wrapDegrees(endYaw + 180.0f);
    }

    private static void applyAnimation(final CameraAccess camera, final long elapsedMs, final long now) {
        final long elapsed = Math.max(0L, elapsedMs);
        final long forwardTotal = PHASE1_MS + PHASE2_MS;

        if (!reversing && p0Start != null && p1Forward != null && p2Up != null) {
            if (elapsed <= PHASE1_MS) {
                // Phase 1: move forward 1 block + rotate 180°, pitch stays 0
                final float t = (float) elapsed / (float) PHASE1_MS;
                final Vec3 pos = lerpVec3(p0Start, p1Forward, t);
                final float yaw = lerpAngle(startYaw, startMidYaw, t);
                camera.playershells$setPosition(pos);
                camera.playershells$setRotation(yaw, 0.0f, 0.0F);
            } else if (elapsed <= forwardTotal) {
                // Phase 2: rise for 2s, yaw fixed at startMidYaw, animate pitch 0 -> 90
                final long phase2Elapsed = elapsed - PHASE1_MS;
                final float tRise = (float) phase2Elapsed / (float) PHASE2_MS;
                final Vec3 pos = lerpVec3(p1Forward, p2Up, tRise);

                final float pitch;
                if (phase2Elapsed <= PITCH_MS) {
                    final float tPitch = (float) phase2Elapsed / (float) PITCH_MS;
                    pitch = Mth.lerp(tPitch, 0.0f, 90.0f);
                } else {
                    pitch = 90.0f;
                }

                camera.playershells$setPosition(pos);
                camera.playershells$setRotation(startMidYaw, pitch, 0.0F);
            } else {
                // Hold above the departure Forge until the destination is ready
                camera.playershells$setPosition(p2Up);
                camera.playershells$setRotation(startMidYaw, 90.0f, 0.0F);
                arrivalWaitStartedMs = now;
                if (onFinished != null) {
                    final Runnable callback = onFinished;
                    onFinished = null;
                    callback.run();
                }
            }
            return;
        }

        if (endPosition == null || endForge == null || endStepDir == null) {
            resetPosition();
            return;
        }

        final Vec3 e0End = endPosition;
        final Vec3 e1Forward = e0End.add(endStepDir);

        if (elapsed <= PHASE2_MS) {
            // Start directly above the destination Forge
            final float tDown = (float) elapsed / (float) PHASE2_MS;
            final Vec3 pos = lerpVec3(e0End.add(0.0, BLOCKS_UP, 0.0), e1Forward, tDown);
            camera.playershells$setPosition(pos);
            camera.playershells$setRotation(endMidYaw, 90.0F, 0.0F);
        } else if (elapsed <= PHASE2_MS + PHASE1_MS) {
            // Reverse of Phase 1: move 1 block back to end + rotate from endMidYaw -> endYaw
            final long phase1BackElapsed = elapsed - PHASE2_MS;
            final float t = (float) phase1BackElapsed / (float) PHASE1_MS;

            final Vec3 pos = lerpVec3(e1Forward, e0End, t);
            final float yaw = lerpAngle(endMidYaw, endYaw, t);

            final float pitch;
            if (phase1BackElapsed <= PITCH_MS) {
                final float tPitch = (float) phase1BackElapsed / (float) PITCH_MS;
                pitch = Mth.lerp(tPitch, 90.0f, 0.0f);
            } else {
                pitch = 0.0f;
            }

            camera.playershells$setPosition(pos);
            camera.playershells$setRotation(yaw, pitch, 0.0F);
        } else {
            camera.playershells$setPosition(e0End);
            camera.playershells$setRotation(endYaw, 0.0f, 0.0F);
            resetPosition();
        }
    }
}
