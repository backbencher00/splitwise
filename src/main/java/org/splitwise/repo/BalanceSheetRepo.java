package org.splitwise.repo;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceSheetRepo {
    // balance[A].get(B) = amount A owes B (positive BigDecimal)
    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, BigDecimal>> balanceSheet = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, BigDecimal> getOrCreateRow(String userId){
        return balanceSheet.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
    }

    public static BigDecimal get(String from, String to){
        return getOrCreateRow(from).getOrDefault(to, BigDecimal.ZERO);
    }

    public static void addDebt(String from, String to, BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;
        getOrCreateRow(from).merge(to, amount, BigDecimal::add);
    }
}