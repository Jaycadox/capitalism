package xyz.jayphen.capitalism.lang;

import java.text.DecimalFormat;

public class NumberFormatter {
	public static String addCommas (double val) {
		if (val == 0) return "0";
		DecimalFormat formatter = new DecimalFormat("#,###");
		return formatter.format(val);
	}
}
