package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;
import xyz.jayphen.capitalism.players.display.PlayerDisplay;

public class PlayerLeave implements Listener {
	@EventHandler
	public void onPlayerJoin (PlayerQuitEvent event) {
		event.setQuitMessage("");
		new MessageBuilder("Quit").appendComponent(PlayerDisplay.from(event.getPlayer())).broadcast();
	}
}
