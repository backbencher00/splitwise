package org.splitwise.service;

import org.splitwise.enums.SplitType;
import org.splitwise.exceptions.InvalidSplitException;
import org.splitwise.repo.BalanceSheetRepo;
import org.splitwise.repo.UserRepo;
import org.splitwise.strategy.SplitStrategy;
import org.splitwise.utils.MoneyUtils;

import java.math.BigDecimal;
import java.util.*;

public class Splitervice {

    // 2) add expense
    // splitValues: for PERCENTAGE -> percentages, for EXACT -> amounts, for EQUAL -> ignored (can be null)
    SplitStrategy strategy;
    public void setSplitStrategy(SplitStrategy strategy){
        this.strategy = strategy;
    }
    public void addExpense(String paidByUserId,
                           BigDecimal amount,
                           List<String> participants,
                           SplitType splitType,
                           List<BigDecimal> splitValues) {

        validateExpenseInput(paidByUserId, amount, participants, splitType, splitValues);

        int n = participants.size();
        List<BigDecimal> shares = strategy.computeShares(amount, n, splitType, splitValues);

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




}