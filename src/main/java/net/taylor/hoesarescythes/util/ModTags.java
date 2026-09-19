package net.taylor.hoesarescythes.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.taylor.hoesarescythes.HoesAreScythes;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> SCYTHE_BLOCKS = createTag();

        private static TagKey<Block> createTag() {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(HoesAreScythes.MOD_ID, "scythe_blocks"));
        }
    }
}