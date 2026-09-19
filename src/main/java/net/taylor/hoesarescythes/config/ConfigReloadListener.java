package net.taylor.hoesarescythes.config;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.taylor.hoesarescythes.HoesAreScythes;

public final class ConfigReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(HoesAreScythes.MOD_ID, "config_reload");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        HoesAreScythes.LOGGER.info("[HoesAreScythes] Reloading config from {}", ConfigManager.path());
        ConfigManager.loadOrCreateDefault();
    }
}