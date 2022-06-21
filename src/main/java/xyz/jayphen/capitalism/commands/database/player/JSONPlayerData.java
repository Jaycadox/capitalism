package xyz.jayphen.capitalism.commands.database.player;

import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.commands.database.player.json.Stats;

import java.util.ArrayList;

public class JSONPlayerData {
	public Stats stats = new Stats();
	public String uuid = "";
	public ArrayList<Claim> claims = new ArrayList<>();

	public boolean seenLandlordTip = false;


}
