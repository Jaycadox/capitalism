package xyz.jayphen.capitalism.claims;

public class ClaimOffer {
	public ClaimLocation locationOffer = null;
	public int price = 0;

	public Boolean valid = true;

	public ClaimOffer (ClaimLocation locationOffer, int price) {
		this.locationOffer = locationOffer;
		this.price = price;
	}
}
