package xyz.jayphen.capitalism.database;

import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;

import java.io.File;
import java.sql.*;

public class Database {
	public static String          dbPath   = new File(Capitalism.plugin.getDataFolder() + "\\database.db").getAbsolutePath();
	public static Connection      ctn;
	public static EconomyInjector injector = null;
	
	public Database() {
		if (!exists()) generate();
		if (ctn == null) {
			try {
				ctn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		injector = new EconomyInjector();
	}
	
	public void createNewTable(Connection cnt) {
		// SQL statement for creating a new table
		String sql = "CREATE TABLE `players` (\n" + "\t`uuid` TEXT(32),\n" + "\t`money` INT(32),\n" + "\t`joined_lottery` INT(32),\n" +
		             "\t`json` TEXT(32),\n" + "\tPRIMARY KEY (`uuid`));";
		
		try {
			Statement stmt = cnt.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Connection connect() {
		return ctn;
	}
	
	private void generate() {
		
		String url = "jdbc:sqlite:" + dbPath;
		try {
			new File(Capitalism.plugin.getDataFolder() + "\\").mkdir();
			File f = new File(dbPath);
			f.createNewFile();
			ctn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			Connection conn = connect();
			if (conn != null) {
				DatabaseMetaData meta = conn.getMetaData();
				Capitalism.LOG.info("[SQLITE] Driver name: " + meta.getDriverName());
				Capitalism.LOG.info("[SQLITE] Database generated.");
			}
			createNewTable(conn);
			
			
		} catch (Exception e) {
			Capitalism.LOG.warning("[SQLITE] Failed to generate database.");
			e.printStackTrace();
			
		}
	}
	
	private boolean exists() {
		return new File(Capitalism.plugin.getDataFolder() + "\\database.db").exists();
	}
}
