package xyz.jayphen.capitalism.claims;

import java.util.Objects;

public class ClaimLocation {
	public int startX = 0;
	public int startZ = 0;
	public int endX = 0;
	public int endZ = 0;

	public String world = "";
	public ClaimLocation() {}

	public ClaimLocation (int startX, int startZ, int endX, int endZ, String world) {
		this.startX = startX;
		this.startZ = startZ;
		this.endX = endX;
		this.endZ = endZ;
		this.world = world;
	}

	@Override
	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClaimLocation that = (ClaimLocation) o;
		return startX == that.startX && startZ == that.startZ && endX == that.endX && endZ == that.endZ && Objects.equals(world, that.world);
	}

	@Override
	public int hashCode () {
		return Objects.hash(startX, startZ, endX, endZ, world);
	}
}
