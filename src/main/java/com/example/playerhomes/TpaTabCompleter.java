package com.example.playerhomes;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpaTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(sender)) names.add(online.getName());
            }
            return filter(names, args[0]);
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
