package xyz.jayphen.capitalism.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.players.display.PlayerDisplay;

public class Chat implements Listener {
	@EventHandler(priority = EventPriority.LOW)
	public void onChat(AsyncPlayerChatEvent event) {
		if (ChatInputEvent.onChat(event)) return;
		if (Lottery.onChat(event)) return;
		if (PlaytimeRewards.onChat(event)) return;
		
		event.setCancelled(true);
		var cmp = PlayerDisplay.from(event.getPlayer()).append(Component.text(": ", TextColor.color(0xcccccc)))
				.append(Component.text(event.getMessage(), TextColor.color(0xededed)));
		Capitalism.ADVENTURE.all().sendMessage(cmp);
	}
	
}
