package xyz.jayphen.capitalism.database.player;

public class RetractedBan {
	public Integer id     = 0;
	public String  reason = "none provided";
	
	public RetractedBan(Integer id, String reason) {
		this.id     = id;
		this.reason = reason;
	}
}
