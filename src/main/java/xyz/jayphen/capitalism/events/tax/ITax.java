package xyz.jayphen.capitalism.events.tax;

public interface ITax {
		double getTaxAmount(int money);
		
		TaxResult applyTax(int money);
}
