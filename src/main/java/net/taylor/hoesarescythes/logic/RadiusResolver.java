package net.taylor.hoesarescythes.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.taylor.hoesarescythes.config.ConfigManager;
import net.taylor.hoesarescythes.config.ModConfig;

import java.util.Map;

/** Returns the scythe radius for an item, using the JSON config first, then defaults. */
public final class RadiusResolver {
    private RadiusResolver() {}

    /** Vanilla default radii (edit here for copper etc.). */
    private static final Map<Item, Integer> VANILLA_DEFAULTS = Map.of(
            Items.WOODEN_HOE,   1,
            Items.STONE_HOE,    1,
            Items.IRON_HOE,     2,
            Items.COPPER_HOE,   2,
            Items.DIAMOND_HOE,  3,
            Items.GOLDEN_HOE,   4,
            Items.NETHERITE_HOE,4
    );

    /** @return radius (>=0). 0 means “not a scythe” and disables the AoE. */
    public static int getRadius(ItemStack stack) {
        if (stack == null) return 0;

        // 1) Config-defined entries (first match wins)
        ModConfig cfg = ConfigManager.get();
        for (ModConfig.HEntry e : cfg.hoes) {
            if (e.item != null && idEquals(stack.getItem(), e.item)) {
                return clamp(e.radius);
            }
            if (e.tag != null && e.tag.startsWith("#") && isInItemTag(stack, e.tag.substring(1))) {
                return clamp(e.radius);
            }
        }

        // 2) Vanilla defaults (only if not replacing)
        if (!cfg.replaceDefaultHoes) {
            Integer r = VANILLA_DEFAULTS.get(stack.getItem());
            if (r != null) return r;
        }

        return 0;
    }

    private static boolean idEquals(Item item, String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return id != null && BuiltInRegistries.ITEM.getKey(item).equals(id);
    }

    private static boolean isInItemTag(ItemStack stack, String tagIdNoHash) {
        ResourceLocation tagId = ResourceLocation.tryParse(tagIdNoHash);
        if (tagId == null) return false;
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        return stack.is(key);
    }

    private static int clamp(int r) {
        return Math.max(0, Math.min(16, r));
    }
}