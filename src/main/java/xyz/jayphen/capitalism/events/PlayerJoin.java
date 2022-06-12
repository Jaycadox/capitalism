package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;

public class PlayerJoin implements Listener {
	@EventHandler
	public void onPlayerJoin (PlayerJoinEvent event) {
		DatabasePlayer.from(event.getPlayer());
		event.setJoinMessage(new MessageBuilder("Join").append(Token.TokenType.CAPTION, event.getPlayer().getName()).build());
	}
}
