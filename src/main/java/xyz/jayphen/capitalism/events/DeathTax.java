package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.Transaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxResult;
import xyz.jayphen.capitalism.events.tax.TaxedDeath;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

public class DeathTax implements Listener {
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		DatabasePlayer dbp = DatabasePlayer.from(event.getEntity());
		if (dbp.getMoneySafe() == 0) return;
		TaxResult tax = TaxedDeath.INSTANCE.applyTax((int) dbp.getMoneySafe());
		if (tax.getAmountTaxed() == 0) return;
		
		Transaction t = new Transaction(event.getEntity().getUniqueId(), DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(),
		                                (int) tax.getAmountTaxed()
		);
		
		TransactionResult result = t.transact();
		if (result.getType() == TransactionResult.TransactionResultType.ERROR) {
			new MessageBuilder("Death Tax").appendCaption("Failed to apply death tax. You've gotten lucky this time >:(").send(event.getEntity());
			return;
		}
		dbp.getJsonPlayer().getData().stats.amountTaxed += tax.getAmountTaxed();
		dbp.getJsonPlayer().save();
		new MessageBuilder("Death Tax").appendVariable("$" + NumberFormatter.addCommas(tax.getAmountTaxed()))
				.appendCaption("has been deducted from your account. This was").appendVariable(( Math.ceil(tax.getTaxAmount() * 100) ) + "%")
				.appendCaption("of your account's balance").send(event.getEntity());
	}
}
