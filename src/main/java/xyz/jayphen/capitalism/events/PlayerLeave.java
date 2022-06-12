package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;

public class PlayerLeave implements Listener {
	@EventHandler
	public void onPlayerJoin (PlayerQuitEvent event) {
		event.setQuitMessage(new MessageBuilder("Quit").append(Token.TokenType.CAPTION, event.getPlayer().getName()).build());
	}
}
