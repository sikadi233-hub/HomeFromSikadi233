package com.example.playerhomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShareHomeCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public ShareHomeCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player sharer = (Player) sender;

        if (args.length < 2) {
            sharer.sendMessage(ChatColor.RED + "用法：/fxcs <玩家名> <传送点名>");
            sharer.sendMessage(ChatColor.GRAY + "例如：/fxcs Alex 家");
            return true;
        }

        String targetName = args[0];
        String homeName = args[1].toLowerCase();

        String error = plugin.requestShare(sharer, targetName, homeName);

        if (error != null) {
            sharer.sendMessage(ChatColor.RED + "❌ " + error);
            return true;
        }

        int timeout = plugin.getShareTimeout();

        // ===== 分享者看到的消息 =====
        sharer.sendMessage(ChatColor.GREEN + "📨 已向 " +
                ChatColor.GOLD + targetName +
                ChatColor.GREEN + " 发送分享请求！（传送点: " +
                ChatColor.GOLD + homeName + ChatColor.GREEN + "）");
        sharer.sendMessage(ChatColor.GRAY + "等待对方接受...（" + timeout + "秒后自动取消）");

        // ===== 目标玩家看到的消息（可点击） =====
        Player target = plugin.getServer().getPlayer(targetName);
        if (target != null) {
            target.sendMessage("");

            target.spigot().sendMessage(
                    new ComponentBuilder("🎁 ")
                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                            .append(sharer.getName())
                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                            .append(" 想分享传送点 ")
                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                            .append(homeName)
                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                            .append(" 给你！")
                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                            .create()
            );

            target.spigot().sendMessage(
                    new ComponentBuilder("  ")
                            .append("[✔ 接受]")
                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                            .bold(true)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tycs " + sharer.getName() + " " + homeName))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("点击接受 ")
                                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                                            .append(sharer.getName())
                                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                                            .append(" 分享的 ")
                                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                                            .append(homeName)
                                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                                            .create()))
                            .append("    ")
                            .reset()
                            .append("[✘ 拒绝]")
                            .color(net.md_5.bungee.api.ChatColor.RED)
                            .bold(true)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/btycs " + sharer.getName() + " " + homeName))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("点击拒绝 ")
                                            .color(net.md_5.bungee.api.ChatColor.RED)
                                            .append(sharer.getName())
                                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                                            .append(" 分享的 ")
                                            .color(net.md_5.bungee.api.ChatColor.RED)
                                            .append(homeName)
                                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                                            .create()))
                            .append("     ")
                            .reset()
                            .append("⏳ " + timeout + "秒后过期")
                            .color(net.md_5.bungee.api.ChatColor.GRAY)
                            .create()
            );

            target.sendMessage("");
        }

        return true;
    }
}
