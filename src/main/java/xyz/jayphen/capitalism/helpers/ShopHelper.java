package xyz.jayphen.capitalism.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimItemShop;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.TaxTransaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.*;
import java.util.stream.Collectors;

public class ShopHelper {
	public static Inventory getInventoryFromSign(Location loc) {
		Block b = loc.getBlock();
		if (!( b.getState() instanceof Sign s )) return null;
		var data = s.getBlockData();
		if (!( data instanceof Directional d )) return null;
		Block potChest = b.getRelative(d.getFacing().getOppositeFace());
		if (potChest.getState() instanceof Chest chest) {
			return chest.getInventory();
		}
		return null;
	}
	
	public static Chest getChestFromSign(Location loc) {
		Block b = loc.getBlock();
		if (!( b.getState() instanceof Sign s )) return null;
		var data = s.getBlockData();
		if (!( data instanceof Directional d )) return null;
		Block potChest = b.getRelative(d.getFacing().getOppositeFace());
		if (potChest.getState() instanceof Chest chest) {
			return chest;
		}
		return null;
	}
	
	public static int getPriceFromSign(Location loc) {
		if (!( loc.getBlock().getState() instanceof Sign d )) return 0;
		var lines = d.lines();
		if (lines.isEmpty()) return 0;
		String plainText = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
		if (!plainText.startsWith("$")) return 0;
		int amount = 0;
		try {
			amount = Integer.parseInt(plainText.replace("$", ""));
		} catch (Exception ignored) {
		}
		if (1 > amount) return 0;
		return amount;
	}
	
	public static void openShop(Player p, ClaimItemShop shop, Chest chest, Claim claim) {
		InventoryHelper inventoryHelper = new InventoryHelper("Shop | $" + NumberFormatter.addCommas(shop.getPrice()) + "/item",
		                                                      chest.getInventory().getSize() / 9, (helper, submenu) -> {
			ArrayList<ItemStack> rawItems = new ArrayList<>();
			for (int i = 0; i < chest.getInventory().getSize() - 1; i++) {
				rawItems.add(chest.getInventory().getItem(i));
			}
			ArrayList<ItemStack> items = rawItems.stream().filter(x -> x != null && x.getType() != Material.AIR)
					.collect(Collectors.toCollection(ArrayList::new));
			ArrayList<ItemStack> filteredItems = items.stream().map(ItemStack::new).collect(Collectors.toCollection(ArrayList::new));
			final int[]          index         = { 0 };
			
			var itemMap = new HashMap<ItemStack, ItemStack>();
			
			ArrayList<ItemStack> displayItems = filteredItems.stream().map(x -> {
				var newStack = new ItemStack(x);
				newStack.setAmount(1);
				var meta = newStack.getItemMeta();
				newStack.lore(List.of(Component.text(""), MiniMessage.miniMessage()
						.deserialize("<reset><yellow>" + getQuantity(items, filteredItems.get(index[0])) + " in stock</yellow>")));
				itemMap.put(newStack, filteredItems.get(index[0]));
				index[0]++;
				//newStack.setItemMeta(meta);
				return newStack;
			}).distinct().collect(Collectors.toCollection(ArrayList::new));
			int rowsHas = chest.getInventory().getSize() / 9;
			int rows    = rowsHas - (int) Math.floor(Math.ceil((double) displayItems.size() / 9.0) / 2.0) - 1;
			helper.setMargin(0, rows / 2);
			for (int i = 0; i < displayItems.size(); i++) {
				int finalI = i;
				helper.setItem(i, displayItems.get(i), () -> {
					ChatInput.createQuery(
							"quantity of " + displayItems.get(finalI).getType().name().toLowerCase(Locale.ROOT).replace("_", " ") + " to purchase",
							response -> {
								int amount = 0;
								try {
									amount = Integer.parseInt(response);
								} catch (Exception ignored) {
								}
								if (amount <= 0) return;
								int quantity = getQuantity(items, itemMap.get(displayItems.get(finalI)));
								if (quantity < amount) {
									new MessageBuilder("Shop").appendCaption("There isn't enough of that specific item in stock to sell").send(p);
									return;
								}
								var removeAmounts = getRemoveAmounts(rawItems, itemMap.get(displayItems.get(finalI)), amount);
								if (removeAmounts == null) {
									new MessageBuilder("Shop").appendCaption(
											"An error occurred whilst attempting to calculate placements. This is most likely a bug").send(p);
									return;
								}
								TaxTransaction trans = new TaxTransaction(p.getUniqueId(),
								                                          Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getUniqueId(),
								                                          amount * shop.getPrice()
								);
								var res = trans.transact(TaxedTransaction.INSTANCE, true);
								if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
									new MessageBuilder("Shop").appendCaption("Transaction failed due to").appendVariable(res.getErrorReason())
											.send(p);
									return;
								}
								
								new MessageBuilder("Shop").appendCaption("Purchased")
										.appendComponent(itemMap.get(displayItems.get(finalI)).displayName().color(NamedTextColor.YELLOW))
										.appendVariable("x" + amount).appendCaption("for")
										.appendVariable("$" + NumberFormatter.addCommas(trans.getTotalAmount(TaxedTransaction.INSTANCE))).send(p);
								DatabasePlayer.from(UUID.fromString(claim.owner)).getJsonPlayer().queueMessage(
										new MessageBuilder("Purchase Notification").appendVariable(p.getName()).appendCaption("has purchased")
												.appendComponent(itemMap.get(displayItems.get(finalI)).displayName().color(NamedTextColor.YELLOW))
												.appendVariable("x" + amount).appendCaption("for").appendVariable("$" + amount * shop.getPrice())
												.make());
								for (var kp : removeAmounts.entrySet()) {
									rawItems.get(kp.getKey()).setAmount(rawItems.get(kp.getKey()).getAmount() - kp.getValue());
								}
								var finalStack = new ItemStack(itemMap.get(displayItems.get(finalI)));
								finalStack.setAmount(1);
								for (int t = 0; t < amount; t++) {
									p.getInventory().addItem(finalStack);
								}
								
							}, p
					);
				});
			}
		}
		);
		inventoryHelper.show(p);
	}
	
	private static HashMap<Integer, Integer> getRemoveAmounts(ArrayList<ItemStack> items, ItemStack item, int amount) {
		var       amts = new HashMap<Integer, Integer>();
		ItemStack temp = new ItemStack(item);
		temp.setAmount(1);
		int index = 0;
		for (ItemStack stack : items) {
			index++;
			if (stack == null) continue;
			ItemStack temp1 = new ItemStack(stack);
			temp1.setAmount(1);
			if (!( temp1.hashCode() == temp.hashCode() )) continue;
			int amtToRemove = Math.min(stack.getAmount(), amount);
			amts.put(index - 1, amtToRemove);
			amount -= amtToRemove;
			if (amount <= 0) break;
		}
		if (amount > 0) return null;
		return amts;
	}
	
	private static int getQuantity(ArrayList<ItemStack> items, ItemStack item) {
		ItemStack temp = new ItemStack(item);
		temp.setAmount(1);
		
		int amount = 0;
		for (ItemStack stack : items) {
			ItemStack temp1 = new ItemStack(stack);
			temp1.setAmount(1);
			if (!( temp1.hashCode() == temp.hashCode() )) continue;
			amount += stack.getAmount();
		}
		return amount;
	}
}
