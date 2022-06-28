package xyz.jayphen.capitalism.claims;

import org.bukkit.Location;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.commands.database.player.JSONPlayerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ClaimManager {
	private static final ArrayList<Claim> claimCache = new ArrayList<>();
	public static HashMap<UUID, Claim> adminDrafts = new HashMap<>();
	private static long lastCacheMiss = 0;
	
	public static ArrayList<Claim> getAllClaims() {
		if (System.currentTimeMillis() - lastCacheMiss > 8000) {
			ArrayList<JSONPlayerData> data = DatabasePlayer.allJsonPlayerData();
			claimCache.clear();
			for (JSONPlayerData jpd : data) {
				claimCache.addAll(jpd.claims);
			}
			lastCacheMiss = System.currentTimeMillis();
		}
		return claimCache;
	}
	
	public static Claim getDatabaseClaim(Claim c) {
		for (JSONPlayerData jpd : DatabasePlayer.allJsonPlayerData()) {
			for (Claim playerClaim : jpd.claims) {
				if (playerClaim.location.hashCode() == c.location.hashCode()) {
					return playerClaim;
				}
			}
		}
		return null;
	}
	
	public static Optional<Claim> getCachedClaim(Location loc) {
		for (Claim c : getAllClaims()) {
			if (c.inside(loc)) {
				return Optional.of(c);
			}
		}
		return Optional.empty();
	}
	
	
}
