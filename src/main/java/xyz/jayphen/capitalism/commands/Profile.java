package xyz.jayphen.capitalism.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.events.tax.TaxedDeath;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.helpers.InventoryHelper;
import xyz.jayphen.capitalism.helpers.InventoryScroll;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Profile implements CommandExecutor {
	@Override
	public boolean onCommand (CommandSender commandSender, Command command, String s, String[] args) {
		if(!(commandSender instanceof Player p)) return true;
		final InventoryScroll[] scroll = {null};
		DatabasePlayer databasePlayer = DatabasePlayer.from(p);
		InventoryHelper inventoryHelper = new InventoryHelper(p.getName() + "'s profile", 3, (helper, submenu) -> {
			helper.setMargin(1, 1);
			if(submenu.equals("root")) {
				helper.setMargin(3, 1);
				helper.setItem(0, 0, "&eBank account & Stats", Material.RAW_GOLD, () -> {
					helper.push("bank", 3);
				});
				helper.setItem(0, 2, "&eLand claims", Material.FILLED_MAP, () -> {
					helper.push("claims", 3);
				});
				helper.addMargin(0, 1);
			}
			AtomicInteger count = new AtomicInteger();
			if(submenu.equals("claims")) {
				List<InventoryScroll.ItemRunnable> itemRunnables = databasePlayer.getJsonPlayer().getData().claims.stream().map(
						x -> {
							ItemStack it = new ItemStack(count.get() % 2 == 0 ? Material.MAP : Material.FILLED_MAP);
							ItemMeta meta = it.getItemMeta();
							meta.setDisplayName(ChatColor.YELLOW + x.getName() + ChatColor.GRAY + " (" + count.get() + ")");
							it.setItemMeta(meta);
							int id = count.get();
							Runnable r = () -> {
								p.closeInventory();
								p.performCommand("landlord id=" + id);
							};
							count.getAndIncrement();
							return new InventoryScroll.ItemRunnable(
									it, r
							);
						}
				).collect(Collectors.toList());
				if(scroll[0] == null)
					scroll[0] = InventoryScroll.get(0, 6, helper, itemRunnables);
				scroll[0].render();

			}
			if(submenu.equals("bank")) {
				helper.setItem(0, 0, "&eMoney", Material.GREEN_DYE, () -> {
				}, List.of("&7You have: &a&l$" + NumberFormatter.addCommas(databasePlayer.getMoneySafe())));
				helper.setItem(0, 1, "&eTaxes", Material.BARRIER, () -> {
				}, List.of("&7You've paid: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.amountTaxed) + "&r&7 in tax"));
				helper.setItem(0, 2, "&eMoney received", Material.PINK_SHULKER_BOX, () -> {
				}, List.of("&7You've received: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.moneyRecieved)));
				helper.setItem(0, 3, "&eMoney sent", Material.ENDER_PEARL, () -> {
				}, List.of("&7You've sent: &a&l$" + NumberFormatter.addCommas(databasePlayer.getJsonPlayer().getData().stats.moneySent)));
				helper.setItem(0, 4, "&eTax brackets", Material.IRON_BARS, () -> {
				}, List.of("&7Transaction tax: &e" + (100 * TaxedTransaction.INSTANCE.applyTax((int) databasePlayer.getMoneySafe()).getTaxAmount()) + "%",
				           "&7Death tax: &e" + (100 * TaxedDeath.INSTANCE.applyTax((int) databasePlayer.getMoneySafe()).getTaxAmount()) + "%"
				));
			}
		});
		inventoryHelper.show(p);
		return true;
	}
}
