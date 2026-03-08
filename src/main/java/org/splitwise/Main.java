package org.splitwise;

import org.splitwise.enums.SplitType;
import org.splitwise.model.Transaction;
import org.splitwise.service.BalanceService;
import org.splitwise.service.SettleTransactionService;
import org.splitwise.service.Splitervice;
import org.splitwise.service.UserService;
import org.splitwise.strategy.EqualSplit;
import org.splitwise.strategy.ExactSplit;
import org.splitwise.utils.MoneyUtils;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Splitervice splitervice = new Splitervice();
        SettleTransactionService settleTransactionService = new SettleTransactionService();
        UserService service = new UserService();
        BalanceService balanceService = new BalanceService();
        // add users
        service.addUser("u1", "A");
        service.addUser("u2", "B");
        service.addUser("u3", "C");
        service.addUser("u4", "D");

        // u1 paid 120 split among u1,u2,u3 equal -> each 40 -> u2 owes u1 40, u3 owes u1 40
        splitervice.setSplitStrategy(new EqualSplit());
        splitervice.addExpense("u1", MoneyUtils.bd(120), Arrays.asList("u1", "u2", "u3"), SplitType.EQUAL, null);

        // u2 paid 150 split exact: u1:50, u2:50, u3:50
        splitervice.setSplitStrategy(new ExactSplit());
        splitervice.addExpense("u2", MoneyUtils.bd(150), Arrays.asList("u1","u2","u3"), SplitType.EXACT,
                Arrays.asList(MoneyUtils.bd(50), MoneyUtils.bd(50), MoneyUtils.bd(50)));

        // u3 paid 100 split by percentage: u1:50%, u2:25%, u3:25%
        splitervice.setSplitStrategy(new ExactSplit());
        splitervice.addExpense("u3", MoneyUtils.bd(100), Arrays.asList("u1","u2","u3"),
                SplitType.PERCENTAGE, Arrays.asList(MoneyUtils.bd(50), MoneyUtils.bd(25), MoneyUtils.bd(25)));

        System.out.println("=== BALANCES (who owes whom) ===");
        balanceService.showBalances();

        System.out.println("\n=== SETTLEMENT TRANSACTIONS ===");
        List<Transaction> tx = settleTransactionService.settleUp();
        tx.forEach(System.out::println);
    }
}