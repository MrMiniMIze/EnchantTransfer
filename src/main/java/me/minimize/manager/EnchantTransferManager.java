package me.minimize.manager;

import me.minimize.EnchantTransferPlugin;
import me.minimize.util.EconomyUtil;
import me.minimize.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class EnchantTransferManager {

    public static boolean handleTransfer(Player player, ItemStack item1, ItemStack item2) {
        // Must be paper in both slots
        if (!isPaper(item1) || !isPaper(item2)) {
            player.sendMessage(ChatColor.RED + "Both slots must have paper!");
            return false;
        }

        // If no enchants on item1
        int totalEnchants = getTotalEnchants(item1);
        if (totalEnchants == 0) {
            player.sendMessage(MessageUtil.getMsg("no-enchants"));
            return false;
        }

        // Check cost & balance
        double cost = calculateTotalCost(player, item1);
        double balance = EconomyUtil.getBalance(player);
        if (balance < cost) {
            player.sendMessage(MessageUtil.getMsg("insufficient-funds")
                    .replace("%cost%", String.valueOf(cost))
                    .replace("%balance%", String.valueOf(balance)));
            return false;
        }

        // Transfer enchant logic
        transferPaperEnchants(item1, item2);

        EconomyUtil.withdraw(player, cost);
        player.sendMessage(MessageUtil.getMsg("transfer-success")
                .replace("%cost%", String.valueOf(cost)));
        return true;
    }

    public static int getTotalEnchants(ItemStack item) {
        if (!isPaper(item)) return 0;
        return item.getEnchantments().size();
    }

    public static double calculateTotalCost(Player player, ItemStack item1) {
        if (!isPaper(item1)) return 0.0;

        double total = 0.0;
        // For each enchant on the paper, we look up cost from "PAPER"
        for (Map.Entry<Enchantment, Integer> entry : item1.getEnchantments().entrySet()) {
            String group = "PAPER"; // We treat paper as "PAPER" cost group
            double groupCost = EnchantTransferPlugin.getInstance()
                    .getConfig().getConfigurationSection("cost-groups")
                    .getDouble(group, 0.0);
            total += groupCost;
        }
        return total;
    }

    /**
     * Transfer enchants from item1 (paper) to item2 (paper),
     * merging levels by picking the higher level.
     * We skip canEnchantItem() check, since we're forcibly
     * allowing paper -> paper.
     */
    private static void transferPaperEnchants(ItemStack item1, ItemStack item2) {
        Map<Enchantment, Integer> donorEnchants = item1.getEnchantments();

        // Merge
        for (Map.Entry<Enchantment, Integer> entry : donorEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int donorLevel = entry.getValue();
            int existingLevel = item2.getEnchantmentLevel(ench);
            int newLevel = Math.max(donorLevel, existingLevel);

            if (existingLevel > 0) {
                item2.removeEnchantment(ench);
            }
            item2.addUnsafeEnchantment(ench, newLevel);
        }

        // Remove all enchants from item1
        stripPaperEnchants(item1);
    }

    private static void stripPaperEnchants(ItemStack item) {
        // remove all vanilla enchants from paper
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        for (Enchantment ench : item.getEnchantments().keySet()) {
            meta.removeEnchant(ench);
        }
        item.setItemMeta(meta);
    }

    private static boolean isPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER;
    }
}
