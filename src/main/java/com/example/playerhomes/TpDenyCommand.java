package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpDenyCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public TpDenyCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;

        HomeFromSikadi233.TpaRequest req = plugin.getTpaRequest(player.getUniqueId());
        if (req == null) {
            player.sendMessage(ChatColor.RED + "❌ 没有待处理的传送请求！");
            return true;
        }

        Player senderPlayer = req.sender();
        if (senderPlayer.isOnline()) {
            senderPlayer.sendMessage(ChatColor.RED + "🚫 " + ChatColor.GOLD + player.getName() + ChatColor.RED + " 拒绝了你的传送请求。");
        }

        player.sendMessage(ChatColor.YELLOW + "🚫 已拒绝 " + ChatColor.GOLD + senderPlayer.getName() + ChatColor.YELLOW + " 的传送请求。");
        plugin.removeTpaRequest(player.getUniqueId());
        return true;
    }
}
