package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HomeOfCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public HomeOfCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能传送！");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ 你没有权限使用这个命令！（仅限 OP）");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法：/homeof <玩家名> <传送点名>");
            return true;
        }

        String targetName = args[0];
        String homeName = args[1].toLowerCase();

        UUID targetUUID = plugin.getUUIDByName(targetName);
        if (targetUUID == null) {
            player.sendMessage(ChatColor.RED + "❌ 找不到玩家 " + ChatColor.GOLD + targetName);
            return true;
        }

        Location homeLocation = plugin.getOwnHome(targetUUID, homeName);
        if (homeLocation == null) {
            player.sendMessage(ChatColor.RED + "❌ " + ChatColor.GOLD + targetName +
                    ChatColor.RED + " 没有名为 " + ChatColor.GOLD + homeName +
                    ChatColor.RED + " 的传送点");
            return true;
        }

        player.teleport(homeLocation);
        player.sendMessage(ChatColor.GREEN + "🏠 已传送到 " +
                ChatColor.GOLD + targetName +
                ChatColor.GREEN + " 的传送点 " +
                ChatColor.GOLD + homeName + "！");
        return true;
    }
}
