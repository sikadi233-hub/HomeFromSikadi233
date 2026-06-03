package com.example.playerhomes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RtpCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;
    private final Random random = new Random();

    public RtpCommand(HomeFromSikadi233 plugin) {
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
            int remaining = plugin.getRtpRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                String timeStr = minutes > 0 ? minutes + "分" + seconds + "秒" : seconds + "秒";
                player.sendMessage(plugin.getMsgError() + "⏳ RTP冷却中！请等待 " +
                        plugin.getMsgPrimary() + timeStr +
                        plugin.getMsgError() + " 后再试。");
                return true;
            }
        }


        int minR = plugin.getRtpMinRadius();
        int maxR = plugin.getRtpMaxRadius();

        player.sendMessage(ChatColor.YELLOW + "🔍 正在寻找安全位置... (" +
                ChatColor.GRAY + "范围: " + minR + "-" + maxR + "格" +
                ChatColor.YELLOW + ")");

        World world = player.getWorld();
        Location center = player.getLocation();
        int cooldown = plugin.getRtpCooldown();

        // ⬇️ 异步加载区块，不卡主线程
        findSafeAsync(world, center, minR, maxR, 10).thenAccept(safeLocation -> {
            if (safeLocation == null) {
                // 回主线程发消息
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "❌ 找不到安全位置！请重试。"));
                return;
            }
            // 回主线程传送
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.recordRtpUse(player.getUniqueId());
                String cdStr = cooldown >= 60 ? (cooldown / 60) + "分钟" : cooldown + "秒";
                player.teleport(safeLocation);
                player.sendMessage(ChatColor.GREEN + "🎲 随机传送成功！");
                player.sendMessage(ChatColor.GRAY + "  坐标: " +
                        ChatColor.WHITE + safeLocation.getBlockX() + ", " +
                        safeLocation.getBlockZ() +
                        ChatColor.GRAY + " (冷却" + cdStr + ")");
            });
        });

        return true;
    }

    /**
     * 异步递归查找安全位置
     */
    private CompletableFuture<Location> findSafeAsync(World world, Location center,
                                                      int minR, int maxR, int attempts) {
        if (attempts <= 0) return CompletableFuture.completedFuture(null);

        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = minR + random.nextDouble() * (maxR - minR);
        int tx = center.getBlockX() + (int) (Math.cos(angle) * dist);
        int tz = center.getBlockZ() + (int) (Math.sin(angle) * dist);

        // ⬇️ Paper 异步加载区块（不卡主线程）
        return world.getChunkAtAsync(tx >> 4, tz >> 4).thenCompose(chunk -> {
            // 区块已加载，getHighestBlockYAt 不会阻塞
            int y = world.getHighestBlockYAt(tx, tz) + 1;
            Location loc = new Location(world, tx + 0.5, y, tz + 0.5);
            if (isSafe(world, loc)) return CompletableFuture.completedFuture(loc);
            // 不安全，重试
            return findSafeAsync(world, center, minR, maxR, attempts - 1);
        });
    }

    private boolean isSafe(World world, Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Block ground = world.getBlockAt(x, y - 1, z);
        if (!ground.getType().isSolid()) return false;

        Material gt = ground.getType();
        if (gt == Material.LAVA || gt == Material.CACTUS ||
                gt == Material.MAGMA_BLOCK ||
                gt.name().contains("CAMPFIRE") ||
                gt == Material.SWEET_BERRY_BUSH) return false;

        if (!world.getBlockAt(x, y, z).getType().isAir()) return false;
        if (!world.getBlockAt(x, y + 1, z).getType().isAir()) return false;
        if (world.getBlockAt(x, y, z).getType() == Material.WATER) return false;

        return true;
    }
}
