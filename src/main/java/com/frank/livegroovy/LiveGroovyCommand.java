package com.frank.livegroovy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public final class LiveGroovyCommand implements CommandExecutor, TabCompleter {
    private final ScriptManager scriptManager;

    public LiveGroovyCommand(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isAllowed(sender)) {
            sender.sendMessage("You must be an operator to use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            ScriptManager.ReloadResult result = scriptManager.reloadAll();
            sender.sendMessage("LiveGroovy loaded " + result.loaded() + " script(s), " + result.failures() + " failure(s).");
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            List<String> scripts = scriptManager.loadedScripts();
            sender.sendMessage(scripts.isEmpty() ? "No Groovy scripts loaded." : "Loaded scripts: " + String.join(", ", scripts));
            return true;
        }
        if (args[0].equalsIgnoreCase("run") && args.length >= 2) {
            scriptManager.runScript(sender, args[1]);
            return true;
        }
        sender.sendMessage("Usage: /groovy reload|list|run <script>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!isAllowed(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("reload", "list", "run");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            return new ArrayList<>(scriptManager.loadedScripts());
        }
        return List.of();
    }

    private boolean isAllowed(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.isOp();
    }
}

