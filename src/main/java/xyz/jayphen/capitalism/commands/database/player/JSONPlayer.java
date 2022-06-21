package xyz.jayphen.capitalism.commands.database.player;

import xyz.jayphen.capitalism.claims.Claim;

public class JSONPlayer {

	public void setDbp (DatabasePlayer dbp) {
		this.dbp = dbp;
	}

	protected void setData (JSONPlayerData data) {
		this.data = data;
	}

	private DatabasePlayer dbp;

	public JSONPlayerData getData () {
		return data;
	}

	public Claim getClaim(Claim c) {
		for(int i = 0; i < data.claims.size(); i++) {
			if(data.claims.get(i).location.hashCode() == c.location.hashCode()) {
				return data.claims.get(i);
			}
		}
		return null;
	}

	private JSONPlayerData data;

	public void save() {
		dbp.saveJsonPlayer();
	}

	public JSONPlayer() {}
}
