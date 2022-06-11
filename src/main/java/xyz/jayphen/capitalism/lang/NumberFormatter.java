package xyz.jayphen.capitalism.lang;

import java.text.DecimalFormat;

public class NumberFormatter {
    public static String addCommas(double val) {
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        return formatter.format(val);
    }
}
