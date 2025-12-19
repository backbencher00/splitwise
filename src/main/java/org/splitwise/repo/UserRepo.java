package org.splitwise.repo;

import org.splitwise.model.User;

import java.util.concurrent.ConcurrentHashMap;

public class UserRepo {
    public static final ConcurrentHashMap<String, User> userDb = new ConcurrentHashMap<>();

    public static void addUser(User user){
        userDb.put(user.getUserId(), user);
    }

    public static boolean exists(String userId){
        return userDb.containsKey(userId);
    }
}