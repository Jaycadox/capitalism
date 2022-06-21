package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Lottery implements Listener {
	static int amount = 0;
	private static boolean exists() {
		try {
			DatabaseMetaData md = Capitalism.db.connect().getMetaData();
			ResultSet rs = md.getColumns(null, null, "players", "joined_lottery");
			return rs.next();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void generate() {
		try {
			Connection ctn = Capitalism.db.connect();
			Statement statement = ctn.createStatement();
			statement.execute("ALTER TABLE players ADD joined_lottery INT(1);");
			statement.execute("UPDATE players SET joined_lottery = 0;");
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	public static void nag(Player p) {
		if(amount == 0) return;
		if(DatabasePlayer.from(p).getJoinedLottery()) return;
		MessageBuilder mb = new MessageBuilder("Lottery")
				.append(Token.TokenType.CAPTION, "Click here to join lottery for")
				.append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount) + ".")
				.append(Token.TokenType.CAPTION, "Lottery's are drawn around 5-6PM Sydney time. You must be online when they're drawn.");
		p.spigot().sendMessage(mb.buildWithCommand("__I_JOIN_LOTTERY__"));
	}
	public static void register () {
		if(!exists())
			generate();

		new BukkitRunnable() {
			@Override
			public void run () {
				runLotteryLoop();
			}
		}.runTaskTimer(Capitalism.plugin, 0, 60 * 60 * 20);
	}
	private static void runLotteryLoop() {
		if (!checkForLottery()) {
			return;
		}
		if(amount != 0)
			handleWin();
		amount = (int) Math.min(Database.injector.getInjector().getMoneySafe(), 200000);
		if(amount < 200000) {
			Bukkit.getServer().broadcastMessage(
					new MessageBuilder("Lottery")
							.append(Token.TokenType.CAPTION, "Bank is currently too poor for a lottery to be hosted")
							.build()
			);
			amount = 0;
			return;
		}
		MessageBuilder mb = new MessageBuilder("Lottery")
				.append(Token.TokenType.CAPTION, "Click here to join lottery for")
				.append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount) + ".")
				.append(Token.TokenType.CAPTION, "Lottery's are drawn around 5-6PM Sydney time. You must be online when they're drawn.");
		Bukkit.getConsoleSender().sendMessage(mb.build());
		Bukkit.getServer().spigot().broadcast(mb
			.buildWithCommand("__I_JOIN_LOTTERY__"));
	}
	private static void handleWin() {
		ArrayList<UUID> entered = DatabasePlayer.allLotteryEnteredPeople();
		for(UUID u : entered) {
			DatabasePlayer.from(u).setJoinedLottery(false);
		}
		if(entered.size() < 2) {
			Bukkit.broadcastMessage(
					new MessageBuilder("Lottery")
							.append(Token.TokenType.CAPTION, "Not enough people joined the lottery for a roll")
							.build()
			);
			amount = 0;
			return;
		}
		ArrayList<UUID> eligible = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers())
		{
			if(entered.contains(p.getUniqueId()))
			{
				eligible.add(p.getUniqueId());
			}
		}
		if(eligible.isEmpty()) {
			Bukkit.broadcastMessage(
					new MessageBuilder("Lottery")
							.append(Token.TokenType.CAPTION, "Nobody who entered is online, skipping roll")
							.build()
			);
			amount = 0;
			return;
		}


		OfflinePlayer selected = Bukkit.getOfflinePlayer(random(eligible));
		new Transaction(Database.injector.getInjector().getUuid(), selected.getUniqueId(), amount).transact();
		Bukkit.broadcastMessage(
				new MessageBuilder("Lottery")
						.append(Token.TokenType.VARIABLE, selected.getName())
						.append(Token.TokenType.CAPTION, "has won the lottery for")
						.append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount) + "!")
						.build()
		);
		amount = 0;
	}
	public static UUID random(List<UUID> lottery) {
		return lottery.get(new Random().nextInt(lottery.size()));
	}
	private static boolean checkForLottery() {
		Calendar rightNow = Calendar.getInstance();
		int hour = rightNow.get(Calendar.HOUR_OF_DAY);
		return hour == (12 + 8);
	}

	@EventHandler
	public void onChat (AsyncPlayerChatEvent e) {
		if (e.getMessage().equals("__I_JOIN_LOTTERY__")) {
			e.setCancelled(true);
			if(DatabasePlayer.from(e.getPlayer()).getJoinedLottery()) {
				e.getPlayer().sendMessage(
						new MessageBuilder("Lottery")
								.append(Token.TokenType.CAPTION, "You have already been entered in the lottery")
								.build());
				return;
			}
			DatabasePlayer.from(e.getPlayer()).setJoinedLottery(true);
			e.getPlayer().sendMessage(
					new MessageBuilder("Lottery")
							.append(Token.TokenType.CAPTION, "You've entered the lottery, tune in at around 5-6PM AEST for the roll!")
							.build()
			);
		}
	}
}
