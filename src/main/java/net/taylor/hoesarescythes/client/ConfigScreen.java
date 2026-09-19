package net.taylor.hoesarescythes.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.taylor.hoesarescythes.config.ConfigManager;
import net.taylor.hoesarescythes.config.ModConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cloth Config screen for config/hoesarescythes.json, opened from Mod Menu.
 * Only loaded when Cloth Config is installed (see ModMenuIntegration).
 *
 * Vanilla hoes each get a number field (0 = off). Modded hoes and tags are edited as
 * "id = radius" lines, e.g. "othermod:steel_hoe = 3" or "#c:hoes = 3".
 */
public final class ConfigScreen {
    private ConfigScreen() {}

    private static final int MAX_RADIUS = 16; // RadiusResolver clamps to this too

    public static Screen create(Screen parent) {
        ModConfig current = ConfigManager.get();

        // Filled in by the save consumers below, then combined into a fresh config on each save.
        Map<String, Integer> vanillaRadii = new LinkedHashMap<>();
        List<ModConfig.HEntry> otherHoes = new ArrayList<>();
        List<String> extraBlocks = new ArrayList<>();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.hoesarescythes.title"))
                .setSavingRunnable(() -> {
                    ModConfig edited = new ModConfig();
                    edited.replaceDefaultHoes = current.replaceDefaultHoes; // file-only setting; the fields below cover it
                    vanillaRadii.forEach((id, radius) -> edited.hoes.add(hoe(id, radius)));
                    edited.hoes.addAll(otherHoes);
                    edited.extraScythableBlocks = new ArrayList<>(extraBlocks);
                    ConfigManager.save(edited);
                });
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.hoesarescythes.category.general"));

        general.addEntry(entries.startTextDescription(Component.translatable("config.hoesarescythes.server_note")).build());

        for (Map.Entry<String, Integer> d : ConfigManager.DEFAULT_HOES.entrySet()) {
            String id = d.getKey();
            general.addEntry(entries.startIntField(itemName(id), currentRadius(current, id, d.getValue()))
                    .setDefaultValue(d.getValue())
                    .setMin(0)
                    .setMax(MAX_RADIUS)
                    .setTooltip(Component.translatable("config.hoesarescythes.vanilla_hoe.tooltip"))
                    .setSaveConsumer(radius -> vanillaRadii.put(id, radius))
                    .build());
        }

        general.addEntry(entries.startStrList(Component.translatable("config.hoesarescythes.other_hoes"), toHoeLines(otherEntries(current.hoes)))
                .setDefaultValue(List.of())
                .setTooltip(Component.translatable("config.hoesarescythes.other_hoes.tooltip"))
                .setExpanded(true)
                .setInsertInFront(false)
                .setCellErrorSupplier(ConfigScreen::hoeLineError)
                .setCreateNewInstance(list -> new HintCell("", list, Component.translatable("config.hoesarescythes.other_hoes.hint")))
                .setSaveConsumer(lines -> {
                    otherHoes.clear();
                    otherHoes.addAll(parseHoeLines(lines));
                })
                .build());

        general.addEntry(entries.startStrList(Component.translatable("config.hoesarescythes.extra_blocks"), new ArrayList<>(current.extraScythableBlocks))
                .setDefaultValue(List.of())
                .setTooltip(Component.translatable("config.hoesarescythes.extra_blocks.tooltip"))
                .setExpanded(true)
                .setInsertInFront(false)
                .setCellErrorSupplier(ConfigScreen::blockLineError)
                .setCreateNewInstance(list -> new HintCell("", list, Component.translatable("config.hoesarescythes.extra_blocks.hint")))
                .setSaveConsumer(lines -> {
                    extraBlocks.clear();
                    extraBlocks.addAll(nonBlank(lines));
                })
                .build());

        return builder.build();
    }

    // --- Vanilla hoes ---

    private static Component itemName(String id) {
        return Component.translatable(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).getDescriptionId());
    }

    // The radius the game currently uses for this vanilla hoe: its config entry, else its default (or off if defaults are replaced).
    private static int currentRadius(ModConfig cfg, String id, int defaultRadius) {
        for (ModConfig.HEntry e : cfg.hoes) {
            if (e != null && isVanillaEntry(e) && Identifier.parse(e.item).equals(Identifier.parse(id))) {
                return Math.max(0, Math.min(MAX_RADIUS, e.radius));
            }
        }
        return cfg.replaceDefaultHoes ? 0 : defaultRadius;
    }

    private static boolean isVanillaEntry(ModConfig.HEntry e) {
        return e.item != null && isVanillaHoe(e.item);
    }

    private static boolean isVanillaHoe(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return false;
        for (String vanilla : ConfigManager.DEFAULT_HOES.keySet()) {
            if (Identifier.parse(vanilla).equals(id)) return true;
        }
        return false;
    }

    private static List<ModConfig.HEntry> otherEntries(List<ModConfig.HEntry> hoes) {
        List<ModConfig.HEntry> others = new ArrayList<>();
        for (ModConfig.HEntry e : hoes) {
            if (e != null && !isVanillaEntry(e)) others.add(e);
        }
        return others;
    }

    private static ModConfig.HEntry hoe(String itemId, int radius) {
        ModConfig.HEntry e = new ModConfig.HEntry();
        e.item = itemId;
        e.radius = radius;
        return e;
    }

    // --- Other hoe lines: "othermod:steel_hoe = 3" or "#c:hoes = 3" ---

    private static List<String> toHoeLines(List<ModConfig.HEntry> hoes) {
        List<String> lines = new ArrayList<>();
        for (ModConfig.HEntry e : hoes) {
            String target = e.item != null ? e.item : e.tag;
            if (target != null) lines.add(target + " = " + e.radius);
        }
        return lines;
    }

    private static List<ModConfig.HEntry> parseHoeLines(List<String> lines) {
        List<ModConfig.HEntry> hoes = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            int eq = line.lastIndexOf('=');
            String target = line.substring(0, eq).trim();
            ModConfig.HEntry e = new ModConfig.HEntry();
            if (target.startsWith("#")) e.tag = target; else e.item = target;
            e.radius = Integer.parseInt(line.substring(eq + 1).trim());
            hoes.add(e);
        }
        return hoes;
    }

    // Blank lines are ignored on save, so they aren't errors.
    private static Optional<Component> hoeLineError(String line) {
        if (line.isBlank()) return Optional.empty();
        int eq = line.lastIndexOf('=');
        if (eq < 0) return error("config.hoesarescythes.error.hoe_format");

        String target = line.substring(0, eq).trim();
        if (!isValidId(target)) return error("config.hoesarescythes.error.id");
        if (!target.startsWith("#") && isVanillaHoe(target)) return error("config.hoesarescythes.error.vanilla_hoe");

        try {
            int radius = Integer.parseInt(line.substring(eq + 1).trim());
            if (radius < 0 || radius > MAX_RADIUS) return error("config.hoesarescythes.error.radius");
        } catch (NumberFormatException ex) {
            return error("config.hoesarescythes.error.radius");
        }
        return Optional.empty();
    }

    // --- Block lines: "minecraft:fern" or "#minecraft:flowers" ---

    private static Optional<Component> blockLineError(String line) {
        if (line.isBlank()) return Optional.empty();
        return isValidId(line.trim()) ? Optional.empty() : error("config.hoesarescythes.error.id");
    }

    private static List<String> nonBlank(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) result.add(line.trim());
        }
        return result;
    }

    private static boolean isValidId(String idOrTag) {
        String id = idOrTag.startsWith("#") ? idOrTag.substring(1) : idOrTag;
        return !id.isEmpty() && Identifier.tryParse(id) != null;
    }

    private static Optional<Component> error(String key) {
        return Optional.of(Component.translatable(key));
    }
}
