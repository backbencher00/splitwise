package org.splitwise.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    public static final int SCALE = 2;

    public static BigDecimal bd(double v){
        return BigDecimal.valueOf(v).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero(){
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale(BigDecimal v){
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}