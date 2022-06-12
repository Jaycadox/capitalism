package xyz.jayphen.capitalism.economy.transaction;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;

import java.util.UUID;

public class Transaction {
	UUID from = null;
	UUID to = null;
	int amount = 0;


	public Transaction (OfflinePlayer from, OfflinePlayer to, int amount) {
		this.from = from.getUniqueId();
		this.to = to.getUniqueId();
		this.amount = amount;
	}

	public Transaction (UUID from, UUID to, int amount) {
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	private String generateTag () {
		UUID randomUUID = UUID.randomUUID();
		return randomUUID.toString().replaceAll("-", "").substring(0, 4);
	}

	public void sendWatermark (UUID p) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(p);
		if (player.getPlayer() != null && player.hasPlayedBefore()) {
			player.getPlayer().sendMessage(new MessageBuilder("Economy").append(Token.TokenType.CAPTION, "Transaction pending...").build());
		}
	}

	public void checkEconomyIntegrity () {
		long money = DatabasePlayer.sumMoney();
		if (money == 0) {
			Capitalism.LOG.warning("[INTEGRITY] Failed to sum economy");
			return;
		}
		long knownMoney = Database.injector.getMoney();
		if (knownMoney == 0) {
			Capitalism.LOG.warning("[INTEGRITY] Failed to get economy sum");
			return;
		}
		if (knownMoney != money) {
			Capitalism.LOG.warning("[INTEGRITY] Mismatch of " + Math.abs(knownMoney - money) + " detected in economy.");
		} else {
			Capitalism.LOG.info("[INTEGRITY] No economy mismatch detected.");

		}
	}

	public TransactionResult transact () {
		String tag = generateTag();
		sendWatermark(from);
		if (amount < 1) {
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "cannot transact less than $1");
		}

		if (to == from) {
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "cannot transact to the same player");
		}

		EconomyResponse withdraw = Capitalism.eco.withdrawPlayer(Bukkit.getOfflinePlayer(from), amount);

		if (withdraw.type != EconomyResponse.ResponseType.SUCCESS) {
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds");
		}
		sendWatermark(to);
		if (Capitalism.eco.depositPlayer(Bukkit.getOfflinePlayer(to), amount).type != EconomyResponse.ResponseType.SUCCESS) {
			Capitalism.eco.depositPlayer(Bukkit.getOfflinePlayer(from), amount);
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "general error");
		}
		checkEconomyIntegrity();
		return new TransactionResult(TransactionResult.TransactionResultType.SUCCESS, tag);
	}
}
