package xyz.jayphen.capitalism.events;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.helpers.TimeHelper;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;
import xyz.jayphen.capitalism.players.display.PlayerDisplay;
import xyz.jayphen.capitalism.players.list.PlayerListManager;

public class PlayerJoin implements Listener {
	@EventHandler
	public void onPlayerJoin (PlayerJoinEvent event) {
		DatabasePlayer.from(event.getPlayer());
		PlayerListManager.set(event.getPlayer());
		event.setJoinMessage("");
		new MessageBuilder("Join").appendComponent(PlayerDisplay.from(event.getPlayer())).broadcast();
		Lottery.nag(event.getPlayer());
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onConnect(AsyncPlayerPreLoginEvent event) {
		var dbp = DatabasePlayer.from(event.getUniqueId());
		if(dbp.getJsonPlayer().getBannedUntil() == -1) return;
		long timeLeft = dbp.getJsonPlayer().getBannedUntil() - System.currentTimeMillis();
		if(timeLeft < 0) {
			dbp.getJsonPlayer().getData().bannedUntil = (long) -1;
			dbp.getJsonPlayer().save();
			return;
		}
		event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
		Component comp = getBanMessage(dbp, timeLeft);
		event.kickMessage(comp);

	}

	@NotNull
	public static Component getBanMessage (DatabasePlayer dbp, long timeLeft) {
		String formattedTimeLeft = TimeHelper.timeToString(timeLeft);
		var comp = MiniMessage.miniMessage().deserialize(
				"[CAPITALISM SMP]<newline><color:red><bold>INFRACTION NOTICE</bold></color><newline><newline><yellow>\"" + dbp.getJsonPlayer().getBanReason().replace("[wipe]", "") + "\"<newline><newline><reset>Expires in <yellow>"
						+ formattedTimeLeft + "<newline><newline>" + (dbp.getJsonPlayer().getBanReason().contains("[wipe]") ? "<grey>In addition, your player data has been reset<newline><newline><reset>" : "")
						+ "<gray>If you wish to appeal this infraction, please message <yellow>jayphen#6666<gray> on Discord"
		);
		return comp;
	}
}
