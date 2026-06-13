package com.example.playerhomes;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeOfTabCompleter implements TabCompleter {

    private final HomeFromSikadi233 plugin;

    public HomeOfTabCompleter(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        if (!player.isOp()) return null;

        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return filter(names, args[0]);
        } else if (args.length == 2) {
            Map<String, Location> homes = plugin.getHomesByName(args[0]);
            if (homes != null) return filter(new ArrayList<>(homes.keySet()), args[1]);
        }
        return null;
    }

    private List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) return options;
        List<String> filtered = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) filtered.add(s);
        }
        return filtered;
    }
}
