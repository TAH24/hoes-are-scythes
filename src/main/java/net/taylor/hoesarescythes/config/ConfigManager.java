package net.taylor.hoesarescythes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Reads/writes config/hoesarescythes.json
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hoesarescythes.json";

    /** Vanilla hoes and their default radius. Used for new config files, missing entries, and as the fallback. */
    public static final Map<String, Integer> DEFAULT_HOES = defaultHoes();

    // volatile: the config screen saves on the client thread while the (integrated) server thread reads it.
    private static volatile ModConfig CURRENT = new ModConfig();

    private ConfigManager() {}

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    // Returns the last loaded config (never null).
    public static ModConfig get() {
        return CURRENT;
    }

    // Create a default file if missing, then load it into memory.
    public static void loadOrCreateDefault() {
        Path p = path();
        if (!Files.exists(p)) {
            write(p, new ModConfig());
        }
        try {
            String json = Files.readString(p);
            ModConfig cfg = GSON.fromJson(json, ModConfig.class);
            if (cfg == null) cfg = new ModConfig();
            if (normalize(cfg)) {
                write(p, cfg);
            }
            CURRENT = cfg;
        } catch (Exception e) {
            System.err.println("[HoesAreScythes] Failed to read config, keeping previous: " + e.getMessage());
        }
    }

    // Writes the config to disk and applies it immediately (used by the Mod Menu config screen).
    public static void save(ModConfig cfg) {
        normalize(cfg);
        write(path(), cfg);
        CURRENT = cfg;
    }

    /** The vanilla hoes as config entries, in default order. */
    public static List<ModConfig.HEntry> defaultHoeEntries() {
        List<ModConfig.HEntry> entries = new ArrayList<>();
        DEFAULT_HOES.forEach((id, radius) -> entries.add(entry(id, radius)));
        return entries;
    }

    // Fills in missing lists and vanilla hoes. Returns true if anything was added.
    private static boolean normalize(ModConfig cfg) {
        if (cfg.hoes == null) cfg.hoes = new ArrayList<>();
        if (cfg.extraScythableBlocks == null) cfg.extraScythableBlocks = new ArrayList<>();
        return addMissingDefaultHoes(cfg);
    }

    // Appends vanilla hoes the file doesn't list yet (e.g. copper hoes for configs made before they existed).
    // They go at the end, so they never override an existing item or tag entry.
    private static boolean addMissingDefaultHoes(ModConfig cfg) {
        if (cfg.replaceDefaultHoes) return false;

        Set<Identifier> listed = new HashSet<>();
        for (ModConfig.HEntry e : cfg.hoes) {
            Identifier id = (e == null || e.item == null) ? null : Identifier.tryParse(e.item);
            if (id != null) listed.add(id);
        }

        boolean added = false;
        for (Map.Entry<String, Integer> d : DEFAULT_HOES.entrySet()) {
            if (!listed.contains(Identifier.parse(d.getKey()))) {
                cfg.hoes.add(entry(d.getKey(), d.getValue()));
                added = true;
            }
        }
        return added;
    }

    private static void write(Path p, ModConfig cfg) {
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(cfg));
        } catch (IOException e) {
            System.err.println("[HoesAreScythes] Failed to write config: " + e.getMessage());
        }
    }

    private static Map<String, Integer> defaultHoes() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("minecraft:wooden_hoe", 1);
        m.put("minecraft:stone_hoe", 1);
        m.put("minecraft:copper_hoe", 2);
        m.put("minecraft:iron_hoe", 2);
        m.put("minecraft:golden_hoe", 4);
        m.put("minecraft:diamond_hoe", 3);
        m.put("minecraft:netherite_hoe", 4);
        return Collections.unmodifiableMap(m);
    }

    private static ModConfig.HEntry entry(String id, int radius) {
        ModConfig.HEntry e = new ModConfig.HEntry();
        e.item = id;
        e.radius = radius;
        return e;
    }
}