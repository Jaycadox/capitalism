package xyz.jayphen.capitalism.helpers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InventoryScroll {

	InventoryHelper inventory = null;
	int startRow = 0;
	int size = 0;
	int startPos = 0;

	int viewStart = 0;
	int viewEnd = 0;
	public record ItemRunnable(ItemStack stack, Runnable runnable) {};
	List<ItemRunnable> entireView;
	public static final ArrayList<InventoryScroll> INSTANCES = new ArrayList<>();

	public static InventoryScroll get(int column, int size, InventoryHelper inv, List<ItemRunnable> items) {
		for(InventoryScroll inst : INSTANCES) {
			if(inst.hashCode() == Objects.hash(items)) return inst;
		}
		InventoryScroll scroll = new InventoryScroll(column, size, inv, items);
		return scroll;
	}

	public InventoryScroll(int column, int size, InventoryHelper inv, List<ItemRunnable> items) {
		inventory = inv;
		this.size = size;
		if(size < items.size()) {
			this.size -= 2;
		}
		startPos = column;
		entireView = items;
		viewEnd = this.size;
		INSTANCES.add(this);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InventoryScroll that = (InventoryScroll) o;
		return startRow == that.startRow && size == that.size && startPos == that.startPos && Objects.equals(inventory, that.inventory) && Objects.equals(entireView, that.entireView);
	}

	@Override
	public int hashCode () {
		return Objects.hash(entireView);
	}

	public void scrollRight() {
		if(viewEnd == entireView.size()) return;
		viewStart++;
		viewEnd++;
	}
	public void scrollLeft() {
		if(viewStart == 0) return;
		viewStart--;
		viewEnd--;

	}

	public int getSuggestedRow () {
		return suggestedRow;
	}

	int suggestedRow = 0;

	public int getSuggestedCol () {
		return suggestedCol;
	}

	int suggestedCol = 0;

	public void render() {
		if(entireView.size() == 0) {
			inventory.setItem(startRow, startPos, ChatColor.YELLOW + "Empty list", Material.OAK_SIGN, ()->{});
			return;
		}


		int col = startPos;
		for(int i = 0; i <= size; i++) {
			if(i == entireView.size()) break;
			inventory.setItem(startRow, col, entireView.get(i).stack, entireView.get(i).runnable);
			col++;
		}
		suggestedRow = startRow;
		suggestedCol = col;

		if(!(size < entireView.size())) return;

		if(viewStart != 0)
			inventory.setItem(getSuggestedRow(), getSuggestedCol(),
			            InventoryHelper.getBase64Head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=",
			                                          org.bukkit.ChatColor.YELLOW + "" + ChatColor.BOLD + "LEFT", null, this::scrollLeft), ()->{}
			);
		else {
			inventory.addMargin(suggestedRow, suggestedCol);
		}
		if(viewEnd != entireView.size())
			inventory.setItem(getSuggestedRow(), getSuggestedCol() + 1,
			            InventoryHelper.getBase64Head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=",
			                                          org.bukkit.ChatColor.YELLOW + "" + ChatColor.BOLD + "RIGHT", null, this::scrollRight), ()->{}
			);
		else {
			inventory.addMargin(suggestedRow, suggestedCol + 1);
		}
	}
}
