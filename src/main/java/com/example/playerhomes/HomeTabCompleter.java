package com.example.playerhomes;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeTabCompleter implements TabCompleter {

    private final HomeFromSikadi233 plugin;

    public HomeTabCompleter(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            // 自己的传送点 + 分享者名字
            Set<String> own = plugin.getHomeNames(player.getUniqueId());
            Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
            for (String name : own) result.add(name);
            result.addAll(shared.keySet());
        } else if (args.length == 2) {
            // 第二个参数是分享者的传送点名
            String sharerName = args[0];
            Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
            Map<String, Location> homes = shared.get(sharerName);
            if (homes != null) result.addAll(homes.keySet());
        }

        return filter(result, args[args.length - 1]);
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
