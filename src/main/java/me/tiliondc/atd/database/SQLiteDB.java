package me.tiliondc.atd.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDB {

    Connection con;
    Statement stmt;
    JavaPlugin plugin;

    public SQLiteDB(JavaPlugin plugin) {

        this.plugin = plugin;

        try {

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/atd.db");
            stmt = con.createStatement();
            plugin.getLogger().info("Connected successfully to me.tiliondc.atd.database");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

    }


    public synchronized boolean createTable(String tablename, int keys, int values) {
        if(keys < 1) keys = 1;

        String syntax = "CREATE TABLE IF NOT EXISTS " + tablename + " ( ";

        for(int i = 0; i < keys; i++) {
            syntax = syntax + "key" + i + " var_char(63) not null, ";
        }
        for(int i = 0; i < values; i++) {
            syntax = syntax + "value" + i + " var_char(255), ";
        }
        syntax = syntax + "PRIMARY KEY ( key0";

        for(int i = 1; i < keys; i++) {
            syntax = syntax + ",key" + i;
        }
        syntax = syntax + " ) );";

        try {
            return stmt.execute(syntax);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean insertToTable(String tablename, String... data) {

        int keys = getKeysCount(tablename);
        int columns = getColumnCount(tablename);
        if(data.length < keys) return false;

        String keyNames = "key0";
        String keyNamesAndValues = "key0 = " + data[0];
        String valNames = "";

        String syntax = "INSERT OR REPLACE INTO " + tablename + " (";

        for(int i = 1; i < columns; i++) {
            if(i < keys) {
                keyNames = keyNames + ", key" + i;
                if(data[i] == null) return false;
                keyNamesAndValues = keyNamesAndValues + " AND key" + i + " = '" + data[i] + "'";
            }
            else {
                valNames = valNames + ", value" + (i - keys);
            }
        }
        syntax = syntax + keyNames + valNames;
        syntax = syntax + " ) VALUES ( '" + data[0] + "'";
        for(int i = 1; i < data.length && i < columns; i++) {
            if(data[i] == null && i < keys) return false;
            if(data[i] == null) syntax = syntax + "( SELECT " + valNames.split(", ")[i] + " FROM " + tablename +
                    " WHERE " + keyNamesAndValues + " )";
            else syntax = syntax + ", '" + data[i] + "'";
        }
        syntax = syntax + " COLLATE nocase );";

        try {
            return stmt.execute(syntax);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public synchronized String[][] selectFromTable(String tablename, String... keysAndValues) {

        String syntax = "SELECT * FROM " + tablename;
        int keys = getKeysCount(tablename);
        int columns = getColumnCount(tablename);
        String vals = "";
        String cond;
        for(int i = 0; i < keysAndValues.length; i++) {
            if(keysAndValues[i] != null) {
                if(!vals.isEmpty()) {
                    vals = vals + " AND ";
                }
                if(i < keys) {
                    cond = "key" + i;
                } else {
                    cond = "value" + (i - keys);
                }
                vals = vals + cond;
                vals = vals + " = '" + keysAndValues[i] + "'";

            }
        }

        if(!vals.isEmpty()) {
            syntax = syntax + " WHERE " + vals + " COLLATE nocase;";
        }
        ResultSet rs;
        try {
            rs = stmt.executeQuery(syntax);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        List<String[]> results = new ArrayList<>();
        int i = 0;
        try {
            while(rs.next()) {
                String[] temp = {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)};
                results.add(temp);
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results.toArray(new String[0][]);
    }

    public synchronized int getColumnCount(String tablename) {
        String syntax = "SELECT * FROM (" + tablename + ");";

        int count = 0;
        try {
            ResultSet rs = stmt.executeQuery(syntax);
            ResultSetMetaData rsmd = rs.getMetaData();
            count = rsmd.getColumnCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public synchronized int getKeysCount(String tablename) {
        String syntax = "SELECT * FROM (" + tablename + ");";

        int count = 1;
        try {
            ResultSet rs = stmt.executeQuery(syntax);
            ResultSetMetaData rsmd = rs.getMetaData();
            if(rsmd.getColumnCount() < 2) return 0;
            for(int i = 1; i <= rsmd.getColumnCount(); i++) {
                if(!rsmd.getColumnName(i).equals("key" + (i - 1))) break;
                count = i;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return count;
    }

    


}
