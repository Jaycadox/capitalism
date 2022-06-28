package xyz.jayphen.capitalism.events.tax;

public class TaxedDeath implements ITax {
	public static final TaxedDeath INSTANCE = new TaxedDeath();
	
	@Override
	public double getTaxAmount(int money) {
		if (money >= 0 && money <= 999999) {
			return 0.05;
		} else if (money >= 1000000 && money <= 9999999) {
			return 0.07;
		} else if (money >= 10000000 && money <= 99999999) {
			return 0.09;
		} else if (money >= 100000000 && money <= 899999999) {
			return 0.11;
		} else if (money >= 900000000 && money <= 1000000000) {
			return 0.15;
		}
		return 0.05;
	}
	
	@Override
	public TaxResult applyTax(int money) {
		double amtTaxed = Math.min(money, money * getTaxAmount(money));
		return new TaxResult(money - amtTaxed, amtTaxed, getTaxAmount(money));
	}
}
