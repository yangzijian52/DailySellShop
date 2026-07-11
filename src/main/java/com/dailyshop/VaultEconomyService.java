package com.dailyshop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyService implements EconomyService {

    private final Economy economy;

    private VaultEconomyService(Economy economy) {
        this.economy = economy;
    }

    public static EconomyService create(DailySellShop plugin) {
        RegisteredServiceProvider<Economy> registration = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            return null;
        }
        return new VaultEconomyService(registration.getProvider());
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public TransactionResult withdraw(Player player, double amount) {
        return result(economy.withdrawPlayer(player, amount));
    }

    @Override
    public TransactionResult deposit(Player player, double amount) {
        return result(economy.depositPlayer(player, amount));
    }

    private TransactionResult result(EconomyResponse response) {
        return new TransactionResult(response.transactionSuccess(), response.errorMessage);
    }
}
