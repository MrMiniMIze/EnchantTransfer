package me.minimize;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.minimize.commands.EnchantTransferCommand;
import me.minimize.gui.EnchantTransferGUI;
import me.minimize.listeners.InventoryCloseListener;
import me.minimize.util.EconomyUtil;

public class EnchantTransferPlugin extends JavaPlugin {

    private static final boolean TIME_LIMIT_ENABLED = true; // Set true if you want a 30-min disable

    private static EnchantTransferPlugin instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register /etransfer
        if (getCommand("etransfer") != null) {
            getCommand("etransfer").setExecutor(new EnchantTransferCommand(this));
            getCommand("etransfer").setTabCompleter(new EnchantTransferCommand(this));
        }

        // Register GUI events
        getServer().getPluginManager().registerEvents(new EnchantTransferGUI(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);

        // Optional 30-min time limit
        if (TIME_LIMIT_ENABLED) {
            long ticks = 30L * 60L * 20L;
            getLogger().info("TIME_LIMIT_ENABLED = true, disabling in 30 mins...");
            getServer().getScheduler().runTaskLater(this, () -> {
                getLogger().info("30 minutes passed. Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
            }, ticks);
        } else {
            getLogger().info("TIME_LIMIT_ENABLED = false, no auto-disable scheduled.");
        }

        getLogger().info("Paper-Only EnchantTransfer Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantTransfer Plugin Disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        EconomyUtil.setEconomy(economy);
        return (economy != null);
    }

    public static EnchantTransferPlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }
}
