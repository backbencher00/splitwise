package org.splitwise;

import org.splitwise.enums.SplitType;
import org.splitwise.model.Transaction;
import org.splitwise.repo.UserRepo;
import org.splitwise.service.SplitwiseService;
import org.splitwise.utils.MoneyUtils;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SplitwiseService svc = new SplitwiseService();

        // add users
        svc.addUser("u1", "A");
        svc.addUser("u2", "B");
        svc.addUser("u3", "C");
        svc.addUser("u4", "D");

        // u1 paid 120 split among u1,u2,u3 equal -> each 40 -> u2 owes u1 40, u3 owes u1 40
        svc.addExpense("u1", MoneyUtils.bd(120), Arrays.asList("u1", "u2", "u3"), SplitType.EQUAL, null);

        // u2 paid 150 split exact: u1:50, u2:50, u3:50
        svc.addExpense("u2", MoneyUtils.bd(150), Arrays.asList("u1","u2","u3"), SplitType.EXACT,
                Arrays.asList(MoneyUtils.bd(50), MoneyUtils.bd(50), MoneyUtils.bd(50)));

        // u3 paid 100 split by percentage: u1:50%, u2:25%, u3:25%
        svc.addExpense("u3", MoneyUtils.bd(100), Arrays.asList("u1","u2","u3"),
                SplitType.PERCENTAGE, Arrays.asList(MoneyUtils.bd(50), MoneyUtils.bd(25), MoneyUtils.bd(25)));

        System.out.println("=== BALANCES (who owes whom) ===");
        svc.showBalances();

        System.out.println("\n=== SETTLEMENT TRANSACTIONS ===");
        List<Transaction> tx = svc.settleUp();
        tx.forEach(System.out::println);
    }
}