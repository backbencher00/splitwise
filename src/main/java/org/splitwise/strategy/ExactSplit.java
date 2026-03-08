package org.splitwise.strategy;

import org.splitwise.enums.SplitType;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExactSplit implements SplitStrategy{

    @Override
    public List<BigDecimal> computeShares(BigDecimal amount, int n, SplitType splitType, List<BigDecimal> splitValues) {
        List<BigDecimal> shares = new ArrayList<>(Collections.nCopies(n, MoneyUtils.zero()));
        for (int i = 0; i < n; i++) {
            shares.set(i, MoneyUtils.scale(splitValues.get(i)));
        }
        return shares;
    }
}
