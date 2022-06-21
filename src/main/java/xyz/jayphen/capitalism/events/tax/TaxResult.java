package xyz.jayphen.capitalism.events.tax;

public class TaxResult {
	double amountLeft;
	double amountTaxed;

	public double getTaxAmount () {
		return taxAmount;
	}

	double taxAmount;

	public TaxResult (double amountLeft, double amountTaxed, double taxAmount) {
		this.amountLeft = amountLeft;
		this.amountTaxed = amountTaxed;
		this.taxAmount = taxAmount;
	}

	public double getAmountLeft () {
		return amountLeft;
	}

	public double getAmountTaxed () {
		return Math.ceil(amountTaxed);
	}

	public boolean canAfford() {
		return amountLeft > -1;
	}
}
