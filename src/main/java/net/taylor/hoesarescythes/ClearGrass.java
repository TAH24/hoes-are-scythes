package net.taylor.hoesarescythes;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.taylor.hoesarescythes.logic.BreakEffects;
import net.taylor.hoesarescythes.logic.RadiusResolver;
import net.taylor.hoesarescythes.logic.ScythePredicate;

import java.util.ArrayList;
import java.util.List;

public final class ClearGrass {

    private ClearGrass() {}

    public static void register() {
        AttackBlockCallback.EVENT.register(ClearGrass::onAttackBlock);
    }

    private static InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, BlockPos pos, net.minecraft.core.Direction face) {
        if (world.isClientSide()) return InteractionResult.PASS;

        ItemStack tool = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !tool.is(ItemTags.HOES)) return InteractionResult.PASS;

        int radius = RadiusResolver.getRadius(tool);
        if (radius <= 0) return InteractionResult.PASS;

        BlockState state = world.getBlockState(pos);
        if (!isValidInitialTarget(state)) return InteractionResult.PASS;

        boolean didWork = breakBlocksInRadius(player, (ServerLevel) world, hand, pos, state, tool, radius);
        return didWork ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean breakBlocksInRadius(Player player, ServerLevel world, InteractionHand hand,
                                               BlockPos origin, BlockState initial, ItemStack tool, int radius) {
        final boolean targetingCrop = initial.is(BlockTags.CROPS);
        final boolean targetingNetherWart = initial.is(Blocks.NETHER_WART);
        final boolean targetingFullyGrown = isFullyGrownCrop(initial);

        List<BreakEffects.Broken> broken = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                mutable.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                BlockState state = world.getBlockState(mutable);
                if (tryBreakBlock(
                        player, world, hand, mutable, state,
                        targetingCrop, targetingNetherWart, targetingFullyGrown,
                        tool
                )) {
                    broken.add(new BreakEffects.Broken(mutable.immutable(), state));
                }
            }
        }

        BreakEffects.play(world, origin, broken);
        return !broken.isEmpty();
    }

    private static boolean tryBreakBlock(Player player, ServerLevel world, InteractionHand hand, BlockPos pos, BlockState state,
                                         boolean targetingCrop, boolean targetingNetherWart, boolean targetingFullyGrown,
                                         ItemStack tool) {
        if (state.isAir()) return false;
        if (!player.getAbilities().mayBuild) return false;
        if (world.getServer().isUnderSpawnProtection(world, pos, player)) return false;
        if (!shouldBreakBlock(targetingCrop, targetingFullyGrown, targetingNetherWart, state)) return false;

        boolean broke = breakQuietly(world, pos, state, player, tool, !player.getAbilities().instabuild);
        if (!broke) return false;

        if (!player.getAbilities().instabuild && tool.isDamageableItem()) {
            tool.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return true;
    }

    // Same steps as Level.destroyBlock, minus its per-block break sound and particles (BreakEffects handles those),
    // and with the hoe passed as the tool so enchantments like Fortune affect the drops.
    private static boolean breakQuietly(ServerLevel world, BlockPos pos, BlockState state, Player player, ItemStack tool, boolean drop) {
        FluidState fluid = world.getFluidState(pos);
        if (drop) {
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            Block.dropResources(state, world, pos, blockEntity, player, tool);
        }
        boolean removed = world.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
        if (removed) {
            world.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        }
        return removed;
    }

    private static boolean isValidInitialTarget(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.is(Blocks.NETHER_WART)
                || ScythePredicate.isScythable(state);
    }

    private static boolean shouldBreakBlock(boolean targetingCrop, boolean targetingFullyGrown,
                                            boolean targetingNetherWart, BlockState state) {
        if (targetingCrop && state.is(BlockTags.CROPS)) {
            return !targetingFullyGrown || isFullyGrownCrop(state);
        }
        if (targetingNetherWart && state.is(Blocks.NETHER_WART)) {
            return !targetingFullyGrown || isFullyGrownCrop(state);
        }
        return !targetingCrop && !targetingNetherWart && ScythePredicate.isScythable(state);
    }

    private static boolean isFullyGrownCrop(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int ageValue = state.getValue(age);
                int maxAge = age.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
                return ageValue == maxAge;
            }
        }
        return false;
    }
}