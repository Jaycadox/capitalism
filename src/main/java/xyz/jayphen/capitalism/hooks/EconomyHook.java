package xyz.jayphen.capitalism.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import xyz.jayphen.capitalism.economy.CapitalismEconomy;

public class EconomyHook {
	Plugin p = null;
	
	public EconomyHook(Plugin pl) {
		p = pl;
	}
	
	
	public boolean vault(CapitalismEconomy eco) {
		if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		Bukkit.getServicesManager().register(Economy.class, eco, p, ServicePriority.Normal);
		return true;
	}
}
