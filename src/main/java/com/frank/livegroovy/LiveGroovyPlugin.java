package com.frank.livegroovy;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;

public final class LiveGroovyPlugin extends JavaPlugin {
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        Path scriptsDir = getDataFolder().toPath().resolve("scripts");
        Path paletteManifest = getDataFolder().toPath().resolve("live-palette-associations.json");
        this.scriptManager = new ScriptManager(this, scriptsDir, paletteManifest);

        PluginCommand command = Objects.requireNonNull(getCommand("groovy"), "groovy command missing from plugin.yml");
        LiveGroovyCommand executor = new LiveGroovyCommand(this.scriptManager);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        this.scriptManager.reloadAll();
    }

    @Override
    public void onDisable() {
        if (this.scriptManager != null) {
            this.scriptManager.unloadAll();
        }
    }
}
