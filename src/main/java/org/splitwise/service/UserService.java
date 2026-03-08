package org.splitwise.service;

import org.splitwise.exceptions.InvalidSplitException;
import org.splitwise.model.User;
import org.splitwise.repo.BalanceSheetRepo;
import org.splitwise.repo.UserRepo;

public class UserService {

    // 1) add user
    public void addUser(String userId, String name) {
        if (userId == null || userId.isEmpty()) throw new InvalidSplitException("userId required");
        if (UserRepo.exists(userId)) throw new InvalidSplitException("User already exists");
        UserRepo.addUser(new User(userId, name));
        BalanceSheetRepo.getOrCreateRow(userId); // ensure row exists
    }

}
