package org.splitwise.strategy;

import org.splitwise.enums.SplitType;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PercentageSplit implements SplitStrategy{

    @Override
    public List<BigDecimal> computeShares(BigDecimal amount, int n, SplitType splitType, List<BigDecimal> splitValues) {
        List<BigDecimal> shares = new ArrayList<>(Collections.nCopies(n, MoneyUtils.zero()));
        for (int i = 0; i < n; i++) {
            BigDecimal percent = splitValues.get(i);
            BigDecimal share = amount.multiply(percent).divide(BigDecimal.valueOf(100), MoneyUtils.SCALE, RoundingMode.HALF_UP);
            shares.set(i, MoneyUtils.scale(share));
        }
        return shares;
    }
}
