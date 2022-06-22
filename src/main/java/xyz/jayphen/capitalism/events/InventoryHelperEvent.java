package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import xyz.jayphen.capitalism.helpers.InventoryHelper;

public class InventoryHelperEvent implements Listener {
	@EventHandler
	public void onInventoryInteract(InventoryClickEvent event) {
		if(!InventoryHelper.isInventory(event.getInventory())) return;
		event.setCancelled(true);
		if(event.getClickedInventory() == null || !InventoryHelper.isInventory(event.getClickedInventory())) return;
		int slot = event.getSlot();
		ItemStack item = event.getClickedInventory().getItem(slot);
		if(item == null) return;
		InventoryHelper.fromItem(item).run();
		InventoryHelper helper = InventoryHelper.getInventory(event.getClickedInventory());
		if(helper == null) return;
		helper.reRender((Player) event.getWhoClicked());
	}

	@EventHandler
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if(!InventoryHelper.isInventory(event.getInventory())) return;
		InventoryHelper.getInventory(event.getInventory()).destroy();

	}

	public static void closeInventories() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(InventoryHelper.isInventory(p.getOpenInventory().getTopInventory())) {
				p.closeInventory();
			}
		}
	}
}
