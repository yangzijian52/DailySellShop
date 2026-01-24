package com.dailyshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class DailySellShop extends JavaPlugin {

    private static DailySellShop instance;
    private static Economy econ = null;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;
        Logger log = getLogger();

        // 1. 保存默认配置
        saveDefaultConfig();

        // 2. 挂钩 Vault 经济
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - 只有安装了 Vault 和经济插件(如Essentials)才能运行！", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. 初始化管理器
        shopManager = new ShopManager(this);

        // 4. 注册命令
        if (getCommand("ds") != null) {
            getCommand("ds").setExecutor(new ShopCommand());
        }

        // 5. 注册监听器
        getServer().getPluginManager().registerEvents(new ShopListener(), this);

        log.info("DailySellShop 已成功启动！(支持 Java版 & 基岩版)");
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
}
