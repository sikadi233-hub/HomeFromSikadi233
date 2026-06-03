package com.example.playerhomes;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DelHomeTabCompleter implements TabCompleter {

    private final HomeFromSikadi233 plugin;

    public DelHomeTabCompleter(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        if (args.length == 1) {
            Player player = (Player) sender;
            List<String> names = new ArrayList<>(plugin.getHomeNames(player.getUniqueId()));
            // 也加上分享的传送点名称
            Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
            for (Map<String, Location> homes : shared.values()) {
                names.addAll(homes.keySet());
            }
            return filter(names, args[0]);
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
