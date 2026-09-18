package com.frank.livegroovy;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class LiveGroovyApi {
    private final JavaPlugin plugin;
    private final PaletteItems items;

    public LiveGroovyApi(JavaPlugin plugin, Path paletteManifest) {
        this.plugin = plugin;
        this.items = new PaletteItems(plugin, paletteManifest);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public PaletteItems items() {
        return items;
    }
}
