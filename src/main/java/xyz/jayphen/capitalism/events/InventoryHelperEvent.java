package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import xyz.jayphen.capitalism.helpers.InventoryHelper;

import java.util.HashMap;
import java.util.UUID;

public class InventoryHelperEvent implements Listener {
	
	public static final HashMap<UUID, Boolean> CLICK_TYPES = new HashMap<>();
	
	public static void closeInventories() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (InventoryHelper.isInventory(p.getOpenInventory().getTopInventory())) {
				p.closeInventory();
			}
		}
	}
	
	@EventHandler
	public void onInventoryInteract(InventoryClickEvent event) {
		if (!InventoryHelper.isInventory(event.getInventory())) return;
		event.setCancelled(true);
		if (event.getClickedInventory() == null || !InventoryHelper.isInventory(event.getClickedInventory())) return;
		int       slot = event.getSlot();
		ItemStack item = event.getClickedInventory().getItem(slot);
		CLICK_TYPES.put(event.getWhoClicked().getUniqueId(), event.getClick().isLeftClick());
		if (item == null) return;
		InventoryHelper helper = InventoryHelper.getInventory(event.getClickedInventory());
		try {
			helper.fromItem(item).run();
			
		} catch (Exception e) {
			e.printStackTrace();
			event.setCancelled(true);
			event.getWhoClicked().closeInventory();
		}
		
		
		if (helper == null) return;
		try {
			helper.reRender((Player) event.getWhoClicked());
			
		} catch (Exception e) {
			e.printStackTrace();
			event.setCancelled(true);
			event.getWhoClicked().closeInventory();
		}
		
	}
	
	@EventHandler
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (!InventoryHelper.isInventory(event.getInventory())) return;
		InventoryHelper.getInventory(event.getInventory()).destroy();
		
	}
}
