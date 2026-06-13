package com.example.playerhomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class WarpListCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public WarpListCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;

        Map<String, Location> warps = plugin.getWarps();
        if (warps.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "还没有任何地标！");
            if (player.isOp()) player.sendMessage(ChatColor.GRAY + "使用 /setwarp <名称> 设置");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "========== 地标列表 (" + warps.size() + "个) ==========");
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            String worldName = getWorldDisplayName(entry.getValue());
            player.spigot().sendMessage(
                    new ComponentBuilder("  ")
                            .append(entry.getKey())
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + entry.getKey()))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("点击传送到 ")
                                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                                            .append(entry.getKey())
                                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                                            .create()))
                            .append("  [" + worldName + "]")
                            .color(net.md_5.bungee.api.ChatColor.GRAY)
                            .create()
            );
        }
        player.sendMessage(ChatColor.GOLD + "==============================" + "==============");
        return true;
    }

    private String getWorldDisplayName(Location loc) {
        if (loc.getWorld() == null) return "未知";
        return switch (loc.getWorld().getName()) {
            case "world" -> "主世界";
            case "world_nether" -> "地狱";
            case "world_the_end" -> "末地";
            default -> loc.getWorld().getName();
        };
    }
}
