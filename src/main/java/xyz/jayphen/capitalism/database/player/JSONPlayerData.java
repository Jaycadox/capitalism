package xyz.jayphen.capitalism.database.player;

import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimOffer;
import xyz.jayphen.capitalism.database.player.json.Stats;

import java.util.ArrayList;

public class JSONPlayerData {
	public Stats                 stats        = new Stats();
	public String                uuid         = "";
	public ArrayList<Claim>      claims       = new ArrayList<>();
	public ArrayList<ClaimOffer> claimOffers  = new ArrayList<>();
	public ArrayList<String>     messageQueue = new ArrayList<>();
	public Long                  bannedUntil  = (long) -1;
	public String                banReason    = "";
	
	public ArrayList<String> banRecord       = new ArrayList<>();
	public boolean           seenLandlordTip = false;
	
	
}
