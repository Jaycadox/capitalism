package xyz.jayphen.capitalism.database.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JSONPlayer {
	
	private DatabasePlayer dbp;
	private JSONPlayerData data;
	
	public JSONPlayer() {}
	
	public static boolean isClaimOfferValid(ClaimOffer offer) {
		if (offer.valid == null) {
			offer.valid = true;
		}
		return offer.valid;
	}
	
	public void setDbp(DatabasePlayer dbp) {
		this.dbp = dbp;
	}
	
	public List<Component> getMessageQueue() {
		if (data.messageQueue == null) {
			data.messageQueue = new ArrayList<>();
		}
		return data.messageQueue.stream().map(x -> GsonComponentSerializer.gson().deserialize(x)).collect(Collectors.toList());
	}
	
	public Long getBannedUntil() {
		if (data.bannedUntil == null) {
			data.bannedUntil = (long) -1;
		}
		return data.bannedUntil;
	}
	
	public ArrayList<RetractedBan> getRetractedBans() {
		if (data.retractedBans == null) {
			data.retractedBans = new ArrayList<>();
		}
		return data.retractedBans;
	}
	
	public ArrayList<String> getBanRecord() {
		if (data.banRecord == null) {
			data.banRecord = new ArrayList<>();
		}
		return data.banRecord;
	}
	
	public String getBanReason() {
		if (data.banReason == null) {
			data.banReason = "";
		}
		return data.banReason;
	}
	
	public void removeMessageFromQueue(Component cmp) {
		data.messageQueue = data.messageQueue.stream().filter(x -> cmp.hashCode() == GsonComponentSerializer.gson().deserialize(x).hashCode())
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private void addMessageQueue(Component cmp) {
		data.messageQueue.add(GsonComponentSerializer.gson().serialize(cmp));
	}
	
	public void queueMessage(Component msg) {
		addMessageQueue(msg);
		save();
	}
	
	public JSONPlayerData getData() {
		return data;
	}
	
	protected void setData(JSONPlayerData data) {
		this.data = data;
	}
	
	public ArrayList<ClaimOffer> getClaimOffers() {
		if (data.claimOffers == null) {
			data.claimOffers = new ArrayList<>();
		}
		data.claimOffers = new ArrayList<>(data.claimOffers.stream().filter(JSONPlayer::isClaimOfferValid).collect(Collectors.toList()));
		return data.claimOffers;
	}
	
	public Claim getClaim(Claim c) {
		for (int i = 0; i < data.claims.size(); i++) {
			if (data.claims.get(i).location.hashCode() == c.location.hashCode()) {
				return data.claims.get(i);
			}
		}
		return null;
	}
	
	public void save() {
		dbp.saveJsonPlayer();
	}
}
