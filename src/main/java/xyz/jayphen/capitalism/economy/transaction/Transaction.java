package xyz.jayphen.capitalism.economy.transaction;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.UUID;

public class Transaction {
	UUID from   = null;
	UUID to     = null;
	int  amount = 0;
	
	
	public Transaction(OfflinePlayer from, OfflinePlayer to, int amount) {
		this.from   = from.getUniqueId();
		this.to     = to.getUniqueId();
		this.amount = amount;
	}
	
	public Transaction(UUID from, UUID to, int amount) {
		this.from   = from;
		this.to     = to;
		this.amount = amount;
	}
	
	private String generateTag() {
		UUID randomUUID = UUID.randomUUID();
		return randomUUID.toString().replaceAll("-", "").substring(0, 4);
	}
	
	public void sendWatermark(UUID p) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(p);
		if (player.getPlayer() != null && player.hasPlayedBefore()) {
			new MessageBuilder("Economy").appendCaption("Transaction pending...").sendActionBar(player.getPlayer());
		}
	}
	
	public void checkEconomyIntegrity() {
		return; //todo: fix
	}
	
	public TransactionResult transact() {
		String tag = generateTag();
		sendWatermark(from);
		if (amount < 1) {
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "cannot transact less than $1");
		}
		
		//if (to == from) {
		//	return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "cannot transact to the same player");
		//}
		
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
		DatabasePlayer.from(from).getJsonPlayer().getData().stats.moneySent += amount;
		DatabasePlayer.from(from).getJsonPlayer().save();
		DatabasePlayer.from(to).getJsonPlayer().getData().stats.moneyRecieved += amount;
		DatabasePlayer.from(to).getJsonPlayer().save();
		return new TransactionResult(TransactionResult.TransactionResultType.SUCCESS, tag);
	}
}
