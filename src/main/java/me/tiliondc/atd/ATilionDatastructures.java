package me.tiliondc.atd;

import me.tiliondc.atd.database.SQLiteDB;
import me.tiliondc.atd.listeners.BlockListener;
import me.tiliondc.atd.listeners.EntityListener;
import me.tiliondc.atd.listeners.PlayerListener;
import me.tiliondc.atd.listeners.WorldListener;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;

public class ATilionDatastructures extends JavaPlugin {

    SQLiteDB db;

    @Override
    public void onEnable() {
        super.onEnable();

        createConfig();

        db = new SQLiteDB(this);
        if (getConfig().getBoolean("SQL.Enabled")) {
            long inteval = getConfig().getLong("SQL.Saveinterval");
            Collection<String> prefixes = getConfig().getStringList("SQL.Prefix");
            Collection<String> specific = getConfig().getStringList("SQL.Specific");
            boolean caseSensitive = getConfig().getBoolean("SQL.Casesensitive");
            new PlayerListener(this, db, inteval, prefixes, specific, caseSensitive);
            new EntityListener(this, db, inteval, prefixes, specific, caseSensitive);
            new WorldListener(this, db, inteval, prefixes, specific, caseSensitive);
            new BlockListener(this, db, inteval, prefixes, specific, caseSensitive);
        }
    }


    @Override
    public void onDisable() {
        super.onDisable();
    }

    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating!");
                saveDefaultConfig();
            } else {
                @SuppressWarnings("deprecation")
                String version = YamlConfiguration.loadConfiguration(getResource("config.yml")).getString("Version");
                if (version == null) version = "ERROR";
                if (!getConfig().getString("Version").equals(version) || getConfig().getString("Version") == null) {
                    getConfig().save(getDataFolder() + "/old-config-" + getConfig().getString("Version") + ".yml");

                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    saveDefaultConfig();
                    getLogger().info("Old config version, recreating config.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }
}
