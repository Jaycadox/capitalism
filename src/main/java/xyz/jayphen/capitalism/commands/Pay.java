package xyz.jayphen.capitalism.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxResult;
import xyz.jayphen.capitalism.events.tax.TaxedDeath;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

public class Pay implements CommandExecutor {

	@Override
	public boolean onCommand (CommandSender commandSender, Command command, String s, String[] args) {
		if (!(commandSender instanceof Player)) {
			commandSender.sendMessage(new MessageBuilder("Economy").append(Token.TokenType.CAPTION, "Only players can use this command").build());
			return true;
		}
		if (args.length != 2) {
			return false;
		}

		// correct for player stupidity
		if (args[0].matches("-?\\d+")) {
			try {
				Integer.parseInt(args[0]);
				String temp = args[1];
				args[1] = args[0];
				args[0] = temp;
			} catch (NumberFormatException e) {
			}
		}
		try {
			Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			commandSender.sendMessage(new MessageBuilder("Economy").append(Token.TokenType.CAPTION, "Invalid amount of money specified").build());
			return true;
		}
		OfflinePlayer otherPlayer = Bukkit.getServer().getOfflinePlayer(args[0]);
		if (!otherPlayer.hasPlayedBefore()) {
			commandSender.sendMessage(new MessageBuilder("Economy").append(Token.TokenType.CAPTION, "The player").append(Token.TokenType.VARIABLE, args[0]).append(Token.TokenType.CAPTION, "could not be found").build());
			return true;
		}
		Player p = (Player) commandSender;
		int amount = Integer.parseInt(args[1]);
		TaxResult tax = TaxedTransaction.INSTANCE.applyTax(amount);

		if((int)tax.getAmountTaxed() + amount > DatabasePlayer.from(p).getMoneySafe()) {
			commandSender.sendMessage(new MessageBuilder("Transaction")
					                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount))
					                          .append(Token.TokenType.CAPTION, "could not be transfered due to")
					                          .append(Token.TokenType.VARIABLE, "insufficient funds")
					                          .build());
			return true;
		}


		Transaction tax_t = new Transaction(p.getUniqueId(), DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(), (int)tax.getAmountTaxed());
		TransactionResult tax_res = tax_t.transact();

		if (tax_res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			commandSender.sendMessage(new MessageBuilder("Transaction").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount + (int)tax.getAmountTaxed())).append(Token.TokenType.CAPTION, "(after tax) could not be sent due to").append(Token.TokenType.VARIABLE, tax_res.getErrorReason()).build());
			return true;
		}
		DatabasePlayer.from(p).getJsonPlayer().getData().stats.amountTaxed += tax.getAmountTaxed();
		DatabasePlayer.from(p).getJsonPlayer().save();
		commandSender.sendMessage(new MessageBuilder("Transaction Tax")
				                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(tax.getAmountTaxed()))
				                          .append(Token.TokenType.CAPTION, "has been removed from your account following a")
				                          .append(Token.TokenType.VARIABLE, tax.getTaxAmount() * 100 + "%")
				                          .append(Token.TokenType.CAPTION, "tax")
				                          .build());

		Transaction t = new Transaction(p, otherPlayer, amount);
		TransactionResult res = t.transact();

		if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			commandSender.sendMessage(new MessageBuilder("Transaction").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount)).append(Token.TokenType.CAPTION, "could not be transferred to").append(Token.TokenType.VARIABLE, otherPlayer.getName()).append(Token.TokenType.CAPTION, "due to").append(Token.TokenType.VARIABLE, res.getErrorReason()).build());
			return true;
		}
		commandSender.sendMessage(new MessageBuilder("Transaction")
				                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount))
				                          .append(Token.TokenType.BRACKET, res.getHash())
				                          .append(Token.TokenType.CAPTION, "has been transferred to")
				                          .append(Token.TokenType.VARIABLE, otherPlayer.getName() + ".")
				                          .append(Token.TokenType.CAPTION, "You now have")
				                          .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(DatabasePlayer.from(p).getMoneySafe())).build());
		if (otherPlayer.isOnline() && otherPlayer.getPlayer() != null) {
			otherPlayer.getPlayer().sendMessage(new MessageBuilder("Transaction").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(amount)).append(Token.TokenType.BRACKET, res.getHash()).append(Token.TokenType.CAPTION, "has been transferred to your balance. You how have").append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(DatabasePlayer.from(otherPlayer).getMoneySafe())).build());
		}


		return true;
	}


}
