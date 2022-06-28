package xyz.jayphen.capitalism.claims.region;

import org.bukkit.Location;

public class RegionManager {
		public static Region getRegion(Location loc) {
				Location spawn    = new Location(loc.getWorld(), 0, loc.getBlockY(), 0);
				double   distance = loc.distance(spawn);
				if (distance > 2000) {
						return Region.UNCLAIMABLE;
				}
				if (distance > 500) {
						return Region.RESIDENTIAL;
				}
				return Region.COMMERCIAL;
		}
		
		public enum Region {
				COMMERCIAL, RESIDENTIAL, UNCLAIMABLE
		}
		
}
