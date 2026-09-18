package com.frank.livegroovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public final class ScriptManager {
    private final JavaPlugin plugin;
    private final Path scriptsDir;
    private final LiveGroovyApi api;
    private final List<String> loadedScripts = new ArrayList<>();
    private ScriptScope scope;

    public ScriptManager(JavaPlugin plugin, Path scriptsDir, Path paletteManifest) {
        this.plugin = plugin;
        this.scriptsDir = scriptsDir;
        this.api = new LiveGroovyApi(plugin, paletteManifest);
    }

    public ReloadResult reloadAll() {
        unloadAll();
        try {
            Files.createDirectories(scriptsDir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create scripts directory " + scriptsDir, e);
            return new ReloadResult(0, 1);
        }

        this.scope = new ScriptScope(plugin, api);
        GroovyShell shell = createShell(scope);
        int failures = 0;
        loadedScripts.clear();

        try (var stream = Files.list(scriptsDir)) {
            List<Path> scripts = stream
                .filter(path -> path.getFileName().toString().endsWith(".groovy"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
            for (Path script : scripts) {
                try {
                    LiveGroovyScript parsed = (LiveGroovyScript) shell.parse(script.toFile());
                    parsed.setScope(scope);
                    parsed.run();
                    loadedScripts.add(script.getFileName().toString());
                    plugin.getLogger().info("Loaded script " + script.getFileName());
                } catch (Throwable e) {
                    failures++;
                    plugin.getLogger().log(Level.SEVERE, "Failed to load script " + script.getFileName(), e);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to list scripts in " + scriptsDir, e);
            failures++;
        }

        return new ReloadResult(loadedScripts.size(), failures);
    }

    public void unloadAll() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
        loadedScripts.clear();
    }

    public List<String> loadedScripts() {
        return List.copyOf(loadedScripts);
    }

    public Path scriptsDir() {
        return scriptsDir;
    }

    public void runScript(CommandSender sender, String fileName) {
        Path script = scriptsDir.resolve(fileName).normalize();
        if (!script.startsWith(scriptsDir) || !Files.isRegularFile(script)) {
            sender.sendMessage("No such script: " + fileName);
            return;
        }
        unloadAll();
        this.scope = new ScriptScope(plugin, api);
        try {
            GroovyShell shell = createShell(scope);
            LiveGroovyScript parsed = (LiveGroovyScript) shell.parse(script.toFile());
            parsed.setScope(scope);
            parsed.run();
            loadedScripts.add(script.getFileName().toString());
            sender.sendMessage("Loaded " + script.getFileName());
        } catch (Throwable e) {
            sender.sendMessage("Failed to load " + script.getFileName() + ": " + rootMessage(e));
            plugin.getLogger().log(Level.SEVERE, "Failed to run script " + script.getFileName(), e);
        }
    }

    private GroovyShell createShell(ScriptScope scope) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setScriptBaseClass(LiveGroovyScript.class.getName());
        Binding binding = new Binding();
        binding.setVariable("server", Bukkit.getServer());
        binding.setVariable("plugin", plugin);
        binding.setVariable("api", api);
        binding.setVariable("events", scope.events());
        binding.setVariable("commands", scope.commands());
        binding.setVariable("scheduler", scope.scheduler());
        binding.setVariable("items", api.items());
        return new GroovyShell(getClass().getClassLoader(), binding, configuration);
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : root.getClass().getSimpleName();
    }

    public record ReloadResult(int loaded, int failures) {}
}
