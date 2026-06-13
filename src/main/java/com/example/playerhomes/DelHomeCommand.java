package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public DelHomeCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法：/delhome <名称>");
            player.sendMessage(ChatColor.GRAY + "（也可以删除已接受的分享传送点）");
            return true;
        }

        String homeName = args[0];
        String deletedType = plugin.deleteHome(player.getUniqueId(), homeName);

        if (deletedType == null) {
            player.sendMessage(ChatColor.RED + "❌ 传送点 " +
                    ChatColor.GOLD + homeName +
                    ChatColor.RED + " 不存在！");
            return true;
        }

        if (deletedType.equals("own")) {
            player.sendMessage(ChatColor.GREEN + "🗑️ 传送点 " +
                    ChatColor.GOLD + homeName +
                    ChatColor.GREEN + " 已删除！");
        } else {
            player.sendMessage(ChatColor.GREEN + "🗑️ 分享传送点 " +
                    ChatColor.GOLD + homeName +
                    ChatColor.GREEN + " 已移除！");
        }
        return true;
    }
}
