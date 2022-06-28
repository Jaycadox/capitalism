package xyz.jayphen.capitalism.helpers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryHelper {
	public static final HashMap<String, Runnable> SPECIAL_ITEMS = new HashMap<>();
	private static final ArrayList<InventoryHelper> INVENTORIES = new ArrayList<>();
	public final HashMap<ItemStack, Runnable> ITEMS = new HashMap<>();
	private final ArrayList<String> inventoryNameStack = new ArrayList<>();
	private Inventory inventory = null;
	private InventoryHelperRunnable onRender = null;
	private String title = null;
	private int marginHorizontal = 0;
	private boolean wasRightClick = false;
	private int marginVertical = 0;
	private ItemStack lastItemAdded = null;
	
	public InventoryHelper(String title, int rows, InventoryHelperRunnable render) {
		this.inventory = Bukkit.createInventory(null, 9 * rows, title);
		inventoryNameStack.add("root");
		INVENTORIES.add(this);
		this.title = title;
		render.run(this, "root");
		this.onRender = render;
	}

	public static void close(Player p) {
		new BukkitRunnable() {
			@Override
			public void run() {
				p.closeInventory();
			}
		}.runTask(Capitalism.plugin);
	}
	
	public static ItemStack getHead(OfflinePlayer player, String name, List<String> lore) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setOwningPlayer(player);
		meta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', name));
		if (lore != null)
			meta.setLore(lore.stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
		item.setItemMeta(meta);
		return item;
	}
	
	public static ItemStack getBase64Head(String b64, String name, List<String> lore, Runnable onClick) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		
		GameProfile profile = new GameProfile(UUID.randomUUID(), "");
		profile.getProperties().put("textures", new Property("textures", b64));
		Field profileField = null;
		try {
			profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		
		meta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', name));
		if (lore != null)
			meta.setLore(lore.stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
		item.setItemMeta(meta);
		SPECIAL_ITEMS.put(name, onClick);
		return item;
	}
	
	public static boolean isInventory(Inventory i) {
		return INVENTORIES.stream().anyMatch(x -> x.inventory.hashCode() == i.hashCode());
	}
	
	public static InventoryHelper getInventory(Inventory i) {
		for (InventoryHelper helper : INVENTORIES) {
			if (helper.inventory.hashCode() == i.hashCode()) return helper;
		}
		return null;
	}
	
	public Inventory getInventory() {
		return inventory;
	}
	
	public boolean isRightClick() {
		return wasRightClick;
	}
	
	public void setRightClick(boolean wasRightClick) {
		this.wasRightClick = wasRightClick;
	}
	
	public void push(String name, int rows) {
		
		this.inventoryNameStack.add(name);
	}
	
	public void pop() {
		this.inventoryNameStack.remove(this.inventoryNameStack.size() - 1);
	}
	
	public boolean in(String name) {
		return this.inventoryNameStack.get(this.inventoryNameStack.size() - 1).equals(name);
	}
	
	public void setMargin(int horiz, int vert) {
		this.marginHorizontal = horiz;
		this.marginVertical = vert;
	}
	
	private ItemStack marginItem() {
		ItemStack iStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = iStack.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "EMPTY");
		iStack.setItemMeta(meta);
		return iStack;
	}
	
	public void reRender(Player p) {
		inventory.clear();
		onRender.run(this, inventoryNameStack.get(inventoryNameStack.size() - 1));
		renderMargin();
		;
		if (inventoryNameStack.size() > 1)
			setItem(0, getColumnCount() - 1 - ( marginHorizontal * 2 ), ChatColor.RED + "" + ChatColor.BOLD + "BACK", Material.COMPASS, this::pop);
		
		p.updateInventory();
	}
	
	private int getRowCount() {
		return inventory.getSize() / getColumnCount();
	}
	
	private int getColumnCount() {
		return 9;
	}
	
	private void renderMargin() {
		for (int row = 0; row < getRowCount(); row++) {
			for (int i = 0; i < marginHorizontal; i++) {
				inventory.setItem(row * getColumnCount() + i, marginItem());
			}
			for (int i = getColumnCount() - 1; i > getColumnCount() - 1 - marginHorizontal; i--) {
				inventory.setItem(row * getColumnCount() + i, marginItem());
			}
		}
		for (int row = 0; row < marginVertical; row++) {
			for (int i = 0; i < getColumnCount(); i++) {
				inventory.setItem(row * getColumnCount() + i, marginItem());
			}
		}
		for (int row = getRowCount() - 1; row > getRowCount() - 1 - marginVertical; row--) {
			for (int i = 0; i < getColumnCount(); i++) {
				inventory.setItem(row * getColumnCount() + i, marginItem());
			}
		}
	}
	
	public void setItem(int row, int column, String name, Material mat, Runnable onClick) {
		setItem(row, column, ChatColor.translateAlternateColorCodes('&', name), mat, onClick, null);
	}
	
	public void setItem(int row, int column, String name, Material mat, Runnable onClick, List<String> lore) {
		ItemStack iStack = new ItemStack(mat);
		ItemMeta meta = iStack.getItemMeta();
		if (lore == null) {
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		} else {
			meta.setLore(lore.stream().map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
		}
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', name));
		iStack.setItemMeta(meta);
		setItem(row, column, iStack, onClick);
	}
	
	public int getPosition(int row, int column) {
		return ( row + marginVertical ) * getColumnCount() + ( column + marginHorizontal );
	}
	
	public void addMargin(int row, int column) {
		setItem(row, column, marginItem(), () -> {});
	}
	
	public void setItem(int row, int column, ItemStack item, Runnable onClick) {
		inventory.setItem(( row + marginVertical ) * 9 + ( column + marginHorizontal ), item);
		ITEMS.put(item, onClick);
		lastItemAdded = item;
	}
	
	public void setItem(int index, ItemStack item, Runnable onClick) {
		inventory.setItem(( marginVertical * 9 ) + index, item);
		ITEMS.put(item, onClick);
		lastItemAdded = item;
	}
	
	public void show(Player p) {
		new BukkitRunnable() {
			@Override
			public void run() {
				reRender(p);
				p.openInventory(inventory);
			}
		}.runTask(Capitalism.plugin);
		
	}
	
	public void show(Player p, String context) {
		if (!INVENTORIES.contains(this)) {
			INVENTORIES.add(this);
		}
		inventoryNameStack.clear();
		inventoryNameStack.add("root");
		inventoryNameStack.add(context);
		new BukkitRunnable() {
			@Override
			public void run() {
				reRender(p);
				p.openInventory(inventory);
			}
		}.runTask(Capitalism.plugin);
		
	}
	
	public void destroy() {
		INVENTORIES.remove(this);
	}
	
	public Runnable fromItem(ItemStack itemStack) {
		Runnable r = () -> {
		};
		if (SPECIAL_ITEMS.containsKey(itemStack.getItemMeta().getDisplayName())) {
			r = SPECIAL_ITEMS.get(itemStack.getItemMeta().getDisplayName());
			SPECIAL_ITEMS.remove(itemStack.getItemMeta().getDisplayName());
			return r;
		}
		
		for (Map.Entry<ItemStack, Runnable> itemStack1 : ITEMS.entrySet()) {
			if (itemStack1.getKey().hashCode() == itemStack.hashCode()) {
				return itemStack1.getValue();
			}
		}
		
		return r;
	}
}
