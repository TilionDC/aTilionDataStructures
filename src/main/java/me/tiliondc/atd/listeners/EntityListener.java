package me.tiliondc.atd.listeners;

import me.tiliondc.atd.database.SQLiteDB;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
import java.util.UUID;

public class EntityListener implements Listener {

    JavaPlugin plugin;
    SQLiteDB database;
    long savetimeinticks;
    Collection<String> prefixes;
    Collection<String> specifics;
    boolean casesensitive;
    Class craftserver;
    MetadataStoreBase storeBase;
    Map<String, Map<Plugin, MetadataValue>> metadataMap;

    public static final String tableName = "ENTTY_3_1";

    public EntityListener(JavaPlugin plugin, SQLiteDB database, long savetimeinticks, Collection<String> prefixes, Collection<String> specifics, boolean casesensitive) {

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
            fieldStoreBase = craftserver.getDeclaredField("entityMetadata");
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
    public void onChunkLoad(ChunkLoadEvent e) {
        for(Entity en : e.getChunk().getEntities()) {
            loadSpecificEntity(en);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for(Entity en : e.getChunk().getEntities()) {
            saveSpecificEntity(en.getUniqueId());
        }
    }

    public void addEntityToMetadatastore(String pluginName, String entityname, String key, MetadataValue metadata) {

        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        Bukkit.getServer().getPlayer(entityname).setMetadata(key, metadata);

    }

    public void addAutoTimer(long savetimeinticks) {
        if(savetimeinticks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveAllEntities();
                    loadAllEntities();
                }
            }.runTaskTimerAsynchronously(plugin, 0, savetimeinticks);
        }
    }

    public void saveAllEntities() {
        for(String key : metadataMap.keySet()) {
            UUID entityid = UUID.fromString(key.split(":")[0]);
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                saveEntity(value.getName(), entityid, ki, obj.toString());
            }
        }
    }

    public void saveSpecificEntity(UUID entityId) {
        for(String key : metadataMap.keySet()) {
            UUID entity = UUID.fromString(key.split(":")[0]);
            if(!entity.equals(entityId)) return;
            String ki = key.split(":")[1];
            for(Plugin value: metadataMap.get(key).keySet()) {
                Object obj = metadataMap.get(key).get(value).value();
                saveEntity(value.getName(), entity, ki, obj.toString());
            }
        }
    }

    public void saveEntity(String pluginname, UUID entityid, String key, String value) {
        for (String a : prefixes) {
            if (casesensitive) {
                if (a.equals(key))
                    database.insertToTable(tableName, pluginname, entityid.toString(), key, value);
            } else {
                if (a.equalsIgnoreCase(key))
                    database.insertToTable(tableName, pluginname, entityid.toString(), key, value);
            }
        }
        for (String b : specifics) {
            if (casesensitive && b.equals(key))
                database.insertToTable(tableName, pluginname, entityid.toString(), key, value);
            else if (b.equalsIgnoreCase(key))
                database.insertToTable(tableName, pluginname, entityid.toString(), key, value);
        }
    }

    public void loadAllEntities() {
        String[][] rows = database.selectFromTable(tableName, null, null, null, null);
        for(String[] row : rows) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(row[0]);
            Entity entity = getEntityByUniqueId(UUID.fromString(row[1]));
            loadEntity(plugin, entity, row[2], row[3]);
        }
    }

    public void loadSpecificEntity(Entity entity) {
        String[][] rows = database.selectFromTable(tableName, null, entity.getUniqueId().toString(), null, null);
        for(String[] row : rows) {
            loadEntity(Bukkit.getServer().getPluginManager().getPlugin(row[0]), entity, row[2], row[3]);
        }
    }

    public void loadEntity(Plugin plugin, Entity entity, String key, String value) {
        if(plugin == null || entity == null || key == null || value == null) return;
        entity.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    public Entity getEntityByUniqueId(UUID uniqueId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uniqueId))
                    return entity;
            }
        }

        return null;
    }


}
