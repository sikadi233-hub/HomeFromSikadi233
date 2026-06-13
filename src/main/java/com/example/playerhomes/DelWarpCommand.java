package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelWarpCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public DelWarpCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ 你没有权限！(仅限OP)");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法：/delwarp <名称>");
            return true;
        }

        if (plugin.deleteWarp(args[0])) {
            player.sendMessage(ChatColor.GREEN + "🗑️ 地标 " + ChatColor.GOLD + args[0] + ChatColor.GREEN + " 已删除！");
        } else {
            player.sendMessage(ChatColor.RED + "❌ 地标 " + ChatColor.GOLD + args[0] + ChatColor.RED + " 不存在！");
        }
        return true;
    }
}
