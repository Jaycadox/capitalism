package xyz.jayphen.capitalism.commands.database.player;

import org.bukkit.OfflinePlayer;
import xyz.jayphen.capitalism.Capitalism;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;

public class DatabasePlayer {
    Connection connection = null;
    UUID uuid = null;

    private static HashMap<UUID, DatabasePlayer> cache = new HashMap<>();
    public static DatabasePlayer from(OfflinePlayer p) {
        if(cache.containsKey(p.getUniqueId()))
        {
            return cache.get(p.getUniqueId());
        }
        DatabasePlayer dbp = new DatabasePlayer(Capitalism.db.connect(), p.getUniqueId());
        cache.put(p.getUniqueId(), dbp);
        return dbp;
    }
    public DatabasePlayer(Connection c, UUID uuid) {
        this.connection = c;
        this.uuid = uuid;
        if(!exists())
            generate(1000000);
    }
    public void setMoney(double v) {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("UPDATE players\n" +
                    "SET money = " + v + " WHERE uuid = " + uuid.toString() + ";");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public double getMoney() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT money FROM players WHERE uuid = '" + uuid.toString() + "'");
            rs.next();
            return (double)rs.getInt("money");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists()
    {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT uuid FROM players WHERE uuid = '" + uuid.toString() + "'");
            int count = 0;
            while(rs.next()) {
                count++;
            }
            System.out.println(count);
            return count != 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(double startingCash)
    {
        String sql = "INSERT INTO players\n" +
                "VALUES ('" + uuid.toString() + "', " + startingCash + "); ";
        try {
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
