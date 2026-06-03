package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAcceptCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public TpAcceptCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

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
        if (!senderPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "❌ 请求者已下线！");
            plugin.removeTpaRequest(player.getUniqueId());
            return true;
        }

        if (req.type() == HomeFromSikadi233.TpaType.TPA) {
            // /tpa → 请求者传到目标
            senderPlayer.teleport(player);
            senderPlayer.sendMessage(ChatColor.GREEN + "🏠 " + ChatColor.GOLD + player.getName() + ChatColor.GREEN + " 接受了你的TPA请求！");
            player.sendMessage(ChatColor.GREEN + "✅ 已接受TPA请求，" + ChatColor.GOLD + senderPlayer.getName() + ChatColor.GREEN + " 已传送到你这里！");
        } else {
            // /tpahere → 目标传到请求者
            player.teleport(senderPlayer);
            senderPlayer.sendMessage(ChatColor.GREEN + "🏠 " + ChatColor.GOLD + player.getName() + ChatColor.GREEN + " 接受了你的TPAHere请求！");
            player.sendMessage(ChatColor.GREEN + "✅ 已接受TPAHere请求，你已传送到 " + ChatColor.GOLD + senderPlayer.getName() + "！");
        }

        plugin.removeTpaRequest(player.getUniqueId());
        return true;
    }
}
