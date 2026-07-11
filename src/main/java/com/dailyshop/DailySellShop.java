package com.dailyshop;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class DailySellShop extends JavaPlugin {

    private static DailySellShop instance;
    private static EconomyService economyService;
    private ShopManager shopManager;
    private BuyShopManager buyShopManager;
    private PurchaseManager purchaseManager;
    private File sellConfigFile;
    private File shopConfigFile;
    private FileConfiguration sellConfig;
    private FileConfiguration shopConfig;

    @Override
    public void onEnable() {
        instance = this;
        Logger log = getLogger();

        if (!initializeConfigurations()) {
            log.severe("配置文件初始化失败，插件已停止启用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            log.warning("未找到 Vault 或可用经济插件：菜单仍可浏览，但收购和购买交易将被禁止。");
        }

        shopManager = new ShopManager(this);
        buyShopManager = new BuyShopManager(this);
        purchaseManager = new PurchaseManager(this);

        if (getCommand("ds") != null) {
            getCommand("ds").setExecutor(new ShopCommand());
        }
        if (getCommand("dss") != null) {
            BuyShopCommand buyCommand = new BuyShopCommand(this);
            getCommand("dss").setExecutor(buyCommand);
            getCommand("dss").setTabCompleter(buyCommand);
        }

        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        getServer().getPluginManager().registerEvents(new BuyShopListener(this), this);

        log.info("DailySellShop 已成功启用，支持 Java 版与基岩版玩家。");
    }

    @Override
    public void onDisable() {
        if (purchaseManager != null) {
            purchaseManager.shutdown();
        }
    }

    private boolean initializeConfigurations() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            return false;
        }

        sellConfigFile = new File(getDataFolder(), "SellShopconfig.yml");
        shopConfigFile = new File(getDataFolder(), "shopconfig.yml");
        File legacyConfig = new File(getDataFolder(), "config.yml");

        try {
            if (!sellConfigFile.exists()) {
                if (legacyConfig.exists()) {
                    Files.copy(legacyConfig.toPath(), sellConfigFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                    getLogger().info("已将旧 config.yml 迁移为 SellShopconfig.yml；旧文件保留作为备份。");
                } else {
                    saveResource("SellShopconfig.yml", false);
                }
            }
            if (!shopConfigFile.exists()) {
                saveResource("shopconfig.yml", false);
            }
        } catch (IllegalArgumentException | IOException exception) {
            getLogger().severe("无法创建配置文件: " + exception.getMessage());
            return false;
        }

        reloadAllConfigurations();
        return true;
    }

    public void reloadAllConfigurations() {
        sellConfig = YamlConfiguration.loadConfiguration(sellConfigFile);
        shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            economyService = VaultEconomyService.create(this);
        } catch (LinkageError | RuntimeException exception) {
            getLogger().warning("Vault 经济接口初始化失败: " + exception.getMessage());
            return false;
        }
        return economyService != null;
    }

    public static DailySellShop getInstance() {
        return instance;
    }

    public static EconomyService getEconomy() {
        return economyService;
    }

    public boolean hasEconomy() {
        return economyService != null;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public BuyShopManager getBuyShopManager() {
        return buyShopManager;
    }

    public PurchaseManager getPurchaseManager() {
        return purchaseManager;
    }

    public FileConfiguration getSellConfig() {
        return sellConfig;
    }

    public FileConfiguration getShopConfig() {
        return shopConfig;
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
