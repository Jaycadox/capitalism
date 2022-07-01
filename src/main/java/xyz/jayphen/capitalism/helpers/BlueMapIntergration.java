package xyz.jayphen.capitalism.helpers;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import de.bluecolored.bluemap.api.marker.Shape;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;

import java.awt.*;
import java.io.IOException;

public class BlueMapIntergration {
	MarkerAPI markerAPI = null;
	
	public BlueMapIntergration() {
		BlueMapAPI.onEnable(api -> {
			Capitalism.LOG.info("Integrating Capitalism Claims with BlueMap...");
			register();
		});
	}
	
	private void register() {
		BlueMapAPI.getInstance().ifPresent(api -> {
			try {
				markerAPI = api.getMarkerAPI();
				markerAPI.load();
				Capitalism.LOG.info("Running BlueMap land claim display...");
				new BukkitRunnable() {
					@Override
					public void run() {
						markerAPI.getMarkerSets().forEach(x -> markerAPI.removeMarkerSet(x.getId()));
						try {
							markerAPI.save();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						try {
							for (Claim c : ClaimManager.getAllClaims()) {
								if (c == null) continue;
								tick(c, api);
							}
						} catch(Exception ignored) {}

					}
				}.runTaskTimerAsynchronously(Capitalism.plugin, 0, 80);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private void tick(Claim c, BlueMapAPI api) {
		var map = api.getWorlds().stream()
				.filter(x -> c.location.world.toLowerCase().contains(x.getMaps().stream().findFirst().get().getName().toLowerCase()))
				.findFirst().get().getMaps().stream().findFirst().get();
		var       world  = Bukkit.getWorld(c.location.world);
		MarkerSet marker = markerAPI.createMarkerSet(c.location.hashCode() + "");
		
		marker.setLabel(c.getName());
		int centerX = c.getMidpointX();
		int centerZ = c.getMidpointZ();
		
		POIMarker poiMarker = marker.createPOIMarker(c.location.hashCode() + "_marker", map,
		                                             new Vector3d(centerX, Claim.getTallestEmptyYAtLocation(world, centerX, centerZ) + 5,
		                                                          centerZ
		                                             )
		);
		poiMarker.setLabel(c.getName());
		
		var shapeMarker = marker.createExtrudeMarker(c.location.hashCode() + "_shape", map, new Vector3d(centerX, -64, centerZ),
		                                             Shape.createRect(new Vector2d(c.location.startX, c.location.startZ),
		                                                              new Vector2d(c.location.endX, c.location.endZ)
		                                             ), -64, 319
		);
		shapeMarker.setFillColor(new Color(255, 0, 0, 50));
		shapeMarker.setLineWidth(2);
		shapeMarker.setLabel(c.getName());
		shapeMarker.setDepthTestEnabled(true);
		
		try {
			markerAPI.save();
		} catch (Exception ignored) {
		}
		
	}
}
