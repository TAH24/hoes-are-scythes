package net.taylor.hoesarescythes.logic;

import net.taylor.hoesarescythes.config.ConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.taylor.hoesarescythes.HoesAreScythes;
import net.taylor.hoesarescythes.util.ModTags;

public final class ScythePredicate {
    private ScythePredicate() {}

    /** True if the state should be cleared by the scythe logic. */
    public static boolean isScythable(BlockState state) {
        // 1) Your data tag (base list; supports required:false entries)
        if (state.is(ModTags.Blocks.SCYTHE_BLOCKS)) return true;

        // 2) Runtime extensions from config
        for (String entry : ConfigManager.get().extraScythableBlocks) {
            if (entry == null || entry.isEmpty()) continue;

            // Tag entry: "#namespace:path"
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId == null) {
                    HoesAreScythes.LOGGER.debug("Ignoring invalid scythable tag '{}'", entry);
                    continue;
                }
                TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
                if (state.is(tagKey)) return true;
                continue;
            }

            // Single block id: "namespace:path"
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id == null) {
                HoesAreScythes.LOGGER.debug("Ignoring invalid scythable id '{}'", entry);
                continue;
            }
            if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id)) return true;
        }

        return false;
    }
}