package com.ultramega.playershells.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

public final class MathUtils {
    public static final UUID EMPTY_UUID = new UUID(0, 0);

    public static float yawForDirection(final Direction direction) {
        return switch (direction) {
            case NORTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f; //SOUTH
        };
    }

    public static double getMinVelocity(final double velocity, final double absLimit) {
        return Math.abs(velocity) < absLimit ? velocity : absLimit * Math.signum(velocity);
    }

    public static boolean near(final float a, final float b) {
        return Math.abs(a - b) < 1e-4f;
    }

    public static float smoothstep01(final float x) {
        final float smooth = clamp01(x);
        return smooth * smooth * (3f - 2f * smooth);
    }

    public static float clamp01(final float x) {
        return x < 0f ? 0f : Math.min(x, 1f);
    }

    public static int[] interpolateGradient(final float t, final int[] colorA, final int[] colorB, final int[] colorC) {
        if (t < 0.5f) {
            return lerpColor(t / 0.5f, colorA, colorB);
        } else {
            return lerpColor((t - 0.5f) / 0.5f, colorB, colorC);
        }
    }

    private static int[] lerpColor(final float t, final int[] from, final int[] to) {
        return new int[] {
            (int) (from[0] + (to[0] - from[0]) * t),
            (int) (from[1] + (to[1] - from[1]) * t),
            (int) (from[2] + (to[2] - from[2]) * t)
        };
    }

    public static Vec3 lerpVec3(final Vec3 a, final Vec3 b, final float t) {
        final float clampedT = Mth.clamp(t, 0.0F, 1.0F);
        return new Vec3(
            Mth.lerp(clampedT, (float) a.x, (float) b.x),
            Mth.lerp(clampedT, (float) a.y, (float) b.y),
            Mth.lerp(clampedT, (float) a.z, (float) b.z)
        );
    }

    public static float lerpAngle(final float a, final float b, final float t) {
        final float clampedT = Mth.clamp(t, 0.0F, 1.0F);
        final float delta = Mth.wrapDegrees(b - a);
        return a + delta * clampedT;
    }

    public static float wrapDegrees(final float angle) {
        float newAngle = angle % 360;
        if (newAngle < 0) {
            newAngle += 360;
        }
        return newAngle;
    }

    public static int packRGB8(final int r, final int g, final int b) {
        final int ri = Math.min(255, Math.max(0, r));
        final int gi = Math.min(255, Math.max(0, g));
        final int bi = Math.min(255, Math.max(0, b));
        return (ri << 16) | (gi << 8) | bi;
    }

    @Nullable
    public static ServerLevel findTargetLevel(final MinecraftServer server, @Nullable final PositionReference positionReference) {
        if (positionReference == null) {
            return null;
        }
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
            .filter(level -> level.dimension().location().equals(positionReference.dimension()))
            .findAny()
            .orElse(null);
    }

    public static boolean hasPlayerInside(final BlockPos pos, final Level level) {
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        return level.getNearestPlayer(x, y, z, 1, false) != null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(final BlockEntityType<A> serverType,
                                                                                                         final BlockEntityType<E> clientType,
                                                                                                         final BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }

    public static <K, V> Codec<Multimap<K, V>> multiMapCodec(final Codec<K> keyCodec, final Codec<V> valueCodec) {
        return Codec.unboundedMap(keyCodec, valueCodec.listOf()).xmap(map -> {
            if (map != null) {
                final Multimap<K, V> multiMap = HashMultimap.create();

                map.forEach(multiMap::putAll);
                return multiMap;
            }
            return null;
        }, multiMap -> multiMap != null ? multiMap.asMap().entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, e -> Lists.newArrayList(e.getValue()))) : null);
    }

    public static <B extends ByteBuf, K, V, M extends Multimap<K, V>> StreamCodec<B, M> multiMapStreamCodec(final Supplier<? extends M> factory,
                                                                                                            final StreamCodec<? super B, K> keyCodec,
                                                                                                            final StreamCodec<? super B, V> valueCodec) {
        return multiMapStreamCodec(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Multimap<K, V>> StreamCodec<B, M> multiMapStreamCodec(final Supplier<? extends M> factory,
                                                                                                     final StreamCodec<? super B, K> keyCodec,
                                                                                                     final StreamCodec<? super B, V> valueCodec,
                                                                                                     final int maxSize) {
        return new StreamCodec<>() {
            @Override
            public void encode(final B out, final M multimap) { //TODO: this isn't working correctly
                List<Map.Entry<K, V>> entries = new ArrayList<>();
                synchronized (multimap) {
                    if (!multimap.entries().isEmpty()) {
                        entries = new ArrayList<>(multimap.entries());
                    }
                }

                final int size = entries.size();
                if (size > maxSize) {
                    throw new IllegalArgumentException("Multimap size " + size + " exceeds max " + maxSize);
                }

                ByteBufCodecs.writeCount(out, size, maxSize);
                for (final Map.Entry<K, V> e : entries) {
                    keyCodec.encode(out, e.getKey());
                    valueCodec.encode(out, e.getValue());
                }
            }

            @Override
            public M decode(final B in) {
                final int count = ByteBufCodecs.readCount(in, maxSize);
                final M result = factory.get();
                for (int i = 0; i < count; i++) {
                    final K key = keyCodec.decode(in);
                    final V value = valueCodec.decode(in);
                    result.put(key, value);
                }
                return result;
            }
        };
    }
}
