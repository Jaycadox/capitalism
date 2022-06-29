package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.database.Database;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;

import java.util.ArrayList;
import java.util.UUID;

public class PlaytimeRewards implements Listener {
	public static ArrayList<UUID> eligiblePlayers = new ArrayList<>();
	public static ArrayList<UUID> redeemedPlayers = new ArrayList<>();
	
	
	public static void register() {
		new BukkitRunnable() {
			@Override
			public void run() {
				showReward();
			}
		}.runTaskTimer(Capitalism.plugin, 60 * 25 * 20, 60 * 25 * 20);
	}
	
	private static void showReward() {
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			eligiblePlayers.add(p.getUniqueId());
			int numberOfClaims = DatabasePlayer.from(p).getJsonPlayer().getData().claims.size();
			if (numberOfClaims == 0) {
				new MessageBuilder("Reward").appendData(Token.TokenType.CHAT, "Click here", "__I_REDEEM_REWARD__")
						.appendCaption("to redeem your playtime reward. This will expire in 2 minutes").send(p);
			} else {
				new MessageBuilder("Reward").appendCaption("Right-click a bed inside a residential " +
				                                           "land claim that you own to redeem your playtime reward. This will expire in 2 minutes")
						.send(p);
			}
			
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (!redeemedPlayers.contains(p.getUniqueId()) && eligiblePlayers.contains(p.getUniqueId())) {
						new MessageBuilder("Reward").appendCaption("Your playtime reward has expired").send(p);
					}
					redeemedPlayers.clear();
					eligiblePlayers.clear();
				}
			}
		}.runTaskLater(Capitalism.plugin, 60 * 20 * 2);
	}
	
	
	public static boolean onChat(AsyncPlayerChatEvent e) {
		if (e.getMessage().equals("__I_REDEEM_REWARD__")) {
			int numberOfClaims = DatabasePlayer.from(e.getPlayer()).getJsonPlayer().getData().claims.size();
			e.setCancelled(true);
			if (numberOfClaims != 0) return true;
			if (!redeemedPlayers.contains(e.getPlayer().getUniqueId()) && eligiblePlayers.contains(e.getPlayer().getUniqueId())) {
				eligiblePlayers.remove(e.getPlayer().getUniqueId());
				redeemedPlayers.add(e.getPlayer().getUniqueId());
				if (Database.injector.inject(e.getPlayer().getUniqueId(), 100000).getType() ==
				    TransactionResult.TransactionResultType.SUCCESS)
				{
					new MessageBuilder("Reward").appendVariable("$100,000")
							.appendCaption("has been added to your balance").send(e.getPlayer());
				}
			}
			return true;
		}
		return false;
	}
	
}
