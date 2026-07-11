package com.dailyshop;

import org.bukkit.entity.Player;

public interface EconomyService {

    double getBalance(Player player);

    boolean has(Player player, double amount);

    TransactionResult withdraw(Player player, double amount);

    TransactionResult deposit(Player player, double amount);

    record TransactionResult(boolean success, String errorMessage) {
    }
}
