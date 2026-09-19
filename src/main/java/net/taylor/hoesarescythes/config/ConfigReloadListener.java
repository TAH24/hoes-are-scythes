package net.taylor.hoesarescythes.config;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.taylor.hoesarescythes.HoesAreScythes;

public final class ConfigReloadListener implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(HoesAreScythes.MOD_ID, "config_reload");

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        HoesAreScythes.LOGGER.info("[HoesAreScythes] Reloading config from {}", ConfigManager.path());
        ConfigManager.loadOrCreateDefault();
    }
}
