package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public SetWarpCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ 你没有权限！(仅限OP)");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法：/setwarp <名称>");
            return true;
        }

        String name = args[0].toLowerCase();
        plugin.addWarp(name, player.getLocation());
        player.sendMessage(ChatColor.GREEN + "✅ 地标 " + ChatColor.GOLD + name + ChatColor.GREEN + " 设置成功！");
        return true;
    }
}
