package com.frank.livegroovy;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScriptScope implements AutoCloseable {
    private final JavaPlugin plugin;
    private final LiveGroovyApi api;
    private final EventRegistry events;
    private final CommandRegistry commands;
    private final SchedulerApi scheduler;

    public ScriptScope(JavaPlugin plugin, LiveGroovyApi api) {
        this.plugin = plugin;
        this.api = api;
        this.events = new EventRegistry(plugin);
        this.commands = new CommandRegistry(plugin);
        this.scheduler = new SchedulerApi(plugin);
    }

    public LiveGroovyApi api() {
        return api;
    }

    public EventRegistry events() {
        return events;
    }

    public CommandRegistry commands() {
        return commands;
    }

    public SchedulerApi scheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(plugin);
        scheduler.cancelAll();
        commands.unregisterAll();
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
