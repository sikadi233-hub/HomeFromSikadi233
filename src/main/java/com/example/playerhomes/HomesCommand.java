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
import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public HomesCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ========== /csd <玩家名> → OP 专属 ==========
        if (args.length >= 1) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "❌ 你没有权限查看别人的传送点！");
                return true;
            }
            String targetName = args[0];
            Map<String, Location> homes = plugin.getHomesByName(targetName);
            if (homes == null || homes.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + targetName + ChatColor.RED + " 没有任何传送点。");
                return true;
            }
            sender.sendMessage(ChatColor.GOLD + "========== " + targetName + " 的传送点 ==========");
            for (Map.Entry<String, Location> entry : homes.entrySet()) {
                String worldName = getWorldDisplayName(entry.getValue());
                sender.spigot().sendMessage(
                        new ComponentBuilder("  ")
                                .append(entry.getKey())
                                .color(net.md_5.bungee.api.ChatColor.WHITE)
                                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/csop " + targetName + " " + entry.getKey()))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new ComponentBuilder("点击传送到 ")
                                                .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                .append(targetName)
                                                .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                .append(" 的 ")
                                                .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                .append(entry.getKey())
                                                .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                .create()))
                                .append("  [" + worldName + "]")
                                .color(net.md_5.bungee.api.ChatColor.GRAY)
                                .create()
                );
            }
            sender.sendMessage(ChatColor.GOLD + "================================");
            return true;
        }

        // ========== /csd（自己的）==========
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "控制台请使用 /csd <玩家名>");
            return true;
        }

        Player player = (Player) sender;
        Set<String> ownNames = plugin.getHomeNames(player.getUniqueId());
        Map<String, Location> ownHomes = plugin.getHomesByName(player.getName());
        Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
        Map<String, Map<String, Location>> pending = plugin.getPendingShares(player.getUniqueId());

        boolean hasOwn = !ownNames.isEmpty();
        boolean hasShared = !shared.isEmpty();
        boolean hasPending = !pending.isEmpty();

        if (!hasOwn && !hasShared && !hasPending) {
            player.sendMessage(ChatColor.YELLOW + "你还没有任何传送点！");
            player.sendMessage(ChatColor.GRAY + "使用 /szcs <名称> 设置传送点");
            player.sendMessage(ChatColor.GRAY + "或让好友使用 /fxcs <你的名字> <传送点名> 分享给你");
            return true;
        }

        // ---- 自己的 ----
        if (hasOwn && ownHomes != null) {
            int used = ownHomes.size();
            int max = plugin.getMaxOwnHomes();
            player.sendMessage(ChatColor.GOLD + "========== 你的传送点 (" + used + "/" + max + ") ==========");
            for (Map.Entry<String, Location> entry : ownHomes.entrySet()) {
                String worldName = getWorldDisplayName(entry.getValue());
                player.spigot().sendMessage(
                        new ComponentBuilder("  ")
                                .append(entry.getKey())
                                .color(net.md_5.bungee.api.ChatColor.WHITE)
                                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/cs " + entry.getKey()))
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
        }

        // ---- 已接受的分享 ----
        if (hasShared) {
            int total = plugin.countSharedHomes(player.getUniqueId());
            int max = plugin.getMaxSharedHomes();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "========== 已接受的分享 (" + total + "/" + max + ") ==========");
            for (Map.Entry<String, Map<String, Location>> se : shared.entrySet()) {
                String sharer = se.getKey();
                for (Map.Entry<String, Location> he : se.getValue().entrySet()) {
                    String worldName = getWorldDisplayName(he.getValue());
                    player.spigot().sendMessage(
                            new ComponentBuilder("  ")
                                    .append(sharer)
                                    .color(net.md_5.bungee.api.ChatColor.AQUA)
                                    .append(" " + he.getKey())
                                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/cs " + sharer + " " + he.getKey()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            new ComponentBuilder("点击传送到 ")
                                                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                    .append(sharer)
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .append(" 分享的 ")
                                                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                    .append(he.getKey())
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .create()))
                                    .append("  [" + worldName + "]")
                                    .color(net.md_5.bungee.api.ChatColor.GRAY)
                                    .create()
                    );
                }
            }
        }

        // ---- 待处理的分享 ----
        if (hasPending) {
            player.sendMessage(ChatColor.YELLOW + "========== 待处理的分享请求 ==========");
            for (Map.Entry<String, Map<String, Location>> se : pending.entrySet()) {
                String sharer = se.getKey();
                for (Map.Entry<String, Location> he : se.getValue().entrySet()) {
                    String worldName = getWorldDisplayName(he.getValue());
                    player.sendMessage(ChatColor.AQUA + "  📨 " + sharer +
                            ChatColor.WHITE + " → " + he.getKey() +
                            ChatColor.GRAY + "  [" + worldName + "]");
                    // 可点击接受/拒绝
                    player.spigot().sendMessage(
                            new ComponentBuilder("    [✔ 接受]")
                                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                                    .bold(true)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/tycs " + sharer + " " + he.getKey()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            new ComponentBuilder("点击接受 ")
                                                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                    .append(sharer)
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .append(" 分享的 ")
                                                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                                                    .append(he.getKey())
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .create()))
                                    .append("    [✘ 拒绝]")
                                    .color(net.md_5.bungee.api.ChatColor.RED)
                                    .bold(true)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/btycs " + sharer + " " + he.getKey()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            new ComponentBuilder("点击拒绝 ")
                                                    .color(net.md_5.bungee.api.ChatColor.RED)
                                                    .append(sharer)
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .append(" 分享的 ")
                                                    .color(net.md_5.bungee.api.ChatColor.RED)
                                                    .append(he.getKey())
                                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                                    .create()))
                                    .create()
                    );
                }
            }
        }
        return true;
    }

    private String getWorldDisplayName(Location loc) {
        if (loc.getWorld() == null) return "未知";
        String name = loc.getWorld().getName();
        return switch (name) {
            case "world" -> "主世界";
            case "world_nether" -> "地狱";
            case "world_the_end" -> "末地";
            default -> name;
        };
    }
}
