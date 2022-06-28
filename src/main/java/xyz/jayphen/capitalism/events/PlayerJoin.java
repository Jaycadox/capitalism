package xyz.jayphen.capitalism.events;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.jayphen.capitalism.commands.database.player.BanManager;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.players.display.PlayerDisplay;
import xyz.jayphen.capitalism.players.list.PlayerListManager;

public class PlayerJoin implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		DatabasePlayer.from(event.getPlayer());
		PlayerListManager.set(event.getPlayer());
		event.joinMessage(Component.empty());
		new MessageBuilder("Join").appendComponent(PlayerDisplay.from(event.getPlayer())).broadcast();
		Lottery.nag(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onConnect(AsyncPlayerPreLoginEvent event) {
		var dbp = DatabasePlayer.from(event.getUniqueId());
		if (dbp.getJsonPlayer().getBannedUntil() == -1) return;
		long timeLeft = dbp.getJsonPlayer().getBannedUntil() - System.currentTimeMillis();
		if (timeLeft < 0) {
			dbp.getJsonPlayer().getData().bannedUntil = (long) -1;
			dbp.getJsonPlayer().save();
			return;
		}
		event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
		Component comp = BanManager.getBanMessage(dbp, timeLeft);
		event.kickMessage(comp);
		
	}
	
	
}
