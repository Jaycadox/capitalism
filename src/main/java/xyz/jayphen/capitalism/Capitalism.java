package xyz.jayphen.capitalism;

import com.sk89q.worldedit.WorldEdit;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.commands.handler.CommandRegister;
import xyz.jayphen.capitalism.economy.CapitalismEconomy;
import xyz.jayphen.capitalism.events.InventoryHelperEvent;
import xyz.jayphen.capitalism.events.register.EventRegister;
import xyz.jayphen.capitalism.hooks.EconomyHook;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class Capitalism extends JavaPlugin {

	public static Logger LOG = null;
	public static Plugin plugin = null;
	public static Database db = null;
	public static CapitalismEconomy eco = new CapitalismEconomy();
	public final EconomyHook HOOK = new EconomyHook(this);

	public static BukkitAudiences ADVENTURE = null;

	@Override
	public void onEnable () {
		ADVENTURE = BukkitAudiences.create(this);
		plugin = this;
		LOG = this.getLogger();
		CommandRegister.registerAllCommands(this);
		LOG.info("Registered commands.");
		if (HOOK.vault(eco)) {
			LOG.info("Vault has been hooked.");
		}
		db = new Database();
		LOG.info("Loaded database.");
		EventRegister.registerAll();
		LOG.info("Registered events.");

		if(!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
			LOG.warning("WorldEdit not found.");
		} else {
			WorldEdit.getInstance().getItemFactory();
		}
	}


	@Override
	public void onDisable () {
		try {
			Database.ctn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		InventoryHelperEvent.closeInventories();
		ADVENTURE.close();
	}
}
