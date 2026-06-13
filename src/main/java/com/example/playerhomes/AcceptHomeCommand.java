package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptHomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public AcceptHomeCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法：/tycs <分享者名> <传送点名>");
            player.sendMessage(ChatColor.GRAY + "例如：/tycs Steve 家");
            return true;
        }

        String sharerName = args[0];
        String homeName = args[1].toLowerCase();

        String error = plugin.acceptShare(player.getUniqueId(), sharerName, homeName);

        if (error != null) {
            player.sendMessage(ChatColor.RED + "❌ " + error);
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "✅ 已接受 " +
                ChatColor.GOLD + sharerName +
                ChatColor.GREEN + " 分享的传送点 " +
                ChatColor.GOLD + homeName + "！");
        player.sendMessage(ChatColor.GRAY + "使用 /cs " + sharerName + " " + homeName + " 前往");

        Player sharer = plugin.getServer().getPlayer(sharerName);
        if (sharer != null) {
            sharer.sendMessage(ChatColor.GREEN + "✅ " +
                    ChatColor.GOLD + player.getName() +
                    ChatColor.GREEN + " 接受了你的分享请求！（传送点: " +
                    ChatColor.GOLD + homeName + ChatColor.GREEN + "）");
        }

        return true;
    }
}
