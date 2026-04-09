package com.dailyshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class DailySellShop extends JavaPlugin {

    private static DailySellShop instance;
    private static Economy econ;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;
        Logger log = getLogger();

        saveDefaultConfig();

        if (!setupEconomy()) {
            log.severe(String.format("[%s] 必须先安装 Vault 和经济插件后才能运行。", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(this);

        if (getCommand("ds") != null) {
            getCommand("ds").setExecutor(new ShopCommand());
        }

        getServer().getPluginManager().registerEvents(new ShopListener(), this);

        log.info("DailySellShop 已成功启用，支持 Java 版与基岩版玩家。");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }

    public static DailySellShop getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
