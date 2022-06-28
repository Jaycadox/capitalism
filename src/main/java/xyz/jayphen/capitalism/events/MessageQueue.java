package xyz.jayphen.capitalism.events;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;

public class MessageQueue {
	public static void register() {
		new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(Capitalism.plugin, 0, 20);
	}
	
	private static void tick() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			var dbp = DatabasePlayer.from(p);
			for (Component cmp : dbp.getJsonPlayer().getMessageQueue()) {
				Capitalism.ADVENTURE.player(p).sendMessage(cmp);
			}
			dbp.getJsonPlayer().getData().messageQueue.clear();
			dbp.getJsonPlayer().save();
		}
	}
}
