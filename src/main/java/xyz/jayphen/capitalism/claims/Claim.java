package xyz.jayphen.capitalism.claims;

import org.bukkit.*;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Claim {
	public enum ClaimInteractionType {
		OWNER,
		GENERAL,
		WOOD,
	}

	public ClaimLocation location = null;
	public String owner = null;
	public String name = "Unnamed land claim";

	public ClaimItemShop getShopFromCoords(int x, int y, int z) {
		return getSigns().stream().filter(s -> s.getX() == x && s.getY() == y && s.getZ() == z).findFirst().orElse(null);
	}
	public Location getSignLocation(ClaimItemShop shop) {
		return new Location(Bukkit.getWorld(location.world), shop.getX(), shop.getY(), shop.getZ());
	}

	public ArrayList<ClaimItemShop> getSigns () {
		if(this.signs == null) {
			signs = new ArrayList<>();
		}
		return signs;
	}

	public ArrayList<ClaimItemShop> signs = new ArrayList<>();



	public ArrayList<String> getTrusted() {
		if(trusted == null) {
			trusted = new ArrayList<>();
		}
		return trusted;
	}
	public String getName() {
		if(name == null) {
			name = "Unnamed land claim";
		}
		return name;
	}
	private ArrayList<String> trusted = new ArrayList<>();

	private ClaimSettings permissions = new ClaimSettings();

	public ClaimSettings getPermissions() {
		if(permissions == null) {
			permissions = new ClaimSettings();
		}
		return permissions;
	}

	public boolean hasPermission(String p, ClaimInteractionType permission) {
		return hasPermission(Bukkit.getOfflinePlayer(UUID.fromString(p)), permission);
	}

	public boolean hasPermission(UUID p, ClaimInteractionType permission) {
		return hasPermission(Bukkit.getOfflinePlayer(p), permission);
	}

	public boolean hasPermission(OfflinePlayer p, ClaimInteractionType permission) {
		if(p.getUniqueId().toString().equals(owner)) return true;
		if(permission == ClaimInteractionType.OWNER && p.getUniqueId().toString().equals(owner)) return true;
		if(permission == ClaimInteractionType.GENERAL && trusted.contains(p.getUniqueId().toString())) return true;
		if(permission == ClaimInteractionType.WOOD && permissions.accessWoodDoorsAndTrapdoors) return true;
		return false;
	}

	public Claim(Location startPoint, Location endPoint, UUID owner) {
		location = new ClaimLocation();
		location.world = startPoint.getWorld().getName();
		this.owner = owner.toString();

		location.startX = startPoint.getBlockX();
		location.endX = endPoint.getBlockX();

		location.startZ = startPoint.getBlockZ();
		location.endZ = endPoint.getBlockZ();
	}
	public boolean inside(Location loc) {
		if(!location.world.equals(loc.getWorld().getName())) return false;
		int startX = Math.min(location.startX + 5000, location.endX + 5000);
		int endX = Math.max(location.startX + 5000, location.endX + 5000);

		int startZ = Math.min(location.startZ + 5000, location.endZ + 5000);
		int endZ = Math.max(location.startZ + 5000, location.endZ + 5000);
		int max = Math.max(endX - startX, endZ - startZ);
		if(Point2D.distanceSq(startX, startZ, loc.getBlockX() + 5000, loc.getBlockZ() + 5000) > max * max * max * max) {
			return false;
		}


		if(startX + 5000 > -1) {
			if(loc.getBlockX() + 5000 < startX) return false;
		} else {
			if(loc.getBlockX() + 5000 > startX) return false;
		}
		if(startZ + 5000 > -1) {
			if(loc.getBlockZ() + 5000 < startZ) return false;
		} else {
			if(loc.getBlockZ() + 5000 > startZ) return false;
		}

		if(endX + 5000 < -1) {
			if(loc.getBlockX() + 5000 < endX) return false;
		} else {
			if(loc.getBlockX() + 5000 > endX) return false;
		}
		if(endZ + 5000 < -1) {
			return loc.getBlockZ() + 5000 >= endZ;
		} else {
			return loc.getBlockZ() + 5000 <= endZ;
		}
	}

	public static int getTallestEmptyYAtLocation(World w, int x, int z) {
		for(int i = 200; i > 0; i--) {
			Material type = w.getBlockAt(x, i, z).getType();
			if(type != Material.AIR && type != Material.GRASS && type != Material.TALL_GRASS) return i + 1;
		}
		return 0;
	}

	public void transfer(UUID p) {
		this.destroy();
		DatabasePlayer.from(p).getJsonPlayer().getData().claims.add(
				new Claim(new Location(Bukkit.getWorld(location.world), location.startX, 0, location.startZ),
				          new Location(Bukkit.getWorld(location.world), location.endX, 0, location.endZ),
		p));
		DatabasePlayer.from(p).getJsonPlayer().save();

		DatabasePlayer.from(UUID.fromString(owner)).getJsonPlayer().save();
	}

	public int getDistanceFrom(int x, int y) {
		int startX = Math.min(location.startX + 5000, location.endX + 5000);
		int endX = Math.max(location.startX + 5000, location.endX + 5000);

		int startZ = Math.min(location.startZ + 5000, location.endZ + 5000);
		int endZ = Math.max(location.startZ + 5000, location.endZ + 5000);
		int avgX = (startX + endX) / 2;
		int avgZ = (startZ + endZ) / 2;
		return (int) Math.abs(Point2D.distance(avgX, avgZ, x + 5000, y + 5000));
	}

	public int getMidpointX() {
		int startX = Math.min(location.startX, location.endX);
		int endX = Math.max(location.startX, location.endX);

		return (startX + endX) / 2;
	}
	public int getEstWorth() {
		return (this.getArea() * (3000 + ((this.getDistanceFromSpawn() / 30) * 100)));
	}
	public int getMidpointZ() {
		int startZ = Math.min(location.startZ, location.endZ);
		int endZ = Math.max(location.startZ, location.endZ);

		return (startZ + endZ) / 2;
	}

	public int getDistanceFromSpawn() {
		return getDistanceFrom(0, 0);

	}
	public int getArea() {

		int startX = Math.min(location.startX + 5000, location.endX + 5000);
		int endX = Math.max(location.startX + 5000, location.endX + 5000);

		int startZ = Math.min(location.startZ + 5000, location.endZ + 5000);
		int endZ = Math.max(location.startZ + 5000, location.endZ + 5000);


		return (endX-startX) * (endZ-startZ);
	}

	public void destroy() {
		DatabasePlayer.from(UUID.fromString(owner)).getJsonPlayer().getData().claims.removeIf(x -> {
			if(this.getBorderBlocks().size() != x.getBorderBlocks().size()) return false;
			if(!this.owner.equals(x.owner)) return false;
			return this.location.equals(x.location);
		});
		DatabasePlayer.from(UUID.fromString(owner)).getJsonPlayer().save();
	}

	public List<Location> getBorderBlocks() {
		int startX = Math.min(location.startX + 5000, location.endX + 5000);
		int endX = Math.max(location.startX + 5000, location.endX + 5000);

		int startZ = Math.min(location.startZ + 5000, location.endZ + 5000);
		int endZ = Math.max(location.startZ + 5000, location.endZ + 5000);
		ArrayList<Location> locs = new ArrayList<>();
		World w = Bukkit.getWorld(location.world);
		for(int i = startX; i <= endX; i++) {
			for(int j = startZ; j <= endZ; j++) {
				if(i == startX || j == startZ || i == endX || j == endZ) {
					locs.add(new Location(w, i - 5000, getTallestEmptyYAtLocation(w, i - 5000, j - 5000), j - 5000));
				}
			}
		}
		return locs;
	}
}
