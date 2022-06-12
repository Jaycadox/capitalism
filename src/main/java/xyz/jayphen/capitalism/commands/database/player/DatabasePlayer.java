package xyz.jayphen.capitalism.commands.database.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class DatabasePlayer {
	private static HashMap<UUID, DatabasePlayer> cache = new HashMap<>();
	Connection connection = null;
	UUID uuid = null;

	public DatabasePlayer (Connection c, UUID uuid) {
		this.connection = c;
		this.uuid = uuid;
		if (!exists()) generate(1000000);
	}

	public static DatabasePlayer from (OfflinePlayer p) {
		return from(p.getUniqueId());
	}

	public static DatabasePlayer from (UUID p) {
		if (cache.containsKey(p)) {
			return cache.get(p);
		}
		DatabasePlayer databasePlayer = new DatabasePlayer(Capitalism.db.connect(), p);
		cache.put(p, databasePlayer);
		return databasePlayer;
	}

	public static DatabasePlayer nonPlayer (String s) {
		return DatabasePlayer.from(UUID.fromString(s + "-0000-0000-0000-000000000000"));
	}

	public static long sumMoney () {
		try {
			Statement stmt = null;
			stmt = Database.ctn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT money FROM players");
			long sum = 0;
			while (rs.next()) {
				sum += rs.getInt("money");
			}
			return sum;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	public static ArrayList<UUID> allLotteryEnteredPeople() {
		ArrayList<UUID> list = new ArrayList<>();
		try {
			Statement stmt = null;
			stmt = Database.ctn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM players WHERE joined_lottery = 1");
			while (rs.next()) {
				list.add(UUID.fromString(rs.getString("uuid")));
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	public UUID getUuid () {
		return uuid;
	}

	public double getMoney () throws SQLException {
		Statement stmt = null;
		stmt = Database.ctn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT money FROM players WHERE uuid = '" + uuid.toString() + "'");
		rs.next();
		return (double) rs.getInt("money");
	}

	public void setMoney (double v) throws SQLException {
		Statement stmt = Database.ctn.createStatement();
		stmt.execute("UPDATE players\n" + "SET money = " + v + " WHERE uuid = '" + uuid.toString() + "';");
	}

	public double getMoneySafe () {
		try {
			return getMoney();
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean setMoneySafe (int v) {
		try {
			setMoney(v);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	public boolean getJoinedLottery() {
		try {
			Statement stmt = null;
			stmt = Database.ctn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT joined_lottery FROM players WHERE uuid = '" + uuid.toString() + "'");
			rs.next();
			return rs.getInt("joined_lottery") == 1;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public void setJoinedLottery(boolean val) {
		try {
			Statement stmt = null;
			stmt = Database.ctn.createStatement();
			stmt.execute("UPDATE players\n" + "SET joined_lottery = " + (val ? 1 : 0) + " WHERE uuid = '" + uuid.toString() + "';");
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	public boolean exists () {
		Statement stmt = null;
		try {
			stmt = Database.ctn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT uuid FROM players WHERE uuid = '" + uuid.toString() + "'");
			int count = 0;
			while (rs.next()) {
				count++;
			}
			return count != 0;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void generate (double startingCash) {
		String sql = "INSERT INTO players\n" + "VALUES ('" + uuid.toString() + "', " + 0 + ", 0); ";
		try {
			Statement stmt = Database.ctn.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
		}
		TransactionResult trs = Database.injector.inject(uuid, (int) startingCash);
		if (trs.getType() == TransactionResult.TransactionResultType.SUCCESS) {
			if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
				Player p = Bukkit.getOfflinePlayer(uuid).getPlayer();
				p.sendMessage(new MessageBuilder("Welcome").append(Token.TokenType.CAPTION, "Hello,").append(Token.TokenType.VARIABLE, p.getName() + ".").append(Token.TokenType.CAPTION, "Welcome to Capitalism. The goal is to reach $1,000,000,000. You have been awarded").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(startingCash)).append(Token.TokenType.CAPTION, "as a starting bonus").build());
			}
		}
	}
}
