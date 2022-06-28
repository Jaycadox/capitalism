package xyz.jayphen.capitalism.events.register;

import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.events.*;

public class EventRegister {
		public static void registerAll() {
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlayerJoin(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlayerLeave(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlaytimeRewards(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new Lottery(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new DeathTax(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new LandClaimMovement(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new LandClaimInteraction(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new InventoryHelperEvent(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new ChatInputEvent(), Capitalism.plugin);
				Capitalism.plugin.getServer().getPluginManager().registerEvents(new Chat(), Capitalism.plugin);
				PlaytimeRewards.register();
				ClaimVisualizer.register();
				Lottery.register();
				MessageQueue.register();
				
				
				new BukkitRunnable() {
						@Override
						public void run() {
								LandClaimInteraction.monitorSignLoop();
						}
				}.runTaskTimer(Capitalism.plugin, 25, 25);
		}
}
