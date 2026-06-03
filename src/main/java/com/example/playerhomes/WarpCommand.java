package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public WarpCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法：/warp <地标名>");
            player.sendMessage(ChatColor.GRAY + "使用 /warplist 查看所有地标");
            return true;
        }

        Location loc = plugin.getWarp(args[0]);
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "❌ 地标 " + ChatColor.GOLD + args[0] + ChatColor.RED + " 不存在！");
            return true;
        }

        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "🏠 已传送到地标 " + ChatColor.GOLD + args[0] + "！");
        return true;
    }
}
