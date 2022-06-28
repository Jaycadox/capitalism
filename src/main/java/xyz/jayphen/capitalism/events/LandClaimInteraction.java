package xyz.jayphen.capitalism.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimItemShop;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.claims.region.RegionManager;
import xyz.jayphen.capitalism.database.Database;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.helpers.ShopHelper;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.*;
import java.util.stream.Collectors;

public class LandClaimInteraction implements Listener {
	private static final List<Material> WOODEN_DOORS = Arrays.asList(Material.DARK_OAK_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR,
	                                                                 Material.OAK_DOOR, Material.JUNGLE_DOOR, Material.CRIMSON_DOOR,
	                                                                 Material.SPRUCE_DOOR, Material.WARPED_DOOR
	);
	
	private static final List<Material> WOODEN_TRAPDOORS = Arrays.asList(Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.CRIMSON_TRAPDOOR,
	                                                                     Material.JUNGLE_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
	                                                                     Material.SPRUCE_TRAPDOOR, Material.OAK_TRAPDOOR, Material.WARPED_TRAPDOOR
	);
	int                 tntCount                 = 0;
	long                lastReset                = 0;
	ArrayList<Location> knownUnclaimedTNTEmiters = new ArrayList<>();
	
	public static void monitorSignLoop() {
		for (Claim c : ClaimManager.getAllClaims()) {
			if (c == null) continue;
			c = DatabasePlayer.from(UUID.fromString(c.owner)).getJsonPlayer().getClaim(c);
			if (c == null) continue;
			for (ClaimItemShop shop : c.getSigns()) {
				Location loc = new Location(Bukkit.getWorld(c.location.world), shop.getX(), shop.getY(), shop.getZ());
				if (!( loc.getBlock().getState() instanceof Sign )) {
					Claim finalC = c;
					c.signs = c.getSigns().stream().filter(x -> !x.equals(finalC.getShopFromCoords(shop.getX(), shop.getY(), shop.getZ())))
							.collect(Collectors.toCollection(ArrayList::new));
					DatabasePlayer.from(UUID.fromString(c.owner)).getJsonPlayer().save();
					DatabasePlayer.from(UUID.fromString(c.owner)).getJsonPlayer()
							.queueMessage(new MessageBuilder("Shop").appendCaption("One of the signs in your shop has been broken").make());
				}
			}
		}
	}
	
	@EventHandler
	public void onSignEdit(SignChangeEvent event) {
		Claim c = DatabasePlayer.from(event.getPlayer()).getJsonPlayer()
				.getClaim(ClaimManager.getCachedClaim(event.getBlock().getLocation()).orElse(null));
		if (c == null) return;
		if (c.getRegion() != RegionManager.Region.COMMERCIAL) return;
		new BukkitRunnable() {
			@Override
			public void run() {
				Inventory i = ShopHelper.getInventoryFromSign(event.getBlock().getLocation());
				if (i == null) return;
				int price = ShopHelper.getPriceFromSign(event.getBlock().getLocation());
				if (price == 0) return;
				c.getSigns();
				c.signs.add(new ClaimItemShop(price, event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));
				DatabasePlayer.from(UUID.fromString(c.owner)).getJsonPlayer().save();
				DatabasePlayer.from(UUID.fromString(c.owner)).getJsonPlayer().queueMessage(
						new MessageBuilder("Shop").appendCaption("A sign has been registered for")
								.appendVariable("$" + NumberFormatter.addCommas(price)).make());
			}
		}.runTaskLater(Capitalism.plugin, 1);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerBedInteraction(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		int numberOfClaims = DatabasePlayer.from(event.getPlayer()).getJsonPlayer().getData().claims.size();
		if (numberOfClaims == 0) return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!( event.getClickedBlock().getBlockData() instanceof Bed )) return;
		Claim c = ClaimManager.getCachedClaim(event.getClickedBlock().getLocation()).orElse(null);
		if (!c.owner.equals(event.getPlayer().getUniqueId().toString())) return;
		if (PlaytimeRewards.redeemedPlayers.contains(event.getPlayer().getUniqueId())) return;
		if (!PlaytimeRewards.eligiblePlayers.contains(event.getPlayer().getUniqueId())) return;
		PlaytimeRewards.redeemedPlayers.add(event.getPlayer().getUniqueId());
		PlaytimeRewards.eligiblePlayers.remove(event.getPlayer().getUniqueId());
		if (Database.injector.inject(event.getPlayer().getUniqueId(), 100000).getType() == TransactionResult.TransactionResultType.SUCCESS) {
			new MessageBuilder("Reward").appendVariable("$100,000")
					.appendCaption("has been added to your balance").send(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onInteraction(PlayerInteractEvent event) {
		if (( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ) && event.getPlayer().isSneaking()) {
			if (event.getItem() != null && event.getItem().getType().isEdible()) return;
		}
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(
				( event.getClickedBlock() != null ) ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation());
		
		if (optClaim.isEmpty()) {
			return;
		}
		Claim claim = optClaim.get();
		if (!claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.GENERAL)) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (event.getClickedBlock().getState() instanceof Chest) {
					for (ClaimItemShop shop : Objects.requireNonNull(ClaimManager.getDatabaseClaim(claim)).getSigns()) {
						if (ShopHelper.getChestFromSign(claim.getSignLocation(shop)) != null) {
							event.setCancelled(true);
							ShopHelper.openShop(event.getPlayer(), shop, (Chest) event.getClickedBlock().getState(), claim);
							return;
						}
					}
				}
			}
		}
		
		var region = RegionManager.getRegion(new Location(Bukkit.getWorld(claim.location.world), claim.location.startX, 0, claim.location.startZ));
		boolean wasBedClicked = region == RegionManager.Region.COMMERCIAL &&
		                        ( event.getClickedBlock() != null && event.getClickedBlock().getBlockData() instanceof Bed ) &&
		                        event.getAction() == Action.RIGHT_CLICK_BLOCK;
		if (wasBedClicked && claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.GENERAL)) {
			Capitalism.ADVENTURE.player(event.getPlayer())
					.sendActionBar(Component.text("Beds cannot be used in commercial plots of land", NamedTextColor.GRAY));
			event.setCancelled(true);
			return;
		}
		
		if (claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.GENERAL) && !wasBedClicked) return;
		if (claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.OWNER) && !wasBedClicked) return;
		if (event.getClickedBlock() != null &&
		    ( WOODEN_DOORS.contains(event.getClickedBlock().getType()) || WOODEN_TRAPDOORS.contains(event.getClickedBlock().getType()) ))
		{
			if (claim.hasPermission(event.getPlayer(), Claim.ClaimInteractionType.WOOD) && !wasBedClicked) return;
		}
		
		event.setCancelled(true);
		event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
				ChatColor.GRAY + "Interaction blocked as the land is claimed by: " + ChatColor.YELLOW +
				Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName()));
	}
	
	@EventHandler
	public void liquidInteraction(BlockFromToEvent event) {
		Block block = event.getBlock();
		if (block.getType() != Material.WATER && block.getType() != Material.LAVA) return;
		Location loc = event.getToBlock().getLocation();
		if (!searchForSource(loc)) {
			event.setCancelled(true);
			clearStream(loc, 0);
		}
		
	}
	
	private boolean searchForSource(Location loc) {
		boolean inClaimedLand = isInClaimedLand(loc);
		for (int i = -4; i < 4; i++) {
			for (int j = -4; j < 4; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if (localLocation.getBlock().getType() != Material.WATER && localLocation.getBlock().getType() != Material.LAVA) continue;
				boolean localIsInClaimedLand = isInClaimedLand(localLocation);
				if (localIsInClaimedLand != inClaimedLand) {
					return false;
				}
			}
		}
		return true;
	}
	
	@EventHandler
	public void pistonExtendEvent(BlockPistonExtendEvent event) {
		Location loc           = event.getBlock().getLocation();
		boolean  inClaimedLand = isInClaimedLand(loc);
		
		for (Block b : event.getBlocks()) {
			loc = b.getLocation();
			for (int i = -3; i < 3; i++) {
				for (int j = -3; j < 3; j++) {
					Location localLocation        = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
					boolean  localIsInClaimedLand = isInClaimedLand(localLocation);
					if (localIsInClaimedLand != inClaimedLand) {
						event.setCancelled(true);
						return;
					}
					
				}
			}
		}
	}
	
	@EventHandler
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (!blockEventHandler(event, event.getPlayer())) {
			return;
		}
		event.setCancelled(true);
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(( event.getBlock().getLocation() ));
		String claimOwner = optClaim.isPresent() ? Bukkit.getOfflinePlayer(UUID.fromString(optClaim.get().owner)).getName() : "a nearby claim border";
		
		event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
				ChatColor.GRAY + "Interaction blocked as the land is claimed by: " + ChatColor.YELLOW + claimOwner));
	}
	
	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {
		if (!blockEventHandler(event, event.getPlayer())) {
			return;
		}
		event.setCancelled(true);
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(( event.getBlock().getLocation() ));
		
		String claimOwner = optClaim.isPresent() ? Bukkit.getOfflinePlayer(UUID.fromString(optClaim.get().owner)).getName() : "a nearby claim border";
		
		event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
				ChatColor.GRAY + "Interaction blocked as the land is claimed by: " + ChatColor.YELLOW + claimOwner));
	}
	
	private boolean blockEventHandler(BlockEvent event, Player p) {
		Location loc = event.getBlock().getLocation();
		if (isInClaimedLandAndNotTheOwner(loc, p)) {
			return true;
		}
		boolean inClaimedLand = isInClaimedLandAndNotTheOwner(loc, p);
		if (inClaimedLand) return false;
		for (int i = -3; i < 3; i++) {
			for (int j = -3; j < 3; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if (isInClaimedLandAndNotTheOwner(localLocation, p)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void clearStream(Location loc, int depth) {
		loc.getBlock().setType(Material.AIR);
		
		for (int i = -1; i < 1; i++) {
			for (int j = -1; j < 1; j++) {
				Location localLocation = new Location(loc.getWorld(), loc.getX() + i, loc.getY(), loc.getBlockZ() + j);
				if (localLocation.getBlock().getType() != Material.WATER && localLocation.getBlock().getType() != Material.LAVA) continue;
				localLocation.getBlock().setType(Material.AIR);
				clearStream(localLocation, depth + 1);
				
			}
		}
		
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onTntExplosion(EntityExplodeEvent event) {
		if (System.currentTimeMillis() - lastReset > 1000) {
			tntCount  = 0;
			lastReset = System.currentTimeMillis();
		}
		if (++tntCount > 20) {
			event.setCancelled(true);
		}
		
		int blocksInLandClaim = 0, blocksUnclaimed = 0;
		
		for (Block b : event.blockList()) {
			if (isInClaimedLand(b.getLocation())) {
				blocksInLandClaim++;
			} else {
				blocksUnclaimed++;
			}
		}
		boolean nearEmiter = false;
		for (Location emit : knownUnclaimedTNTEmiters) {
			if (emit.distance(event.getLocation()) < 40) {
				nearEmiter = true;
				break;
			}
		}
		
		if (!( blocksInLandClaim > blocksUnclaimed )) {
			knownUnclaimedTNTEmiters.add(event.getLocation());
			new BukkitRunnable() {
				@Override
				public void run() {
					knownUnclaimedTNTEmiters.removeIf(x -> x.hashCode() == event.getLocation().hashCode());
				}
			}.runTaskLater(Capitalism.plugin, 120);
		}
		
		boolean inLandClaim = ( blocksInLandClaim > blocksUnclaimed ) && !nearEmiter && isInClaimedLand(event.getLocation());
		
		for (Block b : event.blockList()) {
			if (!inLandClaim && isInClaimedLand(b.getLocation())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onTntExplosion(BlockExplodeEvent event) {
		if (System.currentTimeMillis() - lastReset > 1000) {
			tntCount  = 0;
			lastReset = System.currentTimeMillis();
		}
		if (++tntCount > 20) {
			event.setCancelled(true);
		}
		
		int blocksInLandClaim = 0, blocksUnclaimed = 0;
		
		for (Block b : event.blockList()) {
			if (isInClaimedLand(b.getLocation())) {
				blocksInLandClaim++;
			} else {
				blocksUnclaimed++;
			}
		}
		boolean nearEmiter = false;
		for (Location emit : knownUnclaimedTNTEmiters) {
			if (emit.distance(event.getBlock().getLocation()) < 40) {
				nearEmiter = true;
				break;
			}
		}
		
		if (!( blocksInLandClaim > blocksUnclaimed )) {
			knownUnclaimedTNTEmiters.add(event.getBlock().getLocation());
			new BukkitRunnable() {
				@Override
				public void run() {
					knownUnclaimedTNTEmiters.removeIf(x -> x.hashCode() == event.getBlock().getLocation().hashCode());
				}
			}.runTaskLater(Capitalism.plugin, 120);
		}
		
		boolean inLandClaim = ( blocksInLandClaim > blocksUnclaimed ) && !nearEmiter && isInClaimedLand(event.getBlock().getLocation());
		
		for (Block b : event.blockList()) {
			if (!inLandClaim && isInClaimedLand(b.getLocation())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) return;
		if (e.getEntity() instanceof Villager && e.getDamager() instanceof Zombie) return;
		if (isInClaimedLandAndNotTheOwner(e.getEntity().getLocation(), e.getDamager())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (isInClaimedLandAndNotTheOwner(event.getTo(), event.getPlayer())) {
			event.setCanCreatePortal(false);
			event.setTo(new Location(event.getFrom().getWorld(), 0, 0, 0));
		}
	}
	
	@EventHandler
	public void growEvent(StructureGrowEvent event) {
		Location lowestLocation = new Location(event.getWorld(), 0, 999, 0);
		for (BlockState bs : event.getBlocks()) {
			if (bs.getLocation().getY() < lowestLocation.getY()) {
				lowestLocation = bs.getLocation();
			}
		}
		Optional<Claim> optSaplingClaim = ClaimManager.getCachedClaim(lowestLocation);
		boolean         saplingInClaim  = optSaplingClaim.isPresent();
		for (BlockState bs : event.getBlocks()) {
			Optional<Claim> optBlockClaim = ClaimManager.getCachedClaim(bs.getLocation());
			if (saplingInClaim == optBlockClaim.isPresent()) continue;
			bs.setType(event.getWorld().getType(bs.getLocation()));
		}
	}
	
	boolean isInClaimedLand(Location loc) {
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(loc);
		return optClaim.isPresent();
	}
	
	boolean isInClaimedLandAndNotTheOwner(Location loc, Entity p) {
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(loc);
		if (optClaim.isPresent() && optClaim.get().owner.equals(p.getUniqueId().toString())) return false;
		if (optClaim.isPresent() && p instanceof Player && optClaim.get().hasPermission((Player) p, Claim.ClaimInteractionType.GENERAL)) {
			return false;
		}
		return optClaim.isPresent();
	}
}