package xyz.jayphen.capitalism.claims;

import java.util.Objects;

public class ClaimItemShop {
	int price;
	int x, y, z;

	public int getPrice () {
		return price;
	}

	public int getX () {
		return x;
	}

	public int getY () {
		return y;
	}

	public int getZ () {
		return z;
	}

	@Override
	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClaimItemShop that = (ClaimItemShop) o;
		return price == that.price && x == that.x && y == that.y && z == that.z;
	}

	@Override
	public int hashCode () {
		return Objects.hash(price, x, y, z);
	}

	public ClaimItemShop (int price, int x, int y, int z) {
		this.price = price;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
