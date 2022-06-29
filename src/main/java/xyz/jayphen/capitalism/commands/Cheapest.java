package xyz.jayphen.capitalism.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimItemShop;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.helpers.InventoryHelper;
import xyz.jayphen.capitalism.helpers.InventoryHelperRunnable;
import xyz.jayphen.capitalism.helpers.InventoryScroll;
import xyz.jayphen.capitalism.helpers.ShopHelper;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Cheapest implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) return false;
		
		StringBuilder itemSeachBuilder = new StringBuilder();
		for (String arg : args) {
			itemSeachBuilder.append(arg)
					.append(' ');
		}
		String   itemSearch = itemSeachBuilder.toString().trim().replace(" ", "_").toUpperCase();
		Material mat        = Material.matchMaterial(itemSearch);
		if (mat == null) {
			new MessageBuilder("Shop").appendCaption("I can't figure out what you mean by:").appendVariable(itemSearch + ".")
					.appendCaption("Try a different query").send(sender);
			return true;
		}
		if (!( sender instanceof Player p )) return true;
		ItemStack           stack = new ItemStack(mat, 1);
		record ShopInfo(int price, int quantity, Location loc) {}
		ArrayList<ShopInfo> shops = new ArrayList<>();
		for (Claim c : ClaimManager.getAllClaims()) {
			if (c == null) continue;
			for (ClaimItemShop shop : c.getSigns()) {
				if (shop == null) continue;
				var chest = ShopHelper.getChestFromSign(
						new Location(Bukkit.getWorld(c.location.world), shop.getX(), shop.getY(), shop.getZ()));
				if (chest == null) continue;
				ArrayList<ItemStack> rawItems = new ArrayList<>();
				for (int i = 0; i < chest.getInventory().getSize() - 1; i++) {
					rawItems.add(chest.getInventory().getItem(i));
				}
				
				ArrayList<ItemStack> items = rawItems.stream().filter(x -> x != null && x.getType() != Material.AIR)
						.collect(Collectors.toCollection(ArrayList::new));
				int amt = ShopHelper.getQuantity(items, stack);
				if (amt != 0) {
					shops.add(new ShopInfo(shop.getPrice(), amt,
					                       new Location(Bukkit.getWorld(c.location.world), shop.getX(), shop.getY(), shop.getZ())
					));
				}
			}
		}
		if (shops.isEmpty()) {
			new MessageBuilder("Shop").appendCaption("Couldn't find any shops that sells").appendVariable(mat.toString()).send(p);
			return true;
		}
		shops = shops.stream().sorted(Comparator.comparingInt(x -> x.price)).collect(Collectors.toCollection(ArrayList::new));
		ArrayList<InventoryScroll.ItemRunnable> itemStacks = shops.stream().map(x -> {
			ItemStack dStack = new ItemStack(mat, x.quantity);
			dStack.setAmount(x.quantity);
			var       meta   = dStack.getItemMeta();
			meta.displayName(Component.text("$" + x.price + "/item", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
					                 .decorate(TextDecoration.BOLD));
			meta.lore(List.of(Component.empty(), MiniMessage.miniMessage().deserialize("<reset><yellow>" + x.quantity + " in stock")
					                  .decoration(TextDecoration.ITALIC, false),
			                  MiniMessage.miniMessage().deserialize("<reset><green>World: <yellow>" + x.loc.getWorld().getName())
					                  .decoration(TextDecoration.ITALIC, false), MiniMessage.miniMessage().deserialize(
									"<reset><green>Location: <yellow>" + x.loc.getX() + ", " + x.loc.getY() + ", " + x.loc.getZ())
					                  .decoration(TextDecoration.ITALIC, false)
			));
			dStack.setItemMeta(meta);
			return new InventoryScroll.ItemRunnable(dStack, () -> {});
		}).collect(Collectors.toCollection(ArrayList::new));
		InventoryScroll[] scroll = { null };
		
		InventoryHelperRunnable r = (inv, submenu) -> {
			if (scroll[0] == null) {
				scroll[0] = new InventoryScroll(0, 9, inv, itemStacks, "root", p.getUniqueId());
			}
			scroll[0].render();
		};
		InventoryHelper helper = new InventoryHelper("Cheapest shops for: " + mat, 1, r);
		helper.show(p);
		return true;
	}
}
