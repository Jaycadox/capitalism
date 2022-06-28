package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
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
			ResultSet        rs = md.getColumns(null, null, "players", "joined_lottery");
			return rs.next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static void generate() {
		try {
			Connection ctn       = Capitalism.db.connect();
			Statement  statement = ctn.createStatement();
			statement.execute("ALTER TABLE players ADD joined_lottery INT(1);");
			statement.execute("UPDATE players SET joined_lottery = 0;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void nag(Player p) {
		if (amount == 0) return;
		if (DatabasePlayer.from(p).getJoinedLottery()) return;
		new MessageBuilder("Lottery").appendData(Token.TokenType.CHAT, "Click here", "__I_JOIN_LOTTERY__").appendCaption("to join lottery for")
				.appendVariable("$" + NumberFormatter.addCommas(amount) + ".")
				.appendCaption("Lottery's are drawn around 5-6PM Sydney time. You must be online when they're drawn.").send(p);
		
	}
	
	public static void register() {
		if (!exists()) generate();
		
		new BukkitRunnable() {
			@Override
			public void run() {
				runLotteryLoop();
			}
		}.runTaskTimer(Capitalism.plugin, 0, 60 * 60 * 20);
	}
	
	private static void runLotteryLoop() {
		if (!checkForLottery()) {
			return;
		}
		if (amount != 0) handleWin();
		amount = (int) Math.min(Database.injector.getInjector().getMoneySafe(), 200000);
		if (amount < 200000) {
			
			new MessageBuilder("Lottery").appendCaption("Bank is currently too poor for a lottery to be hosted").broadcast();
			
			amount = 0;
			return;
		}
		var msg = new MessageBuilder("Lottery").appendData(Token.TokenType.CHAT, "Click here", "__I_JOIN_LOTTERY__")
				.appendCaption("to join lottery for").appendVariable("$" + NumberFormatter.addCommas(amount) + ".")
				.appendCaption("Lottery's are drawn around 5-6PM Sydney time. You must be online when they're drawn.");
		Bukkit.getOnlinePlayers().stream().filter(x -> !DatabasePlayer.from(x).getJoinedLottery()).forEach(msg::send);
		
	}
	
	private static void handleWin() {
		ArrayList<UUID> entered = DatabasePlayer.allLotteryEnteredPeople();
		for (UUID u : entered) {
			DatabasePlayer.from(u).setJoinedLottery(false);
		}
		if (entered.size() < 2) {
			
			new MessageBuilder("Lottery").appendCaption("Not enough people joined the lottery for a roll").broadcast();
			amount = 0;
			return;
		}
		ArrayList<UUID> eligible = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (entered.contains(p.getUniqueId())) {
				eligible.add(p.getUniqueId());
			}
		}
		if (eligible.isEmpty()) {
			
			new MessageBuilder("Lottery").appendCaption("Nobody who entered is online, skipping roll").broadcast();
			
			amount = 0;
			return;
		}
		
		
		OfflinePlayer selected = Bukkit.getOfflinePlayer(random(eligible));
		new Transaction(Database.injector.getInjector().getUuid(), selected.getUniqueId(), amount).transact();
		new MessageBuilder("Lottery").appendVariable(selected.getName()).appendCaption("has won the lottery for")
				.appendVariable("$" + NumberFormatter.addCommas(amount) + "!").broadcast();
		amount = 0;
	}
	
	public static UUID random(List<UUID> lottery) {
		return lottery.get(new Random().nextInt(lottery.size()));
	}
	
	private static boolean checkForLottery() {
		Calendar rightNow = Calendar.getInstance();
		int      hour     = rightNow.get(Calendar.HOUR_OF_DAY);
		return hour == ( 0 );
	}
	
	
	public static boolean onChat(AsyncPlayerChatEvent e) {
		if (e.getMessage().equals("__I_JOIN_LOTTERY__")) {
			e.setCancelled(true);
			if (DatabasePlayer.from(e.getPlayer()).getJoinedLottery()) {
				new MessageBuilder("Lottery").appendCaption("You have already been entered in the lottery").send(e.getPlayer());
				return true;
			}
			DatabasePlayer.from(e.getPlayer()).setJoinedLottery(true);
			new MessageBuilder("Lottery").appendCaption("You've entered the lottery, tune in at around 5-6PM AEST for the roll!").send(e.getPlayer());
			return true;
			
		}
		return false;
	}
}
