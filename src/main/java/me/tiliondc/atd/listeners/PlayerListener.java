package me.tiliondc.atd.listeners;

import me.tiliondc.atd.database.SQLiteDB;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;

public class PlayerListener implements Listener {

    JavaPlugin plugin;
    SQLiteDB database;
    long savetimeinticks;
    Collection<String> prefixes;
    Collection<String> specifics;
    boolean casesensitive;
    Class craftserver;
    MetadataStoreBase storeBase;
    Map<String, Map<Plugin, MetadataValue>> metadataMap;

    public static final String tableName = "PLAYERS_3_1";

    public PlayerListener(JavaPlugin plugin, SQLiteDB database, long savetimeinticks, Collection<String> prefixes, Collection<String> specifics, boolean casesensitive) {

        this.plugin = plugin;
        this.database = database;
        this.savetimeinticks = savetimeinticks;
        this.prefixes = prefixes;
        this.specifics = specifics;
        this.casesensitive = casesensitive;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        database.createTable(tableName, 3, 1);
        craftserver = Bukkit.getServer().getClass();
        try {
            Field fieldStoreBase;
            fieldStoreBase = craftserver.getDeclaredField("playerMetadata");
            fieldStoreBase.setAccessible(true);
            storeBase = ((MetadataStoreBase) fieldStoreBase.get(Bukkit.getServer()));
            Field f = MetadataStoreBase.class.getDeclaredField("metadataMap");
            f.setAccessible(true);
            metadataMap = (HashMap<String, Map<Plugin, MetadataValue>>) f.get(storeBase);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        addAutoTimer(savetimeinticks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerLoginEvent e) {
        loadSpecificPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent e) {
        saveSpecificPlayer(e.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKicked(PlayerKickEvent e) {
        saveSpecificPlayer(e.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent e) {
        loadAllPlayers();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        saveAllPlayers();
    }

    public void addPlayerToMetadatastore(String pluginName, String playername, String key, MetadataValue metadata) {

        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        Bukkit.getServer().getPlayer(playername).setMetadata(key, metadata);

    }

    public void addAutoTimer(long savetimeinticks) {
        if(savetimeinticks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveAllPlayers();
                    loadAllPlayers();
                }
            }.runTaskTimerAsynchronously(plugin, 0, savetimeinticks);
        }
    }

    public void saveAllPlayers() {
        for(String key : metadataMap.keySet()) {
            String player = key.split(":")[0];
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                savePlayer(value.getName(), player, ki, obj.toString());
            }
        }
    }

    public void saveSpecificPlayer(String playername) {
        for(String key : metadataMap.keySet()) {
            String player = key.split(":")[0];
            if(!player.equalsIgnoreCase(playername)) return;
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                savePlayer(value.getName(), player, ki, obj.toString());
            }
        }
    }

    public void savePlayer(String pluginname, String playername, String key, String value) {
        for (String a : prefixes) {
            if (casesensitive) {
                if (a.equals(key))
                    database.insertToTable(tableName, pluginname, playername, key, value);
            } else {
                if (a.equalsIgnoreCase(key))
                    database.insertToTable(tableName, pluginname, playername, key, value);
            }
        }
        for (String b : specifics) {
            if (casesensitive && b.equals(key))
                database.insertToTable(tableName, pluginname, playername, key, value);
            else if (b.equalsIgnoreCase(key))
                database.insertToTable(tableName, pluginname, playername, key, value);
        }
    }

    public void loadAllPlayers() {
        String[][] rows = database.selectFromTable(tableName, null, null, null, null);
        for(String[] row : rows) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(row[0]);
            Player player = Bukkit.getServer().getPlayer(row[1]);
            loadPlayer(plugin, player, row[2], row[3]);
        }
    }

    public void loadSpecificPlayer(Player player) {
        String[][] rows = database.selectFromTable(tableName, null, player.getName(), null, null);
        for(String[] row : rows) {
            loadPlayer(Bukkit.getServer().getPluginManager().getPlugin(row[0]), player, row[2], row[3]);
        }
    }

    public void loadPlayer(Plugin plugin, Player player, String key, String value) {
        if(plugin == null || player == null || key == null || value == null) return;
        player.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

}
