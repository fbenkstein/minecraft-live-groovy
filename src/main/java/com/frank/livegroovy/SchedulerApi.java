package com.frank.livegroovy;

import groovy.lang.Closure;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class SchedulerApi {
    private final JavaPlugin plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public SchedulerApi(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask later(long ticks, Closure<?> closure) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> closure.call(), ticks);
        tasks.add(task);
        return task;
    }

    public BukkitTask repeat(long delayTicks, long periodTicks, Closure<?> closure) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> closure.call(), delayTicks, periodTicks);
        tasks.add(task);
        return task;
    }

    public BukkitTask async(Closure<?> closure) {
        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> closure.call());
        tasks.add(task);
        return task;
    }

    public void cancelAll() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }
}
