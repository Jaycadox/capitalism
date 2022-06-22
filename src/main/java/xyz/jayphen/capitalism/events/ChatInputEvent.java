package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.helpers.ChatInput;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.stream.Collectors;


public class ChatInputEvent implements Listener
{
	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		for(ChatInput.Query q : ChatInput.OPEN_QUERIES) {
			if(q.player() == event.getPlayer().getUniqueId()) {
				event.setCancelled(true);
				ChatInput.OPEN_QUERIES.removeIf(x -> x.player() == event.getPlayer().getUniqueId());
				event.getPlayer().sendMessage(new MessageBuilder("Query").appendCaption("Running query for:")
						                              .appendVariable(q.query()).appendCaption("with value:").appendVariable(event.getMessage()).build());
				new BukkitRunnable() {
					@Override
					public void run () {
						q.runnable().run(event.getMessage());
					}
				}.runTask(Capitalism.plugin);
				return;
			}
		}
	}
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		ChatInput.OPEN_QUERIES.removeIf(x -> x.player() == event.getPlayer().getUniqueId());
	}
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		new BukkitRunnable() {
			@Override
			public void run () {
				try {
					for(ChatInput.Query q : ChatInput.getOpenQueries()) {
						if (q.player() == event.getPlayer().getUniqueId()) {
							ChatInput.getOpenQueries().removeIf(x -> x.player() == event.getPlayer().getUniqueId());
							event.getPlayer().sendMessage(new MessageBuilder("Query").appendCaption("Canceled query with name:")
									                              .appendVariable(q.query()).build());
						}
					}
				} catch(Exception ignored) {}
			}
		}.runTask(Capitalism.plugin);
	}
}