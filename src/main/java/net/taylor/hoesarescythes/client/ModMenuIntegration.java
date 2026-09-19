package net.taylor.hoesarescythes.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Only loaded when Mod Menu is installed (via the "modmenu" entrypoint). */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // The screen is built with Cloth Config. Without it, a factory that returns null makes Mod Menu hide the config button.
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> null;
        }
        return ConfigScreen::create;
    }
}
