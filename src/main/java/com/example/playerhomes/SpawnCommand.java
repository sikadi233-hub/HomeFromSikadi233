package com.example.playerhomes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final HomeFromSikadi233 plugin;

    public SpawnCommand(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家才能使用这个命令！");
            return true;
        }

        Player player = (Player) sender;
        Location spawn = plugin.getSpawn();

        if (spawn == null) {
            spawn = player.getWorld().getSpawnLocation();
        }

        plugin.setBackInProgress(player.getUniqueId(), true);
        player.teleport(spawn);
        plugin.setBackInProgress(player.getUniqueId(), false);

        player.sendMessage(ChatColor.GREEN + "🏁 已传送到出生点！");
        return true;
    }
}
