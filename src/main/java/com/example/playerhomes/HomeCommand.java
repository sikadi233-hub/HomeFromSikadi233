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

public class HomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public HomeCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        // ========== /cs（无参数）→ 显示传送点列表（不看冷却）==========
        if (args.length == 0) {
            showHomeList(player);
            return true;
        }

        // ========== 冷却检查 ==========
        int remaining = plugin.getRemainingCooldown(player.getUniqueId());
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "⏳ 传送冷却中！请等待 " +
                    ChatColor.GOLD + remaining +
                    ChatColor.RED + " 秒后再试。");
            return true;
        }

        // ========== /cs <分享者> <传送点名> → 分享的传送点 ==========
        if (args.length >= 2) {
            String sharerName = args[0];
            String homeName = args[1].toLowerCase();
            Location loc = plugin.getSharedHome(player.getUniqueId(), sharerName, homeName);

            if (loc != null) {
                player.teleport(loc);
                plugin.recordHomeUse(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "🏠 已传送到 " +
                        ChatColor.GOLD + sharerName +
                        ChatColor.GREEN + " 分享的 " +
                        ChatColor.GOLD + homeName + "！" +
                        ChatColor.GRAY + " (" + plugin.getHomeCooldown() + "秒冷却)");
                return true;
            }

            String combinedName = (args[0] + " " + args[1]).toLowerCase();
            Location ownLoc = plugin.getOwnHome(player.getUniqueId(), combinedName);
            if (ownLoc != null) {
                player.teleport(ownLoc);
                plugin.recordHomeUse(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "🏠 已传送到 " +
                        ChatColor.GOLD + combinedName + "！" +
                        ChatColor.GRAY + " (" + plugin.getHomeCooldown() + "秒冷却)");
                return true;
            }

            player.sendMessage(ChatColor.RED + "❌ 传送点不存在！");
            player.sendMessage(ChatColor.GRAY + "使用 /cs 查看传送点列表");
            return true;
        }

        // ========== /cs <名称> → 自己的或分享者 ==========
        String homeName = args[0];

        Location ownLoc = plugin.getOwnHome(player.getUniqueId(), homeName);
        if (ownLoc != null) {
            player.teleport(ownLoc);
            plugin.recordHomeUse(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "🏠 已传送到 " +
                    ChatColor.GOLD + homeName + "！" +
                    ChatColor.GRAY + " (" + plugin.getHomeCooldown() + "秒冷却)");
            return true;
        }

        Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
        if (shared.containsKey(homeName)) {
            Map<String, Location> sharedHomes = shared.get(homeName);
            if (sharedHomes.size() == 1) {
                Map.Entry<String, Location> entry = sharedHomes.entrySet().iterator().next();
                player.teleport(entry.getValue());
                plugin.recordHomeUse(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "🏠 已传送到 " +
                        ChatColor.GOLD + homeName +
                        ChatColor.GREEN + " 分享的 " +
                        ChatColor.GOLD + entry.getKey() + "！" +
                        ChatColor.GRAY + " (" + plugin.getHomeCooldown() + "秒冷却)");
                return true;
            } else {
                player.sendMessage(ChatColor.GOLD + homeName + ChatColor.YELLOW + " 分享了多个传送点给你：");
                for (String hName : sharedHomes.keySet()) {
                    player.sendMessage(ChatColor.GRAY + "  → /cs " + homeName + " " + hName);
                }
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "❌ 传送点 " +
                ChatColor.GOLD + homeName +
                ChatColor.RED + " 不存在！");
        player.sendMessage(ChatColor.GRAY + "使用 /cs 查看传送点列表");
        return true;
    }

    private void showHomeList(Player player) {
        Map<String, Location> ownHomes = plugin.getHomesByName(player.getName());
        Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());

        int ownCount = (ownHomes != null) ? ownHomes.size() : 0;
        int sharedCount = plugin.countSharedHomes(player.getUniqueId());
        int remaining = plugin.getRemainingCooldown(player.getUniqueId());

        if (ownCount == 0 && sharedCount == 0) {
            player.sendMessage(ChatColor.YELLOW + "你还没有任何传送点！");
            player.sendMessage(ChatColor.GRAY + "使用 /szcs <名称> 设置传送点");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "========== 传送点列表 " +
                ChatColor.GRAY + "(" + (ownCount + sharedCount) + "个) " +
                ChatColor.GOLD + "==========");

        // 冷却提示
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "⏳ 冷却中：" + remaining + "秒后可传送");
        } else {
            player.sendMessage(ChatColor.GREEN + "✅ 可以传送 (冷却" + plugin.getHomeCooldown() + "秒)");
        }

        // 自己的
        if (ownHomes != null && !ownHomes.isEmpty()) {
            for (Map.Entry<String, Location> entry : ownHomes.entrySet()) {
                String worldName = getWorldDisplayName(entry.getValue());
                if (remaining > 0) {
                    player.sendMessage(ChatColor.WHITE + "  " + entry.getKey() +
                            ChatColor.GRAY + "  [" + worldName + "]" +
                            ChatColor.RED + "  (冷却中)");
                } else {
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
        }

        // 分享的
        if (!shared.isEmpty()) {
            for (Map.Entry<String, Map<String, Location>> se : shared.entrySet()) {
                String sharer = se.getKey();
                for (Map.Entry<String, Location> he : se.getValue().entrySet()) {
                    String worldName = getWorldDisplayName(he.getValue());
                    if (remaining > 0) {
                        player.sendMessage(ChatColor.AQUA + "  " + sharer + " " +
                                ChatColor.WHITE + he.getKey() +
                                ChatColor.GRAY + "  [" + worldName + "]" +
                                ChatColor.RED + "  (冷却中)");
                    } else {
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
        }
        player.sendMessage(ChatColor.GOLD + "==========================================");
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
