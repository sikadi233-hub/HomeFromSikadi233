package com.example.playerhomes;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AcceptHomeTabCompleter implements TabCompleter {

    private final HomeFromSikadi233 plugin;

    public AcceptHomeTabCompleter(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        Map<String, Map<String, Location>> pending = plugin.getPendingShares(player.getUniqueId());

        if (args.length == 1) {
            return filter(new ArrayList<>(pending.keySet()), args[0]);
        } else if (args.length == 2) {
            Map<String, Location> homes = pending.get(args[0]);
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
