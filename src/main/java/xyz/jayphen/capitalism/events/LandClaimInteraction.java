package xyz.jayphen.capitalism.events;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;

import java.util.*;

public class LandClaimInteraction implements Listener {
	private static final List<Material> WOODEN_DOORS = Arrays.asList(
			Material.DARK_OAK_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.OAK_DOOR, Material.JUNGLE_DOOR, Material.CRIMSON_DOOR, Material.SPRUCE_DOOR, Material.WARPED_DOOR
	);

	private static final List<Material> WOODEN_TRAPDOORS = Arrays.asList(
			Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.OAK_TRAPDOOR, Material.WARPED_TRAPDOOR
	);

	@EventHandler
	public void onInteraction(PlayerInteractEvent event) {
		if((event.getAction() == Action.RIGHT_CLICK_AIR ||
				event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getPlayer().isSneaking()) {
			if(event.getItem() != null && event.getItem().getType().isEdible()) return;
		}

		Optional<Claim> optClaim =
				ClaimManager.getCachedClaim((event.getClickedBlock() != null) ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation());

		if(optClaim.isEmpty()) {
			return;
		}
		Claim claim = optClaim.get();
		if(claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.GENERAL)) return;
		if(event.getClickedBlock() != null && (WOODEN_DOORS.contains(event.getClickedBlock().getType()) || WOODEN_TRAPDOORS.contains(event.getClickedBlock().getType()))) {
			if(claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.WOOD)) return;
		}

		event.setCancelled(true);
		event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
				ChatColor.GRAY + "Interaction blocked as the land is claimed by: "
				+ ChatColor.YELLOW + Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName()
		));
	}


	@EventHandler
	public void liquidInteraction(BlockFromToEvent event) {
		Block block = event.getBlock();
		if (block.getType() != Material.WATER && block.getType() != Material.LAVA) return;
		Location loc = event.getToBlock().getLocation();
		if(!searchForSource(loc)) {
			event.setCancelled(true);
			clearStream(loc, 0);
		}

	}

	private boolean searchForSource (Location loc) {
		boolean inClaimedLand = isInClaimedLand(loc);
		for(int i = -4; i < 4; i++) {
			for(int j = -4; j < 4; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if(localLocation.getBlock().getType() != Material.WATER && localLocation.getBlock().getType() != Material.LAVA) continue;
				boolean localIsInClaimedLand = isInClaimedLand(localLocation);
				if(localIsInClaimedLand != inClaimedLand) {
					return false;
				}
			}
		}
		return true;
	}
	@EventHandler
	public void pistonExtendEvent(BlockPistonExtendEvent event) {
		Location loc = event.getBlock().getLocation();
		boolean inClaimedLand = isInClaimedLand(loc);

		for(Block b : event.getBlocks()) {
			loc = b.getLocation();
			for(int i = -3; i < 3; i++) {
				for(int j = -3; j < 3; j++) {
					Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
					boolean localIsInClaimedLand = isInClaimedLand(localLocation);
					if(localIsInClaimedLand != inClaimedLand) {
						event.setCancelled(true);
						return;
					}

				}
			}
		}
	}

	@EventHandler
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (blockEventHandler(event, event.getPlayer())) {
			event.setCancelled(true);
			Optional<Claim> optClaim = ClaimManager.getCachedClaim((event.getBlock().getLocation()));
			String claimOwner = optClaim.isPresent() ? Bukkit.getOfflinePlayer(UUID.fromString(optClaim.get().owner)).getName() :
					"a nearby claim border";

			event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
					ChatColor.GRAY + "Interaction blocked as the land is claimed by: "
							+ ChatColor.YELLOW + claimOwner));
		}
	}


	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {
		if (blockEventHandler(event, event.getPlayer())) {
			event.setCancelled(true);
			Optional<Claim> optClaim = ClaimManager.getCachedClaim((event.getBlock().getLocation()));

			String claimOwner = optClaim.isPresent() ? Bukkit.getOfflinePlayer(UUID.fromString(optClaim.get().owner)).getName() :
					"a nearby claim border";

			event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
					ChatColor.GRAY + "Interaction blocked as the land is claimed by: "
							+ ChatColor.YELLOW + claimOwner));
		}
	}
	private boolean blockEventHandler (BlockEvent event, Player p) {
		Location loc = event.getBlock().getLocation();
		if(isInClaimedLandAndNotTheOwner(loc, p)) {
			return true;
		}
		boolean inClaimedLand = isInClaimedLand(loc);
		if(inClaimedLand) return false;
		for(int i = -3; i < 3; i++) {
			for(int j = -3; j < 3; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if(isInClaimedLandAndNotTheOwner(localLocation, p)) {
					return true;
				}
			}
		}
		return false;
	}


	private void clearStream(Location loc, int depth) {
		loc.getBlock().setType(Material.AIR);

		for(int i = -1; i < 1; i++) {
			for(int j = -1; j < 1; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if(localLocation.getBlock().getType() != Material.WATER && localLocation.getBlock().getType() != Material.LAVA) continue;
				localLocation.getBlock().setType(Material.AIR);
				clearStream(localLocation, depth + 1);

			}
		}

	}
	int tntCount = 0;
	long lastReset = 0;

	ArrayList<Location> knownUnclaimedTNTEmiters = new ArrayList<>();

	@EventHandler(priority = EventPriority.NORMAL)
	public void onTntExplosion(EntityExplodeEvent event) {
		if(System.currentTimeMillis() - lastReset > 1000) {
			tntCount = 0;
			lastReset = System.currentTimeMillis();
		}
		if(++tntCount > 20) {
			event.setCancelled(true);
		}

		int blocksInLandClaim = 0, blocksUnclaimed = 0;

		for(Block b : event.blockList()) {
			if(isInClaimedLand(b.getLocation())) {
				blocksInLandClaim++;
			} else {
				blocksUnclaimed++;
			}
		}
		boolean nearEmiter = false;
		for(Location emit : knownUnclaimedTNTEmiters) {
			if(emit.distance(event.getLocation()) < 20) {
				nearEmiter = true;
				break;
			}
		}

		if(!(blocksInLandClaim > blocksUnclaimed)) {
			knownUnclaimedTNTEmiters.add(event.getLocation());
			new BukkitRunnable() {
				@Override
				public void run () {
					knownUnclaimedTNTEmiters.removeIf(x -> x.hashCode() == event.getLocation().hashCode());
				}
			}.runTaskLater(Capitalism.plugin, 60);
		}

		boolean inLandClaim = (blocksInLandClaim > blocksUnclaimed) && !nearEmiter && isInClaimedLand(event.getLocation());

		for(Block b : event.blockList()) {
			if(!inLandClaim && isInClaimedLand(b.getLocation())) {
				event.setCancelled(true);
			}
		}
	}



	@EventHandler
	public void growEvent(StructureGrowEvent event) {
		Location lowestLocation = new Location(event.getWorld(), 0, 999, 0);
		for(BlockState bs : event.getBlocks()) {
			if(bs.getLocation().getY() < lowestLocation.getY()) {
				lowestLocation = bs.getLocation();
			}
		}
		Optional<Claim> optSaplingClaim = ClaimManager.getCachedClaim(lowestLocation);
		boolean saplingInClaim = optSaplingClaim.isPresent();
		for(BlockState bs : event.getBlocks()) {
			Optional<Claim> optBlockClaim = ClaimManager.getCachedClaim(bs.getLocation());
			if(saplingInClaim == optBlockClaim.isPresent()) continue;
			bs.setType(event.getWorld().getType(bs.getLocation()));
		}
	}
	boolean isInClaimedLand(Location loc) {
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(loc);
		return optClaim.isPresent();
	}

	boolean isInClaimedLandAndNotTheOwner(Location loc, Player p) {
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(loc);
		if(optClaim.isPresent() && !optClaim.get().hasPermission(p, Claim.ClaimInteractionType.GENERAL)) {
			return false;
		}
		return optClaim.isPresent();
	}
}