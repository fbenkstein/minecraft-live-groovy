package com.frank.livegroovy;

import groovy.lang.Closure;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandRegistry {
    private final JavaPlugin plugin;
    private final CommandMap commandMap;
    private final List<Command> commands = new ArrayList<>();

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
    }

    public void register(String name, Closure<?> handler) {
        register(name, "Script command", "/" + name, Collections.emptyList(), handler);
    }

    public void register(String name, String description, Closure<?> handler) {
        register(name, description, "/" + name, Collections.emptyList(), handler);
    }

    public void register(String name, String description, String usage, List<String> aliases, Closure<?> handler) {
        DynamicCommand command = new DynamicCommand(name, description, usage, aliases == null ? Collections.emptyList() : aliases, handler);
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        commands.add(command);
    }

    public void unregisterAll() {
        if (!(commandMap instanceof SimpleCommandMap simpleCommandMap)) {
            return;
        }
        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(simpleCommandMap);
            for (Command command : commands) {
                command.unregister(commandMap);
                List<String> keysToRemove = new ArrayList<>();
                for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
                    if (entry.getValue() == command) {
                        keysToRemove.add(entry.getKey());
                    }
                }
                for (String key : keysToRemove) {
                    knownCommands.remove(key);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("Unable to unregister dynamic commands cleanly: " + e.getMessage());
        }
        commands.clear();
    }

    private CommandMap resolveCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Bukkit command map", e);
        }
    }
}
