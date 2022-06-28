package xyz.jayphen.capitalism.helpers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InventoryScroll {
		
		public String contex;
		public UUID   player;
		InventoryHelper inventory = null;
		int             startRow  = 0;
		int             size      = 0;
		int             startPos  = 0;
		int             ogSize    = 0;
		int             viewStart = 0;
		
		;
		int                viewEnd      = 0;
		List<ItemRunnable> entireView;
		int                suggestedRow = 0;
		int                suggestedCol = 0;
		
		
		public InventoryScroll(int column, int size, InventoryHelper inv, List<ItemRunnable> items, String context, UUID player) {
				
				inventory   = inv;
				this.ogSize = size;
				this.size   = size;
				if (size < items.size()) {
						this.size -= 2;
				}
				startPos    = column;
				entireView  = items;
				viewEnd     = this.size;
				this.contex = context;
				this.player = player;
		}
		
		public void setView(List<ItemRunnable> entireView) {
				this.entireView = entireView;
				this.size       = ogSize;
				if (size < entireView.size()) {
						this.size -= 2;
				}
				viewEnd   = this.size;
				viewStart = 0;
		}
		
		@Override
		public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				InventoryScroll that = (InventoryScroll) o;
				return startRow == that.startRow && size == that.size && startPos == that.startPos && Objects.equals(inventory, that.inventory) &&
				       Objects.equals(entireView, that.entireView);
		}
		
		@Override
		public int hashCode() {
				return Objects.hash(entireView, contex, player);
		}
		
		public void scrollRight() {
				if (viewEnd == entireView.size()) return;
				viewStart++;
				viewEnd++;
		}
		
		public void scrollLeft() {
				if (viewStart == 0) return;
				viewStart--;
				viewEnd--;
				
		}
		
		public int getSuggestedRow() {
				return suggestedRow;
		}
		
		public int getSuggestedCol() {
				return suggestedCol;
		}
		
		public void render() {
				if (entireView.isEmpty()) {
						inventory.setItem(startRow, startPos, ChatColor.YELLOW + "Empty list", Material.OAK_SIGN, () -> {});
						return;
				}
				
				
				int col = startPos;
				for (int i = viewStart; i < viewEnd; i++) {
						if (i == entireView.size()) break;
						inventory.setItem(startRow, col, entireView.get(i).stack, entireView.get(i).runnable);
						col++;
				}
				suggestedRow = startRow;
				suggestedCol = col;
				
				if (size >= entireView.size()) return;
				
				if (viewStart != 0) inventory.setItem(getSuggestedRow(), getSuggestedCol(), InventoryHelper.getBase64Head(
								"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=",
								org.bukkit.ChatColor.YELLOW + "" + ChatColor.BOLD + "LEFT", null, this::scrollLeft
				), () -> {});
				else {
						inventory.addMargin(suggestedRow, suggestedCol);
				}
				if (viewEnd != entireView.size()) inventory.setItem(getSuggestedRow(), getSuggestedCol() + 1, InventoryHelper.getBase64Head(
								"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=",
								org.bukkit.ChatColor.YELLOW + "" + ChatColor.BOLD + "RIGHT", null, this::scrollRight
				), () -> {});
				else {
						inventory.addMargin(suggestedRow, suggestedCol + 1);
				}
		}
		
		public record ItemRunnable(ItemStack stack, Runnable runnable) {}
}
