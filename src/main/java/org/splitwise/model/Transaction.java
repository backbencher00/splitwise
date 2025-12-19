package org.splitwise.model;

import java.math.BigDecimal;

public class Transaction {
    private final String fromUser;
    private final String toUser;
    private final BigDecimal amount;

    public Transaction(String fromUser, String toUser, BigDecimal amount) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
    }

    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public BigDecimal getAmount() { return amount; }

    @Override
    public String toString() {
        return fromUser + " pays " + toUser + ": " + amount;
    }
}