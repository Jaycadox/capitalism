package xyz.jayphen.capitalism.economy.injection;

import xyz.jayphen.capitalism.database.Database;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class EconomyInjector {
	public static final String SERVER = "11111111";
	
	public EconomyInjector() {
		if (!exists()) generate();
	}
	
	public void generate() {
		String sql = "CREATE TABLE `meta` (\n" + "\t`total_money` INT(32));";
		
		try {
			Statement stmt = Database.ctn.createStatement();
			stmt.execute(sql);
			stmt = Database.ctn.createStatement();
			sql  = "INSERT INTO meta VALUES(0);";
			stmt.execute(sql);
		} catch (SQLException ignored) {
		}
	}
	
	public long getMoney() {
		try {
			Statement stmt = null;
			stmt = Database.ctn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT total_money FROM meta;");
			rs.next();
			return rs.getInt("total_money");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private void setMoney(double v) throws SQLException {
		Statement stmt = Database.ctn.createStatement();
		stmt.execute("UPDATE meta\n" + "SET total_money = " + v + ";");
	}
	
	private void addMoney(int amt) {
		try {
			setMoney(getMoney() + amt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DatabasePlayer getInjector() {
		return DatabasePlayer.nonPlayer(SERVER);
	}
	
	public TransactionResult inject(UUID player, int amount) {
		DatabasePlayer injector = DatabasePlayer.nonPlayer(SERVER);
		if (injector.setMoneySafe((int) ( injector.getMoneySafe() + amount ))) {
			addMoney(amount);
		}
		return new Transaction(injector.getUuid(), player, amount).transact();
	}
	
	public boolean exists() {
		try {
			DatabaseMetaData dbm = Database.ctn.getMetaData();
			return dbm.getTables(null, null, "meta", null).next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
