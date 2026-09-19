package net.taylor.hoesarescythes.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.taylor.hoesarescythes.config.ConfigManager;
import net.taylor.hoesarescythes.config.ModConfig;

/** Returns the scythe radius for an item, using the JSON config first, then defaults. */
public final class RadiusResolver {
    private RadiusResolver() {}

    /** @return radius (>=0). 0 means “not a scythe” and disables the AoE. */
    public static int getRadius(ItemStack stack) {
        if (stack == null) return 0;

        // 1) Config-defined entries: a specific item wins over a tag, then first match wins
        ModConfig cfg = ConfigManager.get();
        for (ModConfig.HEntry e : cfg.hoes) {
            if (e != null && e.item != null && idEquals(stack.getItem(), e.item)) {
                return clamp(e.radius);
            }
        }
        for (ModConfig.HEntry e : cfg.hoes) {
            if (e != null && e.tag != null && e.tag.startsWith("#") && isInItemTag(stack, e.tag.substring(1))) {
                return clamp(e.radius);
            }
        }

        // 2) Vanilla defaults (only if not replacing)
        if (!cfg.replaceDefaultHoes) {
            Integer r = ConfigManager.DEFAULT_HOES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if (r != null) return r;
        }

        return 0;
    }

    private static boolean idEquals(Item item, String idStr) {
        Identifier id = Identifier.tryParse(idStr);
        return id != null && BuiltInRegistries.ITEM.getKey(item).equals(id);
    }

    private static boolean isInItemTag(ItemStack stack, String tagIdNoHash) {
        Identifier tagId = Identifier.tryParse(tagIdNoHash);
        if (tagId == null) return false;
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        return stack.is(key);
    }

    private static int clamp(int r) {
        return Math.max(0, Math.min(16, r));
    }
}