package org.splitwise.strategy;

import org.splitwise.enums.SplitType;

import java.math.BigDecimal;
import java.util.List;

public interface SplitStrategy {

    List<BigDecimal> computeShares(BigDecimal amount, int n, SplitType splitType, List<BigDecimal> splitValues);

}
