package com.example.playerhomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaHereCommand implements CommandExecutor {
    private final HomeFromSikadi233 plugin;
    public TpaHereCommand(HomeFromSikadi233 plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("只有玩家可用！"); return true; }
        Player player = (Player) sender;
        if (args.length < 1) { player.sendMessage(ChatColor.RED + "用法：/tpahere <玩家名>"); return true; }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) { player.sendMessage(ChatColor.RED + "玩家不在线！"); return true; }
        if (target.equals(player)) { player.sendMessage(ChatColor.RED + "不能传给自己！"); return true; }

        plugin.addTpaRequest(target.getUniqueId(),
                new HomeFromSikadi233.TpaRequest(player, target, HomeFromSikadi233.TpaType.TPAHERE, System.currentTimeMillis()));

        int timeout = plugin.getTpaTimeout();
        player.sendMessage(ChatColor.GREEN + "📨 已向 " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + " 发送TPAHere请求！(" + timeout + "秒)");

        target.sendMessage("");
        target.spigot().sendMessage(new ComponentBuilder("📨 ")
                .color(net.md_5.bungee.api.ChatColor.GREEN)
                .append(player.getName()).color(net.md_5.bungee.api.ChatColor.GOLD)
                .append(" 请求你传送到他那里！").color(net.md_5.bungee.api.ChatColor.GREEN).create());
        target.spigot().sendMessage(new ComponentBuilder("  [✔ 接受] ")
                .color(net.md_5.bungee.api.ChatColor.GREEN).bold(true)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("点击接受").color(net.md_5.bungee.api.ChatColor.GREEN).create()))
                .append("[✘ 拒绝] ").color(net.md_5.bungee.api.ChatColor.RED).bold(true)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("点击拒绝").color(net.md_5.bungee.api.ChatColor.RED).create()))
                .append("⏳ " + timeout + "秒").color(net.md_5.bungee.api.ChatColor.GRAY).create());
        target.sendMessage("");
        return true;
    }
}
