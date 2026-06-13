package com.example.playerhomes;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final HomeFromSikadi233 plugin;
    private HikariDataSource dataSource;
    private boolean enabled;
    private String prefix;

    public DatabaseManager(HomeFromSikadi233 plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("mysql.enabled", false);
        this.prefix = plugin.getConfig().getString("mysql.table-prefix", "hfs_");
        if (enabled) connect();
    }

    private void connect() {
        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String db = plugin.getConfig().getString("mysql.database", "minecraft");
        String user = plugin.getConfig().getString("mysql.username", "root");
        String pass = plugin.getConfig().getString("mysql.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("MySQL 连接成功！");
        } catch (Exception e) {
            plugin.getLogger().warning("MySQL 连接失败，降级为 YAML 存储: " + e.getMessage());
            enabled = false;
        }
    }

    private void createTables() {
        try (Connection c = dataSource.getConnection()) {
            // 自己的传送点
            c.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "homes (" +
                            "uuid VARCHAR(36), name VARCHAR(64), world VARCHAR(64), " +
                            "x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, " +
                            "PRIMARY KEY(uuid, name))"
            );
            // 已接受的分享
            c.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "shared_homes (" +
                            "receiver_uuid VARCHAR(36), sharer_name VARCHAR(36), " +
                            "home_name VARCHAR(64), world VARCHAR(64), " +
                            "x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, " +
                            "PRIMARY KEY(receiver_uuid, sharer_name, home_name))"
            );
            // 地标
            c.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "warps (" +
                            "name VARCHAR(64) PRIMARY KEY, world VARCHAR(64), " +
                            "x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT)"
            );
        } catch (SQLException e) {
            plugin.getLogger().warning("创建表失败: " + e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled && dataSource != null && !dataSource.isClosed(); }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getPrefix() { return prefix; }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
