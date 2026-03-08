package org.splitwise.service;

import org.splitwise.exceptions.InvalidSplitException;
import org.splitwise.repo.BalanceSheetRepo;
import org.splitwise.repo.UserRepo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BalanceService {
    // get entire balance sheet representation: Map<user, Map<otherUser, amount user owes otherUser>>
    public Map<String, Map<String, BigDecimal>> getBalances() {
        Map<String, Map<String, BigDecimal>> res = new HashMap<>();
        for (String u : BalanceSheetRepo.balanceSheet.keySet()) {
            res.put(u, new HashMap<>(BalanceSheetRepo.getOrCreateRow(u)));
        }
        return res;
    }

    // get a user's balances (who they owe)
    public Map<String, BigDecimal> getUserBalance(String userId) {
        if (!UserRepo.exists(userId)) throw new InvalidSplitException("User not found");
        Map<String, BigDecimal> row = BalanceSheetRepo.getOrCreateRow(userId);
        Map<String, BigDecimal> nonZero = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : row.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) != 0) nonZero.put(e.getKey(), e.getValue());
        }
        return nonZero;
    }

    public void showBalances() {
        boolean any = false;
        for (String user : BalanceSheetRepo.balanceSheet.keySet()) {
            Map<String, BigDecimal> row = BalanceSheetRepo.getOrCreateRow(user);
            for (Map.Entry<String, BigDecimal> e : row.entrySet()) {
                if (e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    any = true;
                    System.out.println(user + " owes " + e.getKey() + ": " + e.getValue());
                }
            }
        }
        if (!any) System.out.println("No balances");
    }

    public void showUserBalance(String userId) {
        Map<String, BigDecimal> map = getUserBalance(userId);
        if (map.isEmpty()) {
            System.out.println("No balances");
            return;
        }
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            System.out.println(userId + " owes " + e.getKey() + ": " + e.getValue());
        }
    }
}
