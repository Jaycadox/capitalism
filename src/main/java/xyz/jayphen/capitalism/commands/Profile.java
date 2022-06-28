package xyz.jayphen.capitalism.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.TaxTransaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.InventoryHelperEvent;
import xyz.jayphen.capitalism.events.tax.TaxedDeath;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.helpers.InventoryHelper;
import xyz.jayphen.capitalism.helpers.InventoryScroll;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Profile implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
		if (!( commandSender instanceof Player p )) return true;
		final InventoryScroll[] scroll         = { null };
		final InventoryScroll[] offersScroll   = { null };
		DatabasePlayer          databasePlayer = DatabasePlayer.from(p);
		InventoryHelper inventoryHelper = new InventoryHelper(p.getName() + "'s profile", 3, (helper, submenu) -> {
			helper.setMargin(1, 1);
			if (submenu.equals("root")) {
				helper.setMargin(3, 1);
				helper.setItem(0, 0, "&eBank account & Stats", Material.RAW_GOLD, () -> {
					helper.push("bank", 3);
				});
				helper.setItem(0, 1, "&eLand claims", Material.FILLED_MAP, () -> {
					helper.push("claims", 3);
				});
				var offers = databasePlayer.getJsonPlayer().getData().claimOffers.size();
				helper.setItem(0, 2, "&eLand offers", Material.SUNFLOWER, () -> {
					helper.push("offers", 3);
				}, List.of("", offers == 0 ? "&7No current offers" : "&a&l" + offers + " CURRENT OFFER" + ( offers == 1 ? "" : "S" )));
				
			}
			AtomicInteger count = new AtomicInteger();
			if (submenu.equals("claims")) {
				List<InventoryScroll.ItemRunnable> itemRunnables = databasePlayer.getJsonPlayer().getData().claims.stream().map(x -> {
					ItemStack it   = new ItemStack(count.get() % 2 == 0 ? Material.MAP : Material.FILLED_MAP);
					ItemMeta  meta = it.getItemMeta();
					meta.setDisplayName(ChatColor.YELLOW + x.getName() + ChatColor.GRAY + " (" + count.get() + ")");
					it.setItemMeta(meta);
					int id = count.get();
					Runnable r = () -> {
						InventoryHelper.close(p);
						p.performCommand("landlord id=" + id);
					};
					count.getAndIncrement();
					return new InventoryScroll.ItemRunnable(it, r);
				}).collect(Collectors.toList());
				if (scroll[0] == null) scroll[0] = new InventoryScroll(0, 6, helper, itemRunnables, submenu, p.getUniqueId());
				else scroll[0].setView(itemRunnables);
				scroll[0].render();
			}
			if (submenu.equals("offers")) {
				List<InventoryScroll.ItemRunnable> itemRunnables = databasePlayer.getJsonPlayer().getClaimOffers().stream().map(x -> {
					Claim     c    = DatabasePlayer.getClaimFromClaimOffer(x);
					String    area = "(" + c.location.startX + ", " + c.location.startZ + " -> " + c.location.endX + ", " + c.location.endZ + ")";
					ItemStack it   = new ItemStack(count.get() % 2 == 0 ? Material.MAP : Material.FILLED_MAP);
					ItemMeta  meta = it.getItemMeta();
					meta.setDisplayName(
							ChatColor.GREEN + "$" + NumberFormatter.addCommas(x.price) + " " + ChatColor.YELLOW + c.getName() + ChatColor.GRAY +
							" (" + c.getArea() + " blocks)");
					meta.setLore(List.of("&7Location: &e" + area, "&7Market value: &e$" + NumberFormatter.addCommas(c.getEstWorth()),
					                     "&7Owner: &e" + Bukkit.getOfflinePlayer(UUID.fromString(c.owner)).getName(), "", "&7Left-click to: &a&lBUY",
					                     "&7Right-click to: &c&lREJECT"
					).stream().map(msg -> ChatColor.translateAlternateColorCodes('&', msg)).collect(Collectors.toList()));
					it.setItemMeta(meta);
					Runnable r = () -> {
						if (InventoryHelperEvent.CLICK_TYPES.get(p.getUniqueId())) {
							TaxTransaction transaction = new TaxTransaction(p, Bukkit.getOfflinePlayer(UUID.fromString(c.owner)), x.price);
							var            result      = transaction.transact(TaxedTransaction.INSTANCE, true);
							if (result.getType() != TransactionResult.TransactionResultType.SUCCESS) {
								new MessageBuilder("Land Purchase").appendCaption("Transaction failed due to:")
										.appendVariable(result.getErrorReason()).send(p);
								return;
							}
							DatabasePlayer.from(UUID.fromString(DatabasePlayer.getClaimFromClaimOffer(x).owner))
									.deleteAllOffersForClaim(DatabasePlayer.getClaimFromClaimOffer(x));
							DatabasePlayer.from(UUID.fromString(DatabasePlayer.getClaimFromClaimOffer(x).owner)).getJsonPlayer().save();
							ClaimManager.getDatabaseClaim(c).transfer(p.getUniqueId());
							OfflinePlayer other = Bukkit.getOfflinePlayer(UUID.fromString(c.owner));
							DatabasePlayer.from(other.getUniqueId()).getJsonPlayer().queueMessage(
									new MessageBuilder("Land").appendCaption("You no longer own the land at").appendVariable(area + ".")
											.appendCaption("This is because ownership has been purchased by").appendVariable(p.getName())
											.appendCaption("for").appendVariable("$" + NumberFormatter.addCommas(x.price)).make());
							
							new MessageBuilder("Land").appendCaption("You now own the land at").appendVariable(area + ".")
									.appendCaption("This action has costed you")
									.appendVariable("$" + NumberFormatter.addCommas(transaction.getTotalAmount(TaxedTransaction.INSTANCE))).send(p);
							InventoryHelper.close(p);
						} else {
							DatabasePlayer.from(UUID.fromString(DatabasePlayer.getClaimFromClaimOffer(x).owner))
									.deleteAllOffersForClaim(DatabasePlayer.getClaimFromClaimOffer(x));
							DatabasePlayer.from(UUID.fromString(DatabasePlayer.getClaimFromClaimOffer(x).owner)).getJsonPlayer().save();
							new MessageBuilder("Land").appendCaption("Offer has been rejected").send(p);
							DatabasePlayer.from(UUID.fromString(DatabasePlayer.getClaimFromClaimOffer(x).owner)).getJsonPlayer().queueMessage(
									new MessageBuilder("Land").appendCaption("Your land claim offer to").appendVariable(p.getName())
											.appendCaption("has been rejected").make());
						}
						
					};
					count.getAndIncrement();
					return new InventoryScroll.ItemRunnable(it, r);
				}).collect(Collectors.toList());
				if (offersScroll[0] == null) offersScroll[0] = new InventoryScroll(0, 6, helper, itemRunnables, submenu, p.getUniqueId());
				else offersScroll[0].setView(itemRunnables);
				offersScroll[0].render();
				
			}
			if (submenu.equals("bank")) {
				helper.setItem(0, 0, "&eMoney", Material.GREEN_DYE, () -> {
				}, List.of("&7You have: &a&l$" + NumberFormatter.addCommas(databasePlayer.getMoneySafe())));
				helper.setItem(0, 1, "&eTaxes", Material.BARRIER, () -> {
				}, List.of("&7You've paid: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.amountTaxed) +
				           "&r&7 in tax"));
				helper.setItem(0, 2, "&eMoney received", Material.PINK_SHULKER_BOX, () -> {
				}, List.of("&7You've received: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.moneyRecieved)));
				helper.setItem(0, 3, "&eMoney sent", Material.ENDER_PEARL, () -> {
				}, List.of("&7You've sent: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.moneySent)));
				helper.setItem(0, 4, "&eTax brackets", Material.IRON_BARS, () -> {
				}, List.of("&7Transaction tax: &e" +
				           (int) ( 100 * TaxedTransaction.INSTANCE.applyTax((int) databasePlayer.getMoneySafe()).getTaxAmount() ) + "%",
				           "&7Death tax: &e" + (int) ( 100 * TaxedDeath.INSTANCE.applyTax((int) databasePlayer.getMoneySafe()).getTaxAmount() ) + "%"
				));
			}
		});
		inventoryHelper.show(p);
		return true;
	}
}
