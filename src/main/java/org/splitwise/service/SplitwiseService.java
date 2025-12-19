package org.splitwise.service;

import org.splitwise.enums.SplitType;
import org.splitwise.exceptions.InvalidSplitException;
import org.splitwise.model.Transaction;
import org.splitwise.model.User;
import org.splitwise.repo.BalanceSheetRepo;
import org.splitwise.repo.UserRepo;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SplitwiseService {

    // 1) add user
    public void addUser(String userId, String name) {
        if (userId == null || userId.isEmpty()) throw new InvalidSplitException("userId required");
        if (UserRepo.exists(userId)) throw new InvalidSplitException("User already exists");
        UserRepo.addUser(new User(userId, name));
        BalanceSheetRepo.getOrCreateRow(userId); // ensure row exists
    }

    // 2) add expense
    // splitValues: for PERCENTAGE -> percentages, for EXACT -> amounts, for EQUAL -> ignored (can be null)
    public void addExpense(String paidByUserId,
                           BigDecimal amount,
                           List<String> participants,
                           SplitType splitType,
                           List<BigDecimal> splitValues) {

        validateExpenseInput(paidByUserId, amount, participants, splitType, splitValues);

        int n = participants.size();
        List<BigDecimal> shares = computeShares(amount, n, splitType, splitValues);

        // For each participant except payer, participant owes payer 'share'
        for (int i = 0; i < n; i++) {
            String user = participants.get(i);
            BigDecimal share = MoneyUtils.scale(shares.get(i));

            if (user.equals(paidByUserId)) {
                // if payer is part of participants, they effectively paid their share
                // no-op for debt entries (others owe payer)
            } else {
                // user owes paidByUserId share
                BalanceSheetRepo.addDebt(user, paidByUserId, share);
            }
        }
    }

    private List<BigDecimal> computeShares(BigDecimal amount, int n, SplitType splitType, List<BigDecimal> splitValues) {
        List<BigDecimal> shares = new ArrayList<>(Collections.nCopies(n, MoneyUtils.zero()));

        switch (splitType) {
            case EQUAL:
                BigDecimal[] arr = splitEqual(amount, n);
                for (int i = 0; i < n; i++) shares.set(i, arr[i]);
                break;
            case PERCENTAGE:
                for (int i = 0; i < n; i++) {
                    BigDecimal percent = splitValues.get(i);
                    BigDecimal share = amount.multiply(percent).divide(BigDecimal.valueOf(100), MoneyUtils.SCALE, RoundingMode.HALF_UP);
                    shares.set(i, MoneyUtils.scale(share));
                }
                break;
            case EXACT:
                for (int i = 0; i < n; i++) {
                    shares.set(i, MoneyUtils.scale(splitValues.get(i)));
                }
                break;
            default:
                throw new InvalidSplitException("Unknown split type");
        }
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

    private void validateExpenseInput(String paidByUserId,
                                      BigDecimal amount,
                                      List<String> participants,
                                      SplitType splitType,
                                      List<BigDecimal> splitValues) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidSplitException("Amount must be > 0");
        if (participants == null || participants.isEmpty()) throw new InvalidSplitException("Participants required");
        if (!UserRepo.exists(paidByUserId)) throw new InvalidSplitException("Payer not found");
        Set<String> set = new HashSet<>(participants);
        if (set.size() != participants.size()) throw new InvalidSplitException("Duplicate participants");
        // participants must exist
        for (String p : participants) if (!UserRepo.exists(p)) throw new InvalidSplitException("Participant not found: " + p);

        if (splitType == SplitType.PERCENTAGE) {
            if (splitValues == null || splitValues.size() != participants.size())
                throw new InvalidSplitException("Percentage values missing or size mismatch");
            BigDecimal sum = splitValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.valueOf(100)) != 0)
                throw new InvalidSplitException("Percentages must sum to 100");
        } else if (splitType == SplitType.EXACT) {
            if (splitValues == null || splitValues.size() != participants.size())
                throw new InvalidSplitException("Exact values missing or size mismatch");
            BigDecimal sum = splitValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(amount) != 0)
                throw new InvalidSplitException("Exact amounts must sum to total amount");
        }
    }

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

    // settleUp using two heaps (minimize number of transactions)
    public List<Transaction> settleUp() {
        // compute net for each user: net = sumReceived - sumOwed
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
}