package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public SetHomeCommand(HomeFromSikadi233 plugin) {
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
            player.sendMessage(ChatColor.RED + "用法：/sethome <名称>");
            player.sendMessage(ChatColor.GRAY + "你已使用: " +
                    plugin.countOwnHomes(player.getUniqueId()) + "/" +
                    plugin.getMaxOwnHomes() + " 个传送点");
            return true;
        }

        String homeName = args[0].toLowerCase();
        String error = plugin.setHome(player.getUniqueId(), homeName, player.getLocation());

        if (error != null) {
            player.sendMessage(ChatColor.RED + "❌ " + error);
            return true;
        }

        int used = plugin.countOwnHomes(player.getUniqueId());
        int max = plugin.getMaxOwnHomes();
        player.sendMessage(ChatColor.GREEN + "✅ 传送点 " +
                ChatColor.GOLD + homeName +
                ChatColor.GREEN + " 设置成功！ (" + used + "/" + max + ")");
        return true;
    }
}
