package me.minimize.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class EconomyUtil {
    private static Economy economy;

    public static void setEconomy(Economy econ) {
        economy = econ;
    }

    public static double getBalance(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    public static void withdraw(Player player, double amount) {
        if (economy != null && amount > 0) {
            economy.withdrawPlayer(player, amount);
        }
    }
}
