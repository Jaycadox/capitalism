package xyz.jayphen.capitalism.economy.transaction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.events.tax.ITax;
import xyz.jayphen.capitalism.events.tax.TaxResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

import java.util.UUID;

public class TaxTransaction {
	UUID from = null;
	UUID to = null;
	int amount = 0;

	public int getTotalAmount () {
		return totalAmount;
	}

	int totalAmount = 0;

	public TaxTransaction (OfflinePlayer from, OfflinePlayer to, int amount) {
		this.from = from.getUniqueId();
		this.to = to.getUniqueId();
		this.amount = amount;
	}

	public TaxTransaction (UUID from, UUID to, int amount) {
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	private String generateTag () {
		UUID randomUUID = UUID.randomUUID();
		return randomUUID.toString().replaceAll("-", "").substring(0, 4);
	}
	String tag = generateTag();
	public TransactionResult transact (ITax taxProvider, boolean silent) {
		TaxResult tax = taxProvider.applyTax(amount);
		totalAmount = (int) (tax.getAmountTaxed() + amount);
		if((int)tax.getAmountTaxed() + amount > DatabasePlayer.from(from).getMoneySafe()) {
			if(!silent)
				Bukkit.getPlayer(from).sendMessage(new MessageBuilder("Transaction")
					                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount))
					                          .append(Token.TokenType.CAPTION, "could not be transfered due to")
					                          .append(Token.TokenType.VARIABLE, "insufficient funds")
					                          .build());
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds (after tax)");
		}


		Transaction tax_t = new Transaction(from, DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(), (int)tax.getAmountTaxed());
		TransactionResult tax_res = tax_t.transact();

		if (tax_res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			if(!silent)
				Bukkit.getPlayer(from).sendMessage(new MessageBuilder("Transaction").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount + (int)tax.getAmountTaxed())).append(Token.TokenType.CAPTION, "(after tax) could not be sent due to").append(Token.TokenType.VARIABLE, tax_res.getErrorReason()).build());
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds (after tax)");
		}
		DatabasePlayer.from(from).getJsonPlayer().getData().stats.amountTaxed += tax.getAmountTaxed();
		DatabasePlayer.from(from).getJsonPlayer().save();
		if(!silent)
			Bukkit.getPlayer(from).sendMessage(new MessageBuilder("Transaction Tax")
				                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(tax.getAmountTaxed()))
				                          .append(Token.TokenType.CAPTION, "has been removed from your account following a")
				                          .append(Token.TokenType.VARIABLE, tax.getTaxAmount() * 100 + "%")
				                          .append(Token.TokenType.CAPTION, "tax")
				                          .build());

		Transaction t = new Transaction(from, to, amount);
		TransactionResult res = t.transact();

		if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			if(!silent)
				Bukkit.getPlayer(from).sendMessage(new MessageBuilder("Transaction").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount)).append(Token.TokenType.CAPTION, "could not be transferred to").append(Token.TokenType.VARIABLE, Bukkit.getOfflinePlayer(to).getName()).append(Token.TokenType.CAPTION, "due to").append(Token.TokenType.VARIABLE, res.getErrorReason()).build());
			return new TransactionResult(TransactionResult.TransactionResultType.FAIL, tag, "insufficient funds");
		}
		return new TransactionResult(TransactionResult.TransactionResultType.SUCCESS, tag, "");
	}
}
