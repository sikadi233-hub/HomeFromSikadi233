package com.example.playerhomes;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShareHomeTabCompleter implements TabCompleter {

    private final HomeFromSikadi233 plugin;

    public ShareHomeTabCompleter(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        if (args.length == 1) {
            // 在线玩家名
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) names.add(online.getName());
            }
            return filter(names, args[0]);
        } else if (args.length == 2) {
            // 自己的传送点名
            List<String> names = new ArrayList<>(plugin.getHomeNames(player.getUniqueId()));
            return filter(names, args[1]);
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
