package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public BackCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        // 冷却检查（OP 免疫）
        if (!player.isOp()) {
            int remaining = plugin.getBackRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(plugin.getMsgError() + "⏳ /back 冷却中！请等待 " +
                        plugin.getMsgPrimary() + remaining +
                        plugin.getMsgError() + " 秒后再试。");
                return true;
            }
        }


        // ========== /back death → 回到死亡点 ==========
        if (args.length >= 1 && args[0].equalsIgnoreCase("death")) {
            Location deathLoc = plugin.getDeathLocation(player.getUniqueId());

            if (deathLoc == null) {
                player.sendMessage(ChatColor.RED + "❌ 没有死亡记录！");
                return true;
            }

            plugin.setBackInProgress(player.getUniqueId(), true);
            player.teleport(deathLoc);
            plugin.setBackInProgress(player.getUniqueId(), false);
            plugin.recordBackUse(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "💀 已传送到上次死亡点！" +
                    ChatColor.GRAY + " (" + plugin.getBackCooldown() + "秒冷却)");
            player.sendMessage(ChatColor.GRAY + "  坐标: " +
                    ChatColor.WHITE + deathLoc.getBlockX() + ", " +
                    deathLoc.getBlockY() + ", " + deathLoc.getBlockZ());
            return true;
        }

        // ========== /back → 回到上次传送位置 ==========
        Location lastLoc = plugin.getLastTeleportLocation(player.getUniqueId());

        if (lastLoc == null) {
            player.sendMessage(ChatColor.RED + "❌ 没有传送记录！");
            player.sendMessage(ChatColor.GRAY + "使用传送指令后 /back 才能使用");
            return true;
        }

        plugin.setBackInProgress(player.getUniqueId(), true);
        player.teleport(lastLoc);
        plugin.setBackInProgress(player.getUniqueId(), false);
        plugin.recordBackUse(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "↩️ 已回到上次传送位置！" +
                ChatColor.GRAY + " (" + plugin.getBackCooldown() + "秒冷却)");
        player.sendMessage(ChatColor.GRAY + "  坐标: " +
                ChatColor.WHITE + lastLoc.getBlockX() + ", " +
                lastLoc.getBlockY() + ", " + lastLoc.getBlockZ());

        return true;
    }
}
