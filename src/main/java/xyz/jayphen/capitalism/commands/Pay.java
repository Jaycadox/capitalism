package xyz.jayphen.capitalism.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxResult;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

public class Pay implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
		if (!( commandSender instanceof Player )) {
			new MessageBuilder("Economy").appendCaption("Only players can use this command").send(commandSender);
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
			new MessageBuilder("Economy").appendCaption("Invalid amount of money specified").send(commandSender);
			return true;
		}
		OfflinePlayer otherPlayer = Bukkit.getServer().getOfflinePlayer(args[0]);
		if (!otherPlayer.hasPlayedBefore()) {
			new MessageBuilder("Economy").appendCaption("The player").appendVariable(args[0])
					.appendCaption("could not be found").send(commandSender);
			return true;
		}
		Player    p      = (Player) commandSender;
		int       amount = Integer.parseInt(args[1]);
		TaxResult tax    = TaxedTransaction.INSTANCE.applyTax(amount);
		
		if ((int) tax.getAmountTaxed() + amount > DatabasePlayer.from(p).getMoneySafe()) {
			new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
					.appendCaption("could not be transfered due to").appendVariable("insufficient funds").send(commandSender);
			return true;
		}
		
		
		Transaction       tax_t   = new Transaction(p.getUniqueId(), DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(),
		                                            (int) tax.getAmountTaxed()
		);
		TransactionResult tax_res = tax_t.transact();
		
		if (tax_res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount + (int) tax.getAmountTaxed()))
					.appendCaption("(after tax) could not be sent due to").appendVariable(tax_res.getErrorReason()).send(commandSender);
			return true;
		}
		DatabasePlayer.from(p).getJsonPlayer().getData().stats.amountTaxed += tax.getAmountTaxed();
		DatabasePlayer.from(p).getJsonPlayer().save();
		new MessageBuilder("Transaction Tax").appendVariable("$" + NumberFormatter.addCommas(tax.getAmountTaxed()))
				.appendCaption("has been removed from your account following a").appendVariable(tax.getTaxAmount() * 100 + "%")
				.appendCaption("tax").send(commandSender);
		
		Transaction       t   = new Transaction(p, otherPlayer, amount);
		TransactionResult res = t.transact();
		
		if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
			new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
					.appendCaption("could not be transferred to").appendVariable(otherPlayer.getName())
					.appendCaption("due to").appendVariable(res.getErrorReason()).send(commandSender);
			return true;
		}
		new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
				.append(Token.TokenType.BRACKET, res.getHash())
				.appendCaption("has been transferred to").appendVariable(otherPlayer.getName() + ".")
				.appendCaption("You now have").appendVariable("$" + NumberFormatter.addCommas(DatabasePlayer.from(p).getMoneySafe()))
				.send(commandSender);
		if (otherPlayer.isOnline() && otherPlayer.getPlayer() != null) {
			new MessageBuilder("Transaction").appendVariable("$" + NumberFormatter.addCommas(amount))
					.append(Token.TokenType.BRACKET, res.getHash())
					.appendCaption("has been transferred to your balance. You how have")
					.appendVariable("$" + NumberFormatter.addCommas(DatabasePlayer.from(otherPlayer).getMoneySafe())).send(otherPlayer.getPlayer());
		}
		
		
		return true;
	}
	
	
}
