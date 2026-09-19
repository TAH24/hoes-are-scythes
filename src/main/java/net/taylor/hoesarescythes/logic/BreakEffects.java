package net.taylor.hoesarescythes.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sounds and particles for a scythe swing.
 * Vanilla plays a full break sound for every block, so a 9x9 swing stacked up to 81 identical sounds (issue #3).
 * Instead: the block that was hit sounds as normal, each other plant type sounds once,
 * and a few quieter sounds are scattered across the area so a big swing still sounds big.
 */
public final class BreakEffects {
    private BreakEffects() {}

    /** Extra scattered sounds: one per this many broken blocks, up to MAX_EXTRA_SOUNDS. */
    private static final int BLOCKS_PER_EXTRA_SOUND = 8;
    private static final int MAX_EXTRA_SOUNDS = 3;
    private static final float EXTRA_SOUND_VOLUME = 0.5f;
    private static final int PARTICLES_PER_BLOCK = 8;

    public record Broken(BlockPos pos, BlockState state) {}

    public static void play(ServerLevel world, BlockPos origin, List<Broken> broken) {
        if (broken.isEmpty()) return;
        RandomSource random = world.getRandom();

        // The block that was hit gets vanilla's normal break sound and particles.
        Broken main = broken.stream().filter(b -> b.pos().equals(origin)).findFirst().orElse(broken.get(0));
        world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, main.pos(), Block.getId(main.state()));

        Set<SoundType> soundsPlayed = new HashSet<>();
        soundsPlayed.add(main.state().getSoundType());

        for (Broken b : broken) {
            if (b == main) continue;

            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, b.state()),
                    b.pos().getX() + 0.5, b.pos().getY() + 0.5, b.pos().getZ() + 0.5,
                    PARTICLES_PER_BLOCK, 0.25, 0.25, 0.25, 0.0);

            // One sound per other plant type (e.g. flowers mixed into grass).
            if (soundsPlayed.add(b.state().getSoundType())) {
                playBreakSound(world, b, 1.0f, random);
            }
        }

        int extraSounds = Math.min(MAX_EXTRA_SOUNDS, (broken.size() - 1) / BLOCKS_PER_EXTRA_SOUND);
        for (int i = 0; i < extraSounds; i++) {
            playBreakSound(world, broken.get(random.nextInt(broken.size())), EXTRA_SOUND_VOLUME, random);
        }
    }

    // Matches vanilla's block break sound, with slight pitch variation so overlapping sounds don't stack into one harsh tone.
    private static void playBreakSound(ServerLevel world, Broken b, float volumeScale, RandomSource random) {
        SoundType sound = b.state().getSoundType();
        float volume = (sound.getVolume() + 1.0f) / 2.0f * volumeScale;
        float pitch = sound.getPitch() * 0.8f * (0.9f + random.nextFloat() * 0.2f);
        world.playSound(null, b.pos(), sound.getBreakSound(), SoundSource.BLOCKS, volume, pitch);
    }
}
