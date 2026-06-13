package com.example.playerhomes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;

public final class HomeFromSikadi233 extends JavaPlugin implements Listener {

    // ==================== 数据存储 ====================

    // 自己的传送点: UUID → {名 → 坐标}
    private final Map<UUID, Map<String, Location>> homesMap = new HashMap<>();
    // 已接受的分享: 接收者UUID → {分享者名 → {名 → 坐标}}
    private final Map<UUID, Map<String, Map<String, Location>>> sharedHomes = new HashMap<>();
    // 待处理分享
    private final Map<UUID, Map<String, Map<String, Location>>> pendingShares = new HashMap<>();
    // 自动取消任务
    private final Map<UUID, Map<String, Map<String, BukkitTask>>> cancelTasks = new HashMap<>();

    // TPA
    private final Map<UUID, TpaRequest> tpaRequests = new HashMap<>();
    private final Map<UUID, BukkitTask> tpaCancelTasks = new HashMap<>();

    // Warp
    private final Map<String, Location> warps = new HashMap<>();

    // Spawn
    private Location spawnLocation;

    // 冷却
    private final Map<UUID, Long> lastHomeTime = new HashMap<>();
    private final Map<UUID, Long> lastRtpTime = new HashMap<>();
    private final Map<UUID, Long> lastBackTime = new HashMap<>();

    // /back 追踪
    private final Map<UUID, Location> lastTeleportLocation = new HashMap<>();
    private final Map<UUID, Location> deathLocation = new HashMap<>();
    private final Set<UUID> backInProgress = new HashSet<>();

    // 传送预热
    private final Map<UUID, BukkitTask> warmupTasks = new HashMap<>();

    // MySQL
    private DatabaseManager database;

    // 配置值
    private int maxOwnHomes = 4;
    private int maxSharedHomes = 3;
    private int shareTimeout = 30;
    private int homeCooldown = 5;
    private int homeWarmup = 3;
    private int tpaTimeout = 60;
    private int rtpCooldown = 300;
    private int rtpMinRadius = 500;
    private int rtpMaxRadius = 3000;
    private int backCooldown = 10;

    // 消息颜色
    private ChatColor msgPrimary = ChatColor.GOLD;
    private ChatColor msgSuccess = ChatColor.GREEN;
    private ChatColor msgError = ChatColor.RED;
    private ChatColor msgInfo = ChatColor.GRAY;

    // 进服欢迎
    private boolean welcomeMessage = true;

    // ==================== 数据结构 ====================

    public enum TpaType { TPA, TPAHERE }
    public record TpaRequest(Player sender, Player target, TpaType type, long time) {}

    // ==================== 生命周期 ====================

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setConfigValues();

        database = new DatabaseManager(this);
        loadAll();

        // ---- 注册命令 ----
        getCommand("szcs").setExecutor(new SetHomeCommand(this));
        getCommand("szcs").setTabCompleter(new SetHomeTabCompleter(this));
        getCommand("cs").setExecutor(new HomeCommand(this));
        getCommand("cs").setTabCompleter(new HomeTabCompleter(this));
        getCommand("sccs").setExecutor(new DelHomeCommand(this));
        getCommand("sccs").setTabCompleter(new DelHomeTabCompleter(this));
        getCommand("csd").setExecutor(new HomesCommand(this));
        getCommand("csd").setTabCompleter(new HomesTabCompleter());
        getCommand("csop").setExecutor(new HomeOfCommand(this));
        getCommand("csop").setTabCompleter(new HomeOfTabCompleter(this));
        getCommand("fxcs").setExecutor(new ShareHomeCommand(this));
        getCommand("fxcs").setTabCompleter(new ShareHomeTabCompleter(this));
        getCommand("tycs").setExecutor(new AcceptHomeCommand(this));
        getCommand("tycs").setTabCompleter(new AcceptHomeTabCompleter(this));
        getCommand("btycs").setExecutor(new DeclineHomeCommand(this));
        getCommand("btycs").setTabCompleter(new DeclineHomeTabCompleter(this));

        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpa").setTabCompleter(new TpaTabCompleter());
        getCommand("tpahere").setExecutor(new TpaHereCommand(this));
        getCommand("tpahere").setTabCompleter(new TpaTabCompleter());
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this));

        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("setwarp").setTabCompleter(new WarpTabCompleter(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("warp").setTabCompleter(new WarpTabCompleter(this));
        getCommand("warplist").setExecutor(new WarpListCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("delwarp").setTabCompleter(new WarpTabCompleter(this));

        getCommand("rtp").setExecutor(new RtpCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("back").setTabCompleter(new BackTabCompleter());

        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));

        getCommand("hfsreload").setExecutor(new ReloadCommand(this));

        // ---- 事件 ----
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("HomeFromSikadi233 v3.1 已启动！");
    }

    @Override
    public void onDisable() {
        for (var bySharer : cancelTasks.values())
            for (var homes : bySharer.values())
                for (BukkitTask task : homes.values()) task.cancel();
        cancelTasks.clear();
        for (BukkitTask task : tpaCancelTasks.values()) task.cancel();
        tpaCancelTasks.clear();
        for (BukkitTask task : warmupTasks.values()) task.cancel();
        warmupTasks.clear();
        saveAll();
        database.close();
        getLogger().info("HomeFromSikadi233 已关闭。");
    }

    // ==================== 配置 ====================

    public void setConfigValues() {
        maxOwnHomes = getConfig().getInt("max-own-homes", 4);
        maxSharedHomes = getConfig().getInt("max-shared-homes", 3);
        shareTimeout = getConfig().getInt("share-timeout", 30);
        homeCooldown = getConfig().getInt("home-cooldown", 5);
        homeWarmup = getConfig().getInt("home-warmup", 3);
        tpaTimeout = getConfig().getInt("tpa-timeout", 60);
        rtpCooldown = getConfig().getInt("rtp-cooldown", 300);
        rtpMinRadius = getConfig().getInt("rtp-min-radius", 500);
        rtpMaxRadius = getConfig().getInt("rtp-max-radius", 3000);
        backCooldown = getConfig().getInt("back-cooldown", 10);
        welcomeMessage = getConfig().getBoolean("welcome-message", true);
        try { msgPrimary = ChatColor.valueOf(getConfig().getString("msg-primary", "GOLD")); } catch (Exception e) {}
        try { msgSuccess = ChatColor.valueOf(getConfig().getString("msg-success", "GREEN")); } catch (Exception e) {}
        try { msgError = ChatColor.valueOf(getConfig().getString("msg-error", "RED")); } catch (Exception e) {}
        try { msgInfo = ChatColor.valueOf(getConfig().getString("msg-info", "GRAY")); } catch (Exception e) {}
    }

    // ==================== 自己的传送点 ====================

    public String setHome(UUID uuid, String name, Location loc) {
        Map<String, Location> playerHomes = homesMap.computeIfAbsent(uuid, k -> new HashMap<>());
        if (!playerHomes.containsKey(name) && playerHomes.size() >= maxOwnHomes)
            return "已达上限(" + maxOwnHomes + "个)！先删除一些。";
        playerHomes.put(name, loc.clone());
        if (database.isEnabled()) saveHomeToDB(uuid, name, loc);
        saveHomesToFile();
        return null;
    }

    public Location getOwnHome(UUID uuid, String name) {
        Map<String, Location> map = homesMap.get(uuid);
        return map == null ? null : map.get(name);
    }

    public String deleteHome(UUID uuid, String name) {
        Map<String, Location> own = homesMap.get(uuid);
        if (own != null && own.remove(name) != null) {
            if (database.isEnabled()) deleteHomeFromDB(uuid, name);
            saveHomesToFile();
            return "own";
        }
        Map<String, Map<String, Location>> shared = sharedHomes.get(uuid);
        if (shared != null) {
            for (var e : shared.entrySet()) {
                if (e.getValue().remove(name) != null) {
                    if (database.isEnabled()) deleteSharedFromDB(uuid, e.getKey(), name);
                    if (e.getValue().isEmpty()) shared.remove(e.getKey());
                    saveSharedToFile();
                    return "shared";
                }
            }
        }
        return null;
    }

    public Set<String> getHomeNames(UUID uuid) {
        Map<String, Location> map = homesMap.get(uuid);
        return map == null ? Collections.emptySet() : map.keySet();
    }

    public int getMaxOwnHomes() { return maxOwnHomes; }
    public int countOwnHomes(UUID uuid) {
        Map<String, Location> map = homesMap.get(uuid);
        return map == null ? 0 : map.size();
    }

    // ==================== 已接受的分享 ====================

    public Location getSharedHome(UUID receiverUUID, String sharerName, String homeName) {
        var bySharer = sharedHomes.get(receiverUUID);
        if (bySharer == null) return null;
        var homes = bySharer.get(sharerName);
        return homes == null ? null : homes.get(homeName);
    }

    public int countSharedHomes(UUID receiverUUID) {
        var bySharer = sharedHomes.get(receiverUUID);
        if (bySharer == null) return 0;
        int c = 0;
        for (var homes : bySharer.values()) c += homes.size();
        return c;
    }

    public Map<String, Map<String, Location>> getSharedHomes(UUID receiverUUID) {
        var map = sharedHomes.get(receiverUUID);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    public int getMaxSharedHomes() { return maxSharedHomes; }

    // ==================== 待处理分享 ====================

    public String requestShare(Player sharer, String targetName, String homeName) {
        Location loc = getOwnHome(sharer.getUniqueId(), homeName);
        if (loc == null) return "你没有名为 " + homeName + " 的传送点！";
        UUID targetUUID = getUUIDByName(targetName);
        if (targetUUID == null) return "找不到玩家 " + targetName;
        if (targetUUID.equals(sharer.getUniqueId())) return "不能分享给自己！";

        pendingShares.computeIfAbsent(targetUUID, k -> new HashMap<>())
                .computeIfAbsent(sharer.getName(), k -> new HashMap<>())
                .put(homeName, loc.clone());
        savePendingToFile();

        UUID receiverId = targetUUID;
        String sName = sharer.getName();
        String hName = homeName;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this,
                () -> autoCancelShare(receiverId, sName, hName), shareTimeout * 20L);
        cancelTasks.computeIfAbsent(targetUUID, k -> new HashMap<>())
                .computeIfAbsent(sharer.getName(), k -> new HashMap<>()).put(homeName, task);
        return null;
    }

    public String acceptShare(UUID receiverUUID, String sharerName, String homeName) {
        var pending = pendingShares.get(receiverUUID);
        if (pending == null) return "没有来自 " + sharerName + " 的待处理分享！（可能已过期）";
        var homes = pending.get(sharerName);
        if (homes == null) return "没有来自 " + sharerName + " 的待处理分享！（可能已过期）";
        Location loc = homes.get(homeName);
        if (loc == null) return sharerName + " 没有分享名为 " + homeName + " 的传送点给你！（可能已过期）";
        if (countSharedHomes(receiverUUID) >= maxSharedHomes)
            return "你已接收" + maxSharedHomes + "个分享（上限）！";

        cancelAutoCancelTask(receiverUUID, sharerName, homeName);
        homes.remove(homeName);
        if (homes.isEmpty()) pending.remove(sharerName);
        if (pending.isEmpty()) pendingShares.remove(receiverUUID);
        savePendingToFile();

        sharedHomes.computeIfAbsent(receiverUUID, k -> new HashMap<>())
                .computeIfAbsent(sharerName, k -> new HashMap<>()).put(homeName, loc);
        if (database.isEnabled()) saveSharedToDB(receiverUUID, sharerName, homeName, loc);
        saveSharedToFile();
        return null;
    }

    public String declineShare(UUID receiverUUID, String sharerName, String homeName) {
        var pending = pendingShares.get(receiverUUID);
        if (pending == null) return "没有来自 " + sharerName + " 的待处理分享！（可能已过期）";
        var homes = pending.get(sharerName);
        if (homes == null) return "没有来自 " + sharerName + " 的待处理分享！（可能已过期）";
        if (homes.remove(homeName) == null)
            return sharerName + " 没有分享名为 " + homeName + " 的传送点给你！（可能已过期）";
        cancelAutoCancelTask(receiverUUID, sharerName, homeName);
        if (homes.isEmpty()) pending.remove(sharerName);
        if (pending.isEmpty()) pendingShares.remove(receiverUUID);
        savePendingToFile();
        return null;
    }

    private void autoCancelShare(UUID r, String s, String h) {
        var pending = pendingShares.get(r);
        if (pending == null) return;
        var homes = pending.get(s);
        if (homes == null || !homes.containsKey(h)) return;
        homes.remove(h);
        if (homes.isEmpty()) pending.remove(s);
        if (pending.isEmpty()) pendingShares.remove(r);
        savePendingToFile();
        cleanCancelTaskEntry(r, s, h);
        Player rec = Bukkit.getPlayer(r);
        if (rec != null) rec.sendMessage("§e⏰ " + s + " §e分享的 §6" + h + " §e已过期。");
        Player shar = Bukkit.getPlayer(s);
        if (shar != null) shar.sendMessage("§e⏰ 分享给 " + (rec != null ? rec.getName() : "玩家") + " 的 §6" + h + " §e已过期(" + shareTimeout + "秒)。");
    }

    private void cancelAutoCancelTask(UUID r, String s, String h) {
        var by = cancelTasks.get(r);
        if (by == null) return;
        var homes = by.get(s);
        if (homes == null) return;
        BukkitTask t = homes.remove(h);
        if (t != null) { t.cancel(); if (homes.isEmpty()) by.remove(s); if (by.isEmpty()) cancelTasks.remove(r); }
    }

    private void cleanCancelTaskEntry(UUID r, String s, String h) {
        var by = cancelTasks.get(r);
        if (by == null) return;
        var homes = by.get(s);
        if (homes == null) return;
        homes.remove(h);
        if (homes.isEmpty()) by.remove(s);
        if (by.isEmpty()) cancelTasks.remove(r);
    }

    public Map<String, Map<String, Location>> getPendingShares(UUID uuid) {
        var m = pendingShares.get(uuid);
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
    }

    public int getShareTimeout() { return shareTimeout; }

    // ==================== TPA ====================

    public TpaRequest getTpaRequest(UUID targetUUID) { return tpaRequests.get(targetUUID); }

    public void addTpaRequest(UUID targetUUID, TpaRequest req) {
        tpaRequests.put(targetUUID, req);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this,
                () -> autoCancelTpa(targetUUID), tpaTimeout * 20L);
        tpaCancelTasks.put(targetUUID, task);
    }

    public void removeTpaRequest(UUID targetUUID) {
        tpaRequests.remove(targetUUID);
        BukkitTask t = tpaCancelTasks.remove(targetUUID);
        if (t != null) t.cancel();
    }

    private void autoCancelTpa(UUID targetUUID) {
        TpaRequest req = tpaRequests.remove(targetUUID);
        tpaCancelTasks.remove(targetUUID);
        if (req == null) return;
        if (req.sender().isOnline()) req.sender().sendMessage("§e⏰ TPA请求已过期。");
        if (req.target().isOnline()) req.target().sendMessage("§e⏰ 来自 §6" + req.sender().getName() + " §e的TPA请求已过期。");
    }

    public int getTpaTimeout() { return tpaTimeout; }

    // ==================== Warp ====================

    public void addWarp(String name, Location loc) {
        warps.put(name.toLowerCase(), loc.clone());
        if (database.isEnabled()) saveWarpToDB(name, loc);
        saveWarpsToFile();
    }

    public Location getWarp(String name) { return warps.get(name.toLowerCase()); }

    public boolean deleteWarp(String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            if (database.isEnabled()) deleteWarpFromDB(name);
            saveWarpsToFile();
            return true;
        }
        return false;
    }

    public Map<String, Location> getWarps() { return Collections.unmodifiableMap(warps); }

    // ==================== Spawn ====================

    public Location getSpawn() { return spawnLocation; }

    public void setSpawn(Location loc) {
        spawnLocation = loc.clone();
        if (database.isEnabled()) saveSpawnToDB();
        saveSpawnToFile();
    }

    private void loadSpawn() {
        if (database.isEnabled()) loadSpawnFromDB();
        if (spawnLocation == null) {
            Location loc = getConfig().getLocation("spawn");
            if (loc != null) spawnLocation = loc;
        }
        if (spawnLocation == null)
            spawnLocation = getServer().getWorlds().get(0).getSpawnLocation();
    }

    private void saveSpawnToFile() {
        if (spawnLocation != null) { getConfig().set("spawn", spawnLocation); saveConfig(); }
    }

    // ==================== 冷却 ====================

    public int getHomeCooldown() { return homeCooldown; }
    public int getHomeWarmup() { return homeWarmup; }

    public int getRemainingCooldown(UUID uuid) {
        Long last = lastHomeTime.get(uuid);
        if (last == null) return 0;
        long r = homeCooldown - (System.currentTimeMillis() - last) / 1000;
        return r > 0 ? (int) r : 0;
    }

    public void recordHomeUse(UUID uuid) { lastHomeTime.put(uuid, System.currentTimeMillis()); }

    public int getRtpCooldown() { return rtpCooldown; }
    public int getRtpMinRadius() { return rtpMinRadius; }
    public int getRtpMaxRadius() { return rtpMaxRadius; }

    public int getRtpRemainingCooldown(UUID uuid) {
        Long last = lastRtpTime.get(uuid);
        if (last == null) return 0;
        long r = rtpCooldown - (System.currentTimeMillis() - last) / 1000;
        return r > 0 ? (int) r : 0;
    }

    public void recordRtpUse(UUID uuid) { lastRtpTime.put(uuid, System.currentTimeMillis()); }

    public int getBackCooldown() { return backCooldown; }

    public int getBackRemainingCooldown(UUID uuid) {
        Long last = lastBackTime.get(uuid);
        if (last == null) return 0;
        long r = backCooldown - (System.currentTimeMillis() - last) / 1000;
        return r > 0 ? (int) r : 0;
    }

    public void recordBackUse(UUID uuid) { lastBackTime.put(uuid, System.currentTimeMillis()); }

    public Map<UUID, BukkitTask> getWarmupTasks() { return warmupTasks; }

    // ==================== /back ====================

    public Location getLastTeleportLocation(UUID uuid) { return lastTeleportLocation.get(uuid); }
    public Location getDeathLocation(UUID uuid) { return deathLocation.get(uuid); }

    public void setBackInProgress(UUID uuid, boolean b) {
        if (b) backInProgress.add(uuid); else backInProgress.remove(uuid);
    }

    // ==================== 消息颜色 ====================

    public ChatColor getMsgPrimary() { return msgPrimary; }
    public ChatColor getMsgSuccess() { return msgSuccess; }
    public ChatColor getMsgError() { return msgError; }
    public ChatColor getMsgInfo() { return msgInfo; }

    // ==================== 事件 ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (backInProgress.contains(player.getUniqueId())) return;
        if (event.getFrom().getWorld() == null) return;
        // ★ 如果目标是 SDB 战斗场地，不记录
        if (isSikadiBattleArena(event.getTo())) return;
        lastTeleportLocation.put(player.getUniqueId(), event.getFrom().clone());
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Location deathLoc = event.getEntity().getLocation();
        // ★ 如果在 SDB 战斗场地内死亡，不记录（避免 /back death 回到场地）
        if (isSikadiBattleArena(deathLoc)) return;
        deathLocation.put(event.getEntity().getUniqueId(), deathLoc.clone());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!welcomeMessage) return;
        Player player = event.getPlayer();
        int homeCount = countOwnHomes(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage(msgPrimary + "★ " + msgSuccess + "欢迎, " + msgPrimary + player.getName() + msgSuccess + "!");
        if (homeCount == 0)
            player.sendMessage(msgInfo + "  使用 " + msgPrimary + "/szcs <名称>" + msgInfo + " 设置传送点");
        else
            player.sendMessage(msgInfo + "  你有 " + msgPrimary + homeCount + msgInfo + " 个传送点 | " + msgPrimary + "/cs" + msgInfo + " 查看");
        player.sendMessage("");
    }

    // ==================== 工具 ====================

    @SuppressWarnings("deprecation")
    public UUID getUUIDByName(String name) {
        for (Player online : Bukkit.getOnlinePlayers())
            if (online.getName().equalsIgnoreCase(name)) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && (offline.hasPlayedBefore() || offline.isOnline()))
            return offline.getUniqueId();
        return null;
    }

    public Map<String, Location> getHomesByName(String name) {
        UUID uuid = getUUIDByName(name);
        return uuid == null ? null : homesMap.get(uuid);
    }

    public DatabaseManager getDatabase() { return database; }

    // ==================== 数据持久化 (YAML) ====================

    private void loadAll() {
        if (database.isEnabled()) loadFromMySQL();
        loadHomesFromFile();
        loadSharedFromFile();
        loadPendingFromFile();
        loadWarpsFromFile();
        loadSpawn();
    }

    private void saveAll() {
        saveHomesToFile();
        saveSharedToFile();
        savePendingToFile();
        saveWarpsToFile();
        saveSpawnToFile();
    }

    private void loadHomesFromFile() {
        ConfigurationSection sec = getConfig().getConfigurationSection("homes");
        if (sec == null) return;
        for (String uuidStr : sec.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }
            ConfigurationSection ps = sec.getConfigurationSection(uuidStr);
            if (ps == null) continue;
            Map<String, Location> inner = new HashMap<>();
            for (String name : ps.getKeys(false)) {
                Location loc = ps.getLocation(name);
                if (loc != null) inner.put(name, loc);
            }
            homesMap.put(uuid, inner);
        }
    }

    void saveHomesToFile() {
        getConfig().set("homes", null);
        for (var e : homesMap.entrySet())
            for (var he : e.getValue().entrySet())
                getConfig().set("homes." + e.getKey() + "." + he.getKey(), he.getValue());
        saveConfig();
    }

    private void loadSharedFromFile() {
        ConfigurationSection sec = getConfig().getConfigurationSection("shared-homes");
        if (sec == null) return;
        for (String uuidStr : sec.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }
            ConfigurationSection ps = sec.getConfigurationSection(uuidStr);
            if (ps == null) continue;
            Map<String, Map<String, Location>> outer = new HashMap<>();
            for (String sharer : ps.getKeys(false)) {
                ConfigurationSection hs = ps.getConfigurationSection(sharer);
                if (hs == null) continue;
                Map<String, Location> inner = new HashMap<>();
                for (String name : hs.getKeys(false)) {
                    Location loc = hs.getLocation(name);
                    if (loc != null) inner.put(name, loc);
                }
                outer.put(sharer, inner);
            }
            sharedHomes.put(uuid, outer);
        }
    }

    void saveSharedToFile() {
        getConfig().set("shared-homes", null);
        for (var e : sharedHomes.entrySet())
            for (var se : e.getValue().entrySet())
                for (var he : se.getValue().entrySet())
                    getConfig().set("shared-homes." + e.getKey() + "." + se.getKey() + "." + he.getKey(), he.getValue());
        saveConfig();
    }

    private void loadPendingFromFile() {
        ConfigurationSection sec = getConfig().getConfigurationSection("pending-shares");
        if (sec == null) return;
        for (String uuidStr : sec.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }
            ConfigurationSection ps = sec.getConfigurationSection(uuidStr);
            if (ps == null) continue;
            Map<String, Map<String, Location>> outer = new HashMap<>();
            for (String sharer : ps.getKeys(false)) {
                ConfigurationSection hs = ps.getConfigurationSection(sharer);
                if (hs == null) continue;
                Map<String, Location> inner = new HashMap<>();
                for (String name : hs.getKeys(false)) {
                    Location loc = hs.getLocation(name);
                    if (loc != null) inner.put(name, loc);
                }
                outer.put(sharer, inner);
            }
            pendingShares.put(uuid, outer);
        }
    }

    void savePendingToFile() {
        getConfig().set("pending-shares", null);
        for (var e : pendingShares.entrySet())
            for (var se : e.getValue().entrySet())
                for (var he : se.getValue().entrySet())
                    getConfig().set("pending-shares." + e.getKey() + "." + se.getKey() + "." + he.getKey(), he.getValue());
        saveConfig();
    }

    private void loadWarpsFromFile() {
        ConfigurationSection sec = getConfig().getConfigurationSection("warps");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            Location loc = sec.getLocation(name);
            if (loc != null) warps.put(name.toLowerCase(), loc);
        }
    }

    private void saveWarpsToFile() {
        getConfig().set("warps", null);
        for (var e : warps.entrySet())
            getConfig().set("warps." + e.getKey(), e.getValue());
        saveConfig();
    }

    // ==================== MySQL ====================

    private void loadFromMySQL() {
        String p = database.getPrefix();
        try (Connection c = database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + p + "homes");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    Location loc = new Location(Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                    homesMap.computeIfAbsent(uuid, k -> new HashMap<>()).put(rs.getString("name"), loc);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + p + "shared_homes");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("receiver_uuid"));
                    Location loc = new Location(Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                    sharedHomes.computeIfAbsent(uuid, k -> new HashMap<>())
                            .computeIfAbsent(rs.getString("sharer_name"), k -> new HashMap<>())
                            .put(rs.getString("home_name"), loc);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + p + "warps");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Location loc = new Location(Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                    warps.put(rs.getString("name").toLowerCase(), loc);
                }
            }
        } catch (SQLException e) {
            getLogger().warning("MySQL 加载失败: " + e.getMessage());
        }
    }

    private void saveHomeToDB(UUID uuid, String name, Location loc) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "REPLACE INTO " + p + "homes (uuid,name,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX()); ps.setDouble(5, loc.getY()); ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw()); ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL saveHome: " + e.getMessage()); }
    }

    private void deleteHomeFromDB(UUID uuid, String name) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + p + "homes WHERE uuid=? AND name=?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, name); ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL deleteHome: " + e.getMessage()); }
    }

    private void saveSharedToDB(UUID uuid, String sharer, String name, Location loc) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "REPLACE INTO " + p + "shared_homes (receiver_uuid,sharer_name,home_name,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, sharer); ps.setString(3, name);
            ps.setString(4, loc.getWorld().getName());
            ps.setDouble(5, loc.getX()); ps.setDouble(6, loc.getY()); ps.setDouble(7, loc.getZ());
            ps.setFloat(8, loc.getYaw()); ps.setFloat(9, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL saveShared: " + e.getMessage()); }
    }

    private void deleteSharedFromDB(UUID uuid, String sharer, String name) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM " + p + "shared_homes WHERE receiver_uuid=? AND sharer_name=? AND home_name=?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, sharer); ps.setString(3, name); ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL deleteShared: " + e.getMessage()); }
    }

    private void saveWarpToDB(String name, Location loc) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "REPLACE INTO " + p + "warps (name,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, name.toLowerCase()); ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX()); ps.setDouble(4, loc.getY()); ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw()); ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL saveWarp: " + e.getMessage()); }
    }

    private void deleteWarpFromDB(String name) {
        String p = database.getPrefix();
        try (Connection c = database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + p + "warps WHERE name=?")) {
            ps.setString(1, name.toLowerCase()); ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning("MySQL deleteWarp: " + e.getMessage()); }
    }

    private void loadSpawnFromDB() {
        String p = database.getPrefix();
        try (Connection c = database.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS " + p + "spawn (world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT)");
            try (ResultSet rs = s.executeQuery("SELECT * FROM " + p + "spawn LIMIT 1")) {
                if (rs.next()) {
                    spawnLocation = new Location(Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                }
            }
        } catch (SQLException e) { getLogger().warning("加载spawn失败: " + e.getMessage()); }
    }

    private void saveSpawnToDB() {
        if (spawnLocation == null) return;
        String p = database.getPrefix();
        try (Connection c = database.getConnection(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM " + p + "spawn");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO " + p + "spawn (world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?)")) {
                ps.setString(1, spawnLocation.getWorld().getName());
                ps.setDouble(2, spawnLocation.getX()); ps.setDouble(3, spawnLocation.getY());
                ps.setDouble(4, spawnLocation.getZ());
                ps.setFloat(5, spawnLocation.getYaw()); ps.setFloat(6, spawnLocation.getPitch());
                ps.executeUpdate();
            }
        } catch (SQLException e) { getLogger().warning("保存spawn失败: " + e.getMessage()); }
    }
    // ==================== ★ SikadiBattle 联动 ====================

    /** 反射检测玩家是否在 SikadiBattle 对战中 */
    public boolean isInSikadiBattle(UUID uuid) {
        org.bukkit.plugin.Plugin sb = getServer().getPluginManager().getPlugin("SikadiBattle");
        if (sb == null || !sb.isEnabled()) return false;
        try {
            java.lang.reflect.Method getInstance = sb.getClass().getMethod("getInstance");
            Object inst = getInstance.invoke(null);
            java.lang.reflect.Method getBM = inst.getClass().getMethod("getBattleManager");
            Object bm = getBM.invoke(inst);
            java.lang.reflect.Method isInBattle = bm.getClass().getMethod("isInBattle", UUID.class);
            return (boolean) isInBattle.invoke(bm, uuid);
        } catch (Exception e) { return false; }
    }

    /** 反射检测坐标是否在 SikadiBattle 场地内 */
    public boolean isSikadiBattleArena(Location loc) {
        org.bukkit.plugin.Plugin sb = getServer().getPluginManager().getPlugin("SikadiBattle");
        if (sb == null || !sb.isEnabled()) return false;
        try {
            java.lang.reflect.Method getInstance = sb.getClass().getMethod("getInstance");
            Object inst = getInstance.invoke(null);
            java.lang.reflect.Method getAM = inst.getClass().getMethod("getArenaManager");
            Object am = getAM.invoke(inst);
            java.lang.reflect.Method getAll = am.getClass().getMethod("getAllArenas");
            @SuppressWarnings("unchecked")
            java.util.Collection<?> arenas = (java.util.Collection<?>) getAll.invoke(am);
            for (Object arena : arenas) {
                java.lang.reflect.Method isInside = arena.getClass().getMethod("isInside", Location.class);
                if ((boolean) isInside.invoke(arena, loc)) return true;
            }
        } catch (Exception e) { return false; }
        return false;
    }

    /** 战斗中阻止传送 */
    public boolean blockIfInBattle(Player player) {
        if (isInSikadiBattle(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "⚠ 你正在 PvP 对战中，无法使用传送！");
            player.sendMessage(ChatColor.GRAY + "请等待对战结束或使用 /sdb leave 退出");
            return true;
        }
        return false;
    }

    /** 场地内阻止设传送点 */
    public boolean blockIfInArena(Player player) {
        if (isSikadiBattleArena(player.getLocation())) {
            player.sendMessage(ChatColor.RED + "⚠ 你处于 PvP 战斗场地内，不能在此设传送点！");
            return true;
        }
        return false;
    }



}
