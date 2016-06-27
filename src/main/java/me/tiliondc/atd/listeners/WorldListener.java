package me.tiliondc.atd.listeners;

import me.tiliondc.atd.database.SQLiteDB;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorldListener implements Listener {

    JavaPlugin plugin;
    SQLiteDB database;
    long savetimeinticks;
    Collection<String> prefixes;
    Collection<String> specifics;
    boolean casesensitive;
    Class craftserver;
    MetadataStoreBase storeBase;
    Map<String, Map<Plugin, MetadataValue>> metadataMap;

    public static final String tableName = "WORLD_3_1";

    public WorldListener(JavaPlugin plugin, SQLiteDB database, long savetimeinticks, Collection<String> prefixes, Collection<String> specifics, boolean casesensitive) {

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
            fieldStoreBase = craftserver.getDeclaredField("worldMetadata");
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
    public void onWorldLoad(WorldLoadEvent e) {
        loadSpecificWorld(e.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        saveSpecificWorld(e.getWorld());
    }

    public void addWorldToMetadatastore(String pluginName, String worldName, String key, MetadataValue metadata) {

        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        Bukkit.getServer().getWorld(worldName).setMetadata(key, metadata);

    }

    public void addAutoTimer(long savetimeinticks) {
        if(savetimeinticks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveAllWorlds();
                    loadAllWorlds();
                }
            }.runTaskTimerAsynchronously(plugin, 0, savetimeinticks);
        }
    }

    public void saveAllWorlds() {
        for(String key : metadataMap.keySet()) {
            String player = key.split(":")[0];
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                saveWorld(value.getName(), player, ki, obj.toString());
            }
        }
    }

    public void saveSpecificWorld(World world) {
        for(String key : metadataMap.keySet()) {
            String worldid = key.split(":")[0];
            if(!world.getUID().toString().equalsIgnoreCase(worldid)) return;
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                saveWorld(value.getName(), worldid, ki, obj.toString());
            }
        }
    }

    public void saveWorld(String pluginname, String worldname, String key, String value) {
        for (String a : prefixes) {
            if (casesensitive) {
                if (a.equals(key))
                    database.insertToTable(tableName, pluginname, worldname, key, value);
            } else {
                if (a.equalsIgnoreCase(key))
                    database.insertToTable(tableName, pluginname, worldname, key, value);
            }
        }
        for (String b : specifics) {
            if (casesensitive && b.equals(key))
                database.insertToTable(tableName, pluginname, worldname, key, value);
            else if (b.equalsIgnoreCase(key))
                database.insertToTable(tableName, pluginname, worldname, key, value);
        }
    }

    public void loadAllWorlds() {
        String[][] rows = database.selectFromTable(tableName, null, null, null);
        for(String[] row : rows) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(row[0]);
            World world = Bukkit.getServer().getWorld(row[1]);
            loadWorld(plugin, world, row[2], row[3]);
        }
    }

    public void loadSpecificWorld(World world) {
        String[][] rows = database.selectFromTable(tableName, null, world.getUID().toString(), null, null);
        for(String[] row : rows) {
            loadWorld(Bukkit.getServer().getPluginManager().getPlugin(row[0]), world, row[2], row[3]);
        }
    }

    public void loadWorld(Plugin plugin, World world, String key, String value) {
        if(plugin == null || world == null || key == null || value == null) return;
        world.setMetadata(key, new FixedMetadataValue(plugin, value));
    }




}
