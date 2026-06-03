package com.example.playerhomes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class WarpTabCompleter implements TabCompleter {
    private final HomeFromSikadi233 plugin;
    public WarpTabCompleter(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(new ArrayList<>(plugin.getWarps().keySet()), args[0]);
        }
        return null;
    }

    private List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) return options;
        List<String> filtered = new ArrayList<>();
        String lower = input.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) filtered.add(s);
        }
        return filtered;
    }
}
