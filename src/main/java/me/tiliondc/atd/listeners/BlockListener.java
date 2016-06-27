package me.tiliondc.atd.listeners;

import me.tiliondc.atd.database.SQLiteDB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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

public class BlockListener implements Listener {

    JavaPlugin plugin;
    SQLiteDB database;
    long savetimeinticks;
    Collection<String> prefixes;
    Collection<String> specifics;
    boolean casesensitive;
    MetadataStoreBase<Block> storeBase;
    Map<World, Map<String, Map<Plugin, MetadataValue>>> metadataMaps;

    public static final String tableName = "BLOCK_4_1";

    public BlockListener(JavaPlugin plugin, SQLiteDB database, long savetimeinticks, Collection<String> prefixes, Collection<String> specifics, boolean casesensitive) {

        this.plugin = plugin;
        this.database = database;
        this.savetimeinticks = savetimeinticks;
        this.prefixes = prefixes;
        this.specifics = specifics;
        this.casesensitive = casesensitive;

        metadataMaps = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        database.createTable(tableName, 4, 1);
        for(World world : Bukkit.getServer().getWorlds()) {
            try {
                Field fieldStoreBase;
                fieldStoreBase = world.getClass().getDeclaredField("blockMetadata");
                fieldStoreBase.setAccessible(true);
                storeBase = ((MetadataStoreBase<Block>) fieldStoreBase.get(world));
                Field f = MetadataStoreBase.class.getDeclaredField("metadataMap");
                f.setAccessible(true);
                metadataMaps.put(world, (HashMap<String, Map<Plugin, MetadataValue>>) f.get(storeBase));

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        addAutoTimer(savetimeinticks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        World world = e.getWorld();
        if(metadataMaps.get(world) == null) return;
        for(String s : metadataMaps.get(world).keySet()) {
            long x = Long.parseLong(s.split(":")[0]);
            long y = Long.parseLong(s.split(":")[1]);
            long z = Long.parseLong(s.split(":")[2]);
            Location loc = new Location(world, x, y, z);

            if(loc.getChunk().equals(e.getChunk())) {

                saveSpecificBlock(world.getUID().toString(), x + ":" + y + ":" + z);

            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        saveAllBlocks();
    }

    public void addBlockToMetadatastore(String pluginName, String blockName, String key, MetadataValue metadata) {

        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        Bukkit.getServer().getWorld(blockName).setMetadata(key, metadata);

    }

    public void addAutoTimer(long savetimeinticks) {
        if(savetimeinticks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for(World world : Bukkit.getServer().getWorlds()) {
                        try {
                            Field fieldStoreBase;
                            fieldStoreBase = world.getClass().getDeclaredField("blockMetadata");
                            fieldStoreBase.setAccessible(true);
                            storeBase = ((MetadataStoreBase<Block>) fieldStoreBase.get(world));
                            Field f = MetadataStoreBase.class.getDeclaredField("metadataMap");
                            f.setAccessible(true);
                            metadataMaps.put(world, (HashMap<String, Map<Plugin, MetadataValue>>) f.get(storeBase));

                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    saveAllBlocks();
                    loadAllBlocks();
                }
            }.runTaskTimerAsynchronously(plugin, 0, savetimeinticks);
        }
    }

    public void saveAllBlocks() {
        if(metadataMaps == null) return;
        for(World world : Bukkit.getServer().getWorlds()) {
            if(metadataMaps.get(world) == null) continue;
            for (String key : metadataMaps.get(world).keySet()) {
                String cords = key.split(":")[0] + ":" + key.split(":")[1] + ":" + key.split(":")[2];
                String ki = key.split(":")[3];
                for (Plugin value : metadataMaps.get(world).get(key).keySet()) {
                    Object obj = metadataMaps.get(world).get(key).get(value).value();
                    saveWorld(value.getName(), world.getUID().toString(), cords, ki, obj.toString());
                }
            }
        }
    }

    public void saveSpecificBlock(String worldid, String blocklocation) {
        if(metadataMaps == null) return;
        World world = Bukkit.getServer().getWorld(UUID.fromString(worldid));
            for (String key : metadataMaps.get(world).keySet()) {
                String cords = key.split(":")[0] + ":" + key.split(":")[1] + ":" + key.split(":")[2];
                if (!cords.equalsIgnoreCase(blocklocation)) return;
                String ki = key.split(":")[3];
                for (Plugin value : metadataMaps.get(world).get(key).keySet()) {
                    Object obj = metadataMaps.get(world).get(key).get(value).value();
                    saveWorld(value.getName(), world.getUID().toString(), cords, ki, obj.toString());
                }
            }

    }

    public void saveWorld(String pluginname, String worldid, String cords, String key, String value) {
        for (String a : prefixes) {
            if (casesensitive) {
                if (a.equals(key))
                    database.insertToTable(tableName, pluginname, worldid, cords, key, value);
            } else {
                if (a.equalsIgnoreCase(key))
                    database.insertToTable(tableName, pluginname, worldid, cords, key, value);
            }
        }
        for (String b : specifics) {
            if (casesensitive && b.equals(key))
                database.insertToTable(tableName, pluginname, worldid, cords, key, value);
            else if (b.equalsIgnoreCase(key))
                database.insertToTable(tableName, pluginname, worldid, cords, key, value);
        }
    }

    public void loadAllBlocks() {
        String[][] rows = database.selectFromTable(tableName, null, null, null, null);
        for(String[] row : rows) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(row[0]);
            World world = Bukkit.getServer().getWorld(row[1]);
            loadWorld(plugin, world, row[2], row[3]);
        }
    }

    public void loadSpecificBlock(World world) {
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
