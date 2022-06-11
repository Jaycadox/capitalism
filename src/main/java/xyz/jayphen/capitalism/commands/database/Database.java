package xyz.jayphen.capitalism.commands.database;

import xyz.jayphen.capitalism.Capitalism;

import java.io.File;
import java.sql.*;

public class Database {
    String dbPath = new File(Capitalism.plugin.getDataFolder() + "\\database.db").getAbsolutePath();
    public Database()
    {
        if(!exists())
            generate();
    }

    public void createNewTable(Connection cnt) {
        // SQL statement for creating a new table
        String sql = "CREATE TABLE `players` (\n" +
                "\t`uuid` TEXT(32),\n" +
                "\t`money` INT(32),\n" +
                "\tPRIMARY KEY (`uuid`)\n" +
                ");";

        try {
            Statement stmt = cnt.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public Connection connect()
    {
        String url = "jdbc:sqlite:" + dbPath;
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private void generate()
    {

        String url = "jdbc:sqlite:" + dbPath;
        try {
            System.out.println(dbPath);
            new File(Capitalism.plugin.getDataFolder() + "\\").mkdir();
            File f = new File(dbPath);
            f.createNewFile();

            Connection conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                Capitalism.LOG.info("[SQLITE] Driver name: " + meta.getDriverName());
                Capitalism.LOG.info("[SQLITE] Database generated.");
            }
            createNewTable(conn);


        } catch(Exception e) {
            Capitalism.LOG.warning("[SQLITE] Failed to generate database.");
            e.printStackTrace();

        }
    }
    private boolean exists()
    {
        return new File(Capitalism.plugin.getDataFolder() + "\\database.db").exists();
    }
}
