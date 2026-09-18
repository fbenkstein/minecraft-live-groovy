package com.frank.livegroovy;

import groovy.lang.Closure;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

final class DynamicCommand extends Command {
    private final Closure<?> handler;

    DynamicCommand(String name, String description, String usage, List<String> aliases, Closure<?> handler) {
        super(name, description, usage, aliases);
        this.handler = handler;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        Object result = handler.call(sender, args);
        return !(result instanceof Boolean value) || value;
    }
}
