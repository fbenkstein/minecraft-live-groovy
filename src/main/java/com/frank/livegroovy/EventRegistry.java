package com.frank.livegroovy;

import groovy.lang.Closure;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventRegistry {
    private final JavaPlugin plugin;
    private final Listener listener = new Listener() {};

    public EventRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public <T extends Event> void on(Class<T> eventClass, Closure<?> closure) {
        on(eventClass, EventPriority.NORMAL, false, closure);
    }

    public <T extends Event> void on(Class<T> eventClass, EventPriority priority, Closure<?> closure) {
        on(eventClass, priority, false, closure);
    }

    public <T extends Event> void on(Class<T> eventClass, EventPriority priority, boolean ignoreCancelled, Closure<?> closure) {
        EventExecutor executor = (ignored, event) -> {
            if (eventClass.isInstance(event)) {
                closure.call(event);
            }
        };
        Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, plugin, ignoreCancelled);
    }
}
