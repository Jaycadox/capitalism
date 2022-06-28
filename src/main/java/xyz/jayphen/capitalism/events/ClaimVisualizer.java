package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;

public class ClaimVisualizer {
	public static void register() {
		new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(Capitalism.plugin, 0, 4);
	}
	
	private static void tick() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			for (Claim claim : DatabasePlayer.from(p).getJsonPlayer().getData().claims) {
				if (claim == null) continue;
				if (!claim.owner.equals(p.getUniqueId().toString())) continue;
				if (!claim.getPermissions().visualize) continue;
				new BukkitRunnable() {
					@Override
					public void run() {
						for (Location loc : claim.getBorderBlocks()) {
							for (int i = p.getLocation().getBlockY() - 2; i < p.getLocation().getBlockY() + 2; i++) {
								p.spawnParticle(Particle.REDSTONE, loc.getBlockX() + 0.5, i + 0.1, loc.getBlockZ() + 0.5, 1,
								                new Particle.DustOptions(Color.fromBGR(0, 0, 255), 1)
								);
							}
						}
					}
				}.runTaskAsynchronously(Capitalism.plugin);
			}
			
		}
	}
	
}
