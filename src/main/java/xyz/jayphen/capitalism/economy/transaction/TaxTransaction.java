package xyz.jayphen.capitalism.economy.transaction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.events.tax.ITax;
import xyz.jayphen.capitalism.events.tax.TaxResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.UUID;

public class TaxTransaction {
	UUID   from        = null;
	UUID   to          = null;
	int    amount      = 0;
	int    totalAmount = 0;
	String tag         = generateTag();
	
	public TaxTransaction(OfflinePlayer from, OfflinePlayer to, int amount) {
		this.from   = from.getUniqueId();
		this.to     = to.getUniqueId();
		this.amount = amount;
	}
	
	public TaxTransaction(UUID from, UUID to, int amount) {
		this.from   = from;
		this.to     = to;
		this.amount = amount;
	}
	
	public int getTotalAmount() {
		return totalAmount;
	}
	
	public int getTotalAmount(ITax provider) {
		return totalAmount;
	}
	
	private String generateTag() {
		UUID randomUUID = UUID.randomUUID();
		return randomUUID.toString().replaceAll("-", "").substring(0, 4);
	}
	
	public TransactionResult transact(ITax taxProvider, boolean silent) {
		if (Bukkit.getPlayer(from) == null) return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "invalid player");
		TaxResult tax = taxProvider.applyTax(amount);
		totalAmount = (int) ( tax.getAmountTaxed() + amount );
		if ((int) tax.getAmountTaxed() + amount > DatabasePlayer.from(from).getMoneySafe()) {
			if (!silent) new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
					.appendCaption("could not be transfered due to").appendVariable("insufficient funds").send(Bukkit.getPlayer(from));
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds (after tax)");
		}
		
		
		Transaction       tax_t   = new Transaction(from, DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(), (int) tax.getAmountTaxed());
		TransactionResult tax_res = tax_t.transact();
		
		if (tax_res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			if (!silent) new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount + (int) tax.getAmountTaxed()))
					.appendCaption("(after tax) could not be sent due to").appendVariable(tax_res.getErrorReason()).send(Bukkit.getPlayer(from));
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds (after tax)");
		}
		DatabasePlayer.from(from).getJsonPlayer().getData().stats.amountTaxed += tax.getAmountTaxed();
		DatabasePlayer.from(from).getJsonPlayer().save();
		if (!silent) new MessageBuilder("Transaction Tax").appendVariable("$" + NumberFormatter.addCommas(tax.getAmountTaxed()))
				.appendCaption("has been removed from your account following a").appendVariable(tax.getTaxAmount() * 100 + "%").appendCaption("tax")
				.send(Bukkit.getPlayer(from));
		
		Transaction       t   = new Transaction(from, to, amount);
		TransactionResult res = t.transact();
		
		if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			if (!silent) new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
					.appendCaption("could not be transferred to").appendVariable(Bukkit.getOfflinePlayer(to).getName()).appendCaption("due to")
					.appendVariable(res.getErrorReason()).send(Bukkit.getPlayer(from));
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, res.getErrorReason());
		}
		return new TransactionResult(TransactionResult.TransactionResultType.SUCCESS, tag, "");
	}
}
