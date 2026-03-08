package org.splitwise.service;

import org.splitwise.model.Transaction;
import org.splitwise.repo.BalanceSheetRepo;
import org.splitwise.repo.UserRepo;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SettleTransactionService {
    // settleUp using two heaps (minimize number of transactions)
    public List<Transaction> settleUp() {
        // compute net for each user: net = sumReceived - sumOwed
        Map<String, BigDecimal> net = computeNetBalanceOfEachUser();

        // creditors: net > 0, debtors: net < 0
        PriorityQueue<Map.Entry<String, BigDecimal>> creditors = new PriorityQueue<>(
                (a, b) -> b.getValue().compareTo(a.getValue()));

        PriorityQueue<Map.Entry<String, BigDecimal>> debtors = new PriorityQueue<>(
                Comparator.comparing(Map.Entry::getValue));

        for (Map.Entry<String, BigDecimal> e : net.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) > 0) creditors.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
            else if (e.getValue().compareTo(BigDecimal.ZERO) < 0) debtors.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
        }

        List<Transaction> res = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Map.Entry<String, BigDecimal> cred = creditors.poll();
            Map.Entry<String, BigDecimal> debt = debtors.poll();

            BigDecimal credit = cred.getValue();
            BigDecimal debit = debt.getValue().abs();

            BigDecimal min = credit.min(debit).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);

            // debtor pays creditor
            res.add(new Transaction(debt.getKey(), cred.getKey(), min));

            BigDecimal newCred = credit.subtract(min).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);
            BigDecimal newDebt = debit.subtract(min).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);

            if (newCred.compareTo(BigDecimal.ZERO) > 0) creditors.add(new AbstractMap.SimpleEntry<>(cred.getKey(), newCred));
            if (newDebt.compareTo(BigDecimal.ZERO) > 0) debtors.add(new AbstractMap.SimpleEntry<>(debt.getKey(), newDebt.negate()));
        }

        return res;
    }

    private static Map<String, BigDecimal> computeNetBalanceOfEachUser() {
        Map<String, BigDecimal> net = new HashMap<>();

        Set<String> users = new HashSet<>(BalanceSheetRepo.balanceSheet.keySet());

        // ensure every user is considered (in case some rows empty)
        users.addAll(UserRepo.userDb.keySet());

        for (String user : users) {
            BigDecimal owes = BalanceSheetRepo.getOrCreateRow(user).values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal receives = BigDecimal.ZERO;
            for (String other : BalanceSheetRepo.balanceSheet.keySet()) {
                if (other.equals(user)) continue;
                receives = receives.add(BalanceSheetRepo.getOrCreateRow(other).getOrDefault(user, BigDecimal.ZERO));
            }
            BigDecimal n = receives.subtract(owes).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP);
            net.put(user, n);
        }
        return net;
    }
}
