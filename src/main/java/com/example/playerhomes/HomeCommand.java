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
import org.bukkit.scheduler.BukkitTask;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class HomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;
    // 缓存预热监听器
    private final Map<UUID, org.bukkit.event.Listener> moveListeners = new HashMap<>();

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
        if (plugin.blockIfInBattle(player)) return true;
        // /cs 无参数 → 显示列表
        if (args.length == 0) {
            showHomeList(player);
            return true;
        }

        // 冷却检查（OP免疫）
        if (!player.isOp()) {
            int remaining = plugin.getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(plugin.getMsgError() + "⏳ 传送冷却中！请等待 " +
                        plugin.getMsgPrimary() + remaining +
                        plugin.getMsgError() + " 秒后再试。");
                return true;
            }
        }

        Location destination = null;

        // /cs <玩家> <名>
        if (args.length >= 2) {
            destination = plugin.getSharedHome(player.getUniqueId(), args[0], args[1].toLowerCase());
            if (destination == null) {
                destination = plugin.getOwnHome(player.getUniqueId(), (args[0] + " " + args[1]).toLowerCase());
            }
        } else {
            destination = plugin.getOwnHome(player.getUniqueId(), args[0]);
            if (destination == null) {
                var shared = plugin.getSharedHomes(player.getUniqueId());
                if (shared.containsKey(args[0]) && shared.get(args[0]).size() == 1) {
                    destination = shared.get(args[0]).values().iterator().next();
                }
            }
        }

        if (destination == null) {
            player.sendMessage(plugin.getMsgError() + "❌ 传送点不存在！");
            return true;
        }

        // 取消旧预热
        var warmupTasks = plugin.getWarmupTasks();
        BukkitTask old = warmupTasks.remove(player.getUniqueId());
        if (old != null) old.cancel();
        org.bukkit.event.Listener oldListener = moveListeners.remove(player.getUniqueId());
        if (oldListener != null) org.bukkit.event.HandlerList.unregisterAll(oldListener);

        int warmup = plugin.getHomeWarmup();
        if (warmup <= 0 || player.isOp()) {
            player.teleport(destination);
            if (!player.isOp()) plugin.recordHomeUse(player.getUniqueId());
            player.sendMessage(plugin.getMsgSuccess() + "🏠 已传送！" +
                    plugin.getMsgInfo() + " (冷却" + plugin.getHomeCooldown() + "秒)");
            return true;
        }

        final Location dest = destination;
        player.sendMessage(plugin.getMsgPrimary() + "⏳ " + warmup + "秒后传送，请勿移动...");

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            warmupTasks.remove(player.getUniqueId());
            org.bukkit.event.Listener l = moveListeners.remove(player.getUniqueId());
            if (l != null) org.bukkit.event.HandlerList.unregisterAll(l);
            player.teleport(dest);
            plugin.recordHomeUse(player.getUniqueId());
            player.sendMessage(plugin.getMsgSuccess() + "🏠 已传送！" +
                    plugin.getMsgInfo() + " (冷却" + plugin.getHomeCooldown() + "秒)");
        }, warmup * 20L);

        warmupTasks.put(player.getUniqueId(), task);

        // 移动监听
        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (!e.getPlayer().equals(player)) return;
                if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                        && e.getFrom().getBlockY() == e.getTo().getBlockY()
                        && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
                BukkitTask t = warmupTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                    this.unregister();
                    player.sendMessage(plugin.getMsgError() + "❌ 你移动了，传送已取消！");
                }
            }
            void unregister() {
                moveListeners.remove(player.getUniqueId());
                org.bukkit.event.HandlerList.unregisterAll(this);
            }
        };
        moveListeners.put(player.getUniqueId(), listener);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        return true;
    }

    private void showHomeList(Player player) {
        Map<String, Location> ownHomes = plugin.getHomesByName(player.getName());
        Map<String, Map<String, Location>> shared = plugin.getSharedHomes(player.getUniqueId());
        int ownCount = (ownHomes != null) ? ownHomes.size() : 0;
        int sharedCount = plugin.countSharedHomes(player.getUniqueId());

        if (ownCount == 0 && sharedCount == 0) {
            player.sendMessage(plugin.getMsgPrimary() + "你还没有任何传送点！");
            return;
        }

        player.sendMessage(plugin.getMsgPrimary() + "========== 传送点列表 " +
                plugin.getMsgInfo() + "(" + (ownCount + sharedCount) + "个) ==========");

        int remaining = plugin.getRemainingCooldown(player.getUniqueId());
        if (remaining > 0 && !player.isOp()) {
            player.sendMessage(plugin.getMsgError() + "⏳ 冷却中：" + remaining + "秒");
        }

        if (ownHomes != null) {
            for (var e : ownHomes.entrySet()) {
                player.spigot().sendMessage(
                        new ComponentBuilder("  ").append(e.getKey())
                                .color(net.md_5.bungee.api.ChatColor.WHITE)
                                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cs " + e.getKey()))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new ComponentBuilder("点击传送").color(net.md_5.bungee.api.ChatColor.GREEN).create()))
                                .append("  [" + getWorldName(e.getValue()) + "]")
                                .color(net.md_5.bungee.api.ChatColor.GRAY).create()
                );
            }
        }

        if (!shared.isEmpty()) {
            for (var se : shared.entrySet())
                for (var he : se.getValue().entrySet())
                    player.spigot().sendMessage(
                            new ComponentBuilder("  ").append(se.getKey())
                                    .color(net.md_5.bungee.api.ChatColor.AQUA)
                                    .append(" " + he.getKey()).color(net.md_5.bungee.api.ChatColor.WHITE)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cs " + se.getKey() + " " + he.getKey()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            new ComponentBuilder("点击传送").color(net.md_5.bungee.api.ChatColor.GREEN).create()))
                                    .append("  [" + getWorldName(he.getValue()) + "]")
                                    .color(net.md_5.bungee.api.ChatColor.GRAY).create()
                    );
        }
        player.sendMessage(plugin.getMsgPrimary() + "==========================================");
    }

    private String getWorldName(Location loc) {
        if (loc.getWorld() == null) return "未知";
        return switch (loc.getWorld().getName()) {
            case "world" -> "主世界";
            case "world_nether" -> "地狱";
            case "world_the_end" -> "末地";
            default -> loc.getWorld().getName();
        };
    }
}
