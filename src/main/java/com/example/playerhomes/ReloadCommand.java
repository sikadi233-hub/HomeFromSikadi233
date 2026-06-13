package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public ReloadCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !((Player) sender).isOp()) {
            sender.sendMessage(ChatColor.RED + "❌ 你没有权限！(仅限OP)");
            return true;
        }

        plugin.reloadConfig();
        // 重新读配置
        plugin.setConfigValues();
        sender.sendMessage(plugin.getMsgSuccess() + "✅ 配置已重载！");
        return true;
    }
}
