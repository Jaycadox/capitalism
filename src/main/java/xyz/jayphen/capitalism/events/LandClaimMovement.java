package xyz.jayphen.capitalism.events;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.claims.region.RegionManager;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class LandClaimMovement implements Listener {
	private static final HashMap<UUID, UUID> insideLandClaim = new HashMap<>();
	private static final HashMap<UUID, RegionManager.Region> insideRegion = new HashMap<>();
	private static long count = 0;
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (count++ % 5 != 0) {
			return;
		}
		visualize(event, new PlayerRunnable() {
			@Override
			public void onEnterLand(Player enteredPlayer, Claim claim) {
				if (!claim.hasPermission(enteredPlayer, Claim.ClaimInteractionType.GENERAL)) return;
				if (DatabasePlayer.from(enteredPlayer).getJsonPlayer().getData().seenLandlordTip) return;
				DatabasePlayer.from(enteredPlayer).getJsonPlayer().getData().seenLandlordTip = true;
				DatabasePlayer.from(enteredPlayer).getJsonPlayer().save();
				
				new MessageBuilder("Tip").appendCaption("You can type").appendVariable("/land").appendCaption("whilst standing inside your land claim to open the Landlord Settings menu").send(enteredPlayer);
			}
		});
		
		
	}
	
	private void visualize(PlayerMoveEvent event, PlayerRunnable onEnter) {
		Optional<Claim> optClaim = ClaimManager.getCachedClaim(event.getPlayer().getLocation());
		RegionManager.Region reg = RegionManager.getRegion(event.getPlayer().getLocation());
		if (insideRegion.isEmpty() || insideRegion.get(event.getPlayer().getUniqueId()) != reg) {
			insideRegion.put(event.getPlayer().getUniqueId(), reg);
			event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "You are now in the " + ChatColor.YELLOW + reg.toString().toLowerCase(Locale.ROOT) + ChatColor.GRAY + " region."));
		}
		if (optClaim.isEmpty()) {
			if (insideLandClaim.containsKey(event.getPlayer().getUniqueId())) {
				insideLandClaim.remove(event.getPlayer().getUniqueId());
				event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "You are now in unclaimed land"));
			}
			return;
		}
		if (!insideLandClaim.containsKey(event.getPlayer().getUniqueId())) {
			Claim claim = optClaim.get();
			insideLandClaim.put(event.getPlayer().getUniqueId(), UUID.fromString(claim.owner));
			onEnter.onEnterLand(event.getPlayer(), claim);
			event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Land owned by: " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName()));
			new BukkitRunnable() {
				@Override
				public void run() {
					for (Location loc : claim.getBorderBlocks()) {
						for (int i = event.getPlayer().getLocation().getBlockY(); i < event.getPlayer().getLocation().getBlockY() + 40; i++) {
							event.getPlayer().spawnParticle(Particle.REDSTONE, loc.getBlockX() + 0.5, i + 0.1, loc.getBlockZ() + 0.5, 1, new Particle.DustOptions(Color.fromBGR(0, 0, 255), 1));
						}
					}
				}
			}.runTaskAsynchronously(Capitalism.plugin);
			
			
		}
	}
}

abstract class PlayerRunnable {
	public abstract void onEnterLand(Player enteredPlayer, Claim land);
}
