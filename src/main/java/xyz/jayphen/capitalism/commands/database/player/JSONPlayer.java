package xyz.jayphen.capitalism.commands.database.player;

import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimLocation;
import xyz.jayphen.capitalism.claims.ClaimOffer;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class JSONPlayer {

	public void setDbp (DatabasePlayer dbp) {
		this.dbp = dbp;
	}

	protected void setData (JSONPlayerData data) {
		this.data = data;
	}

	private DatabasePlayer dbp;

	public ArrayList<String> getMessageQueue() {
		if(data.messageQueue == null) {
			data.messageQueue = new ArrayList<>();
		}
		return data.messageQueue;
	}

	public void queueMessage(String msg) {
		getMessageQueue().add(msg);
		save();
	}

	public JSONPlayerData getData () {
		return data;
	}
	public ArrayList<ClaimOffer> getClaimOffers() {
		if(data.claimOffers == null) {
			data.claimOffers = new ArrayList<>();
		}
		data.claimOffers = new ArrayList<>(data.claimOffers.stream().filter(JSONPlayer::isClaimOfferValid).collect(Collectors.toList()));
		return data.claimOffers;
	}
	public static boolean isClaimOfferValid(ClaimOffer offer) {
		if(offer.valid == null) {
			offer.valid = true;
		}
		return offer.valid;
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
