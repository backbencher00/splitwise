package org.splitwise.strategy;

import org.splitwise.enums.SplitType;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EqualSplit implements SplitStrategy{

    @Override
    public List<BigDecimal> computeShares(BigDecimal amount, int n, SplitType splitType, List<BigDecimal> splitValues) {
        List<BigDecimal> shares = new ArrayList<>(Collections.nCopies(n, MoneyUtils.zero()));
        BigDecimal[] arr = splitEqual(amount, n);
        for (int i = 0; i < n; i++) shares.set(i, arr[i]);
        return shares;
    }
    private BigDecimal[] splitEqual(BigDecimal amount, int n) {
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), MoneyUtils.SCALE, RoundingMode.DOWN);
        BigDecimal[] arr = new BigDecimal[n];
        Arrays.fill(arr, base);
        BigDecimal used = base.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = amount.subtract(used).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);

        int idx = 0;
        BigDecimal oneCent = BigDecimal.valueOf(0.01).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);
        while (remainder.compareTo(BigDecimal.ZERO) > 0) {
            arr[idx] = arr[idx].add(oneCent);
            remainder = remainder.subtract(oneCent);
            idx = (idx + 1) % n;
        }
        for (int i=0;i<n;i++) arr[i] = MoneyUtils.scale(arr[i]);
        return arr;
    }
}
