package xyz.jayphen.capitalism.events.tax;

import xyz.jayphen.capitalism.events.DeathTax;

public class TaxedTransaction implements ITax {
	public static final TaxedTransaction INSTANCE = new TaxedTransaction();

	@Override
	public double getTaxAmount (int money) {
		return 0.08;
	}

	@Override
	public TaxResult applyTax (int money) {
		double amtTaxed = Math.min(Math.ceil(money * getTaxAmount(money)), 800000);
		return new TaxResult(money - amtTaxed, amtTaxed, getTaxAmount(money));
	}
}
