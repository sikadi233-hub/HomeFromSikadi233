package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public SetSpawnCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ 你没有权限！(仅限OP)");
            return true;
        }

        plugin.setSpawn(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "✅ 出生点已设置为当前位置！");
        return true;
    }
}
