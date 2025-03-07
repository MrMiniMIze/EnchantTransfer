package me.minimize.manager;

import me.minimize.EnchantTransferPlugin;
import me.minimize.util.EconomyUtil;
import me.minimize.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class EnchantTransferManager {

    public static boolean handleTransfer(Player player, ItemStack item1, ItemStack item2) {

        // 1) If item1 or item2 is null/air, block immediately
        if (item1 == null || item1.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Please place a valid item in slot #2 (item1) before confirming!");
            return false;
        }
        if (item2 == null || item2.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Please place a valid item in slot #6 (item2) before confirming!");
            return false;
        }

        boolean item1Paper = (item1.getType() == Material.PAPER);
        boolean item2Paper = (item2.getType() == Material.PAPER);

        // 2) If item1 is paper, item2 must be paper
        if (item1Paper && !item2Paper) {
            player.sendMessage(ChatColor.RED + "Paper can only be transferred to another paper!");
            return false;
        }
        // If item1 is normal, item2 cannot be paper
        if (!item1Paper && item2Paper) {
            player.sendMessage(ChatColor.RED + "This item can't be transferred onto paper!");
            return false;
        }

        // 3) Must have some enchants on item1
        int totalEnchants = getTotalEnchants(item1);
        if (totalEnchants == 0) {
            player.sendMessage(MessageUtil.getMsg("no-enchants"));
            return false;
        }

        // 4) cost
        double cost = calculateTotalCost(player, item1);
        double balance = EconomyUtil.getBalance(player);
        if (balance < cost) {
            player.sendMessage(MessageUtil.getMsg("insufficient-funds")
                    .replace("%cost%", String.valueOf(cost))
                    .replace("%balance%", String.valueOf(balance)));
            return false;
        }

        // 5) If both are normal items
        if (!item1Paper && !item2Paper) {
            if (!transferEnchantsNormal(item1, item2)) {
                player.sendMessage(MessageUtil.getMsg("incompatible-enchant"));
                return false;
            }
        }
        else {
            // Both are paper
            transferEnchantsPaper(item1, item2);
        }

        // 6) Deduct cost
        EconomyUtil.withdraw(player, cost);
        player.sendMessage(MessageUtil.getMsg("transfer-success")
                .replace("%cost%", String.valueOf(cost)));
        return true;
    }

    public static int getTotalEnchants(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        return item.getEnchantments().size();
    }

    public static double calculateTotalCost(Player player, ItemStack item1) {
        if (item1 == null || item1.getType() == Material.AIR) return 0.0;
        int enchantCount = item1.getEnchantments().size();

        String group = classifyItem(item1.getType());
        double groupCost = EnchantTransferPlugin.getInstance()
                .getConfig().getConfigurationSection("cost-groups")
                .getDouble(group, 0.0);

        return enchantCount * groupCost;
    }

    private static boolean transferEnchantsNormal(ItemStack item1, ItemStack item2) {
        Map<Enchantment, Integer> donorEnchants = item1.getEnchantments();
        Map<Enchantment, Integer> targetEnchants = item2.getEnchantments();
        
        // First check if all enchantments can be applied to the target item
        for (Enchantment ench : donorEnchants.keySet()) {
            if (!ench.canEnchantItem(item2)) {
                return false;
            }
        }
        
        // Copy enchants to avoid concurrent modification
        Map<Enchantment, Integer> combinedEnchants = new HashMap<>(targetEnchants);
        
        // Add donor enchants, keeping the higher level
        for (Map.Entry<Enchantment, Integer> entry : donorEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int donorLevel = entry.getValue();
            int existingLevel = combinedEnchants.getOrDefault(ench, 0);
            combinedEnchants.put(ench, Math.max(donorLevel, existingLevel));
        }
        
        // Check for enchantment conflicts
        if (!areEnchantsCompatible(item1, item2, combinedEnchants)) {
            return false;
        }
        
        // If all checks pass, apply enchantments
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
        
        stripAll(item1);
        return true;
    }

    private static void transferEnchantsPaper(ItemStack item1, ItemStack item2) {
        Map<Enchantment, Integer> donorEnchants = item1.getEnchantments();
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
        stripAll(item1);
    }

    /**
     * Checks if enchantments are compatible with each other based on Minecraft's rules.
     * @param sourceItem The source item with enchantments
     * @param targetItem The target item for enchantments
     * @param enchants Map of enchantments to check compatibility for
     * @return true if all enchantments are compatible, false otherwise
     */
    private static boolean areEnchantsCompatible(ItemStack sourceItem, ItemStack targetItem, Map<Enchantment, Integer> enchants) {
        Material material = targetItem.getType();
        String itemType = material.name().toLowerCase();
        
        // 1. Damage Enchantments (Swords/Axes)
        if (itemType.endsWith("_sword") || itemType.endsWith("_axe")) {
            int damageEnchantCount = 0;
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("sharpness") || enchName.equals("smite") || 
                    enchName.equals("bane_of_arthropods") || enchName.equals("cleaving")) {
                    damageEnchantCount++;
                }
            }
            if (damageEnchantCount > 1) return false;
        }
        
        // 2. Tool Enchantments
        if (itemType.endsWith("_pickaxe") || itemType.endsWith("_shovel") || 
            itemType.endsWith("_axe") || itemType.endsWith("_hoe")) {
            boolean hasSilkTouch = false;
            boolean hasFortune = false;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("silk_touch")) hasSilkTouch = true;
                if (enchName.equals("fortune")) hasFortune = true;
            }
            
            if (hasSilkTouch && hasFortune) return false;
        }
        
        // 3. Armor Protection Enchantments
        if (itemType.endsWith("_helmet") || itemType.endsWith("_chestplate") || 
            itemType.endsWith("_leggings") || itemType.endsWith("_boots")) {
            int protectionCount = 0;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("protection") || enchName.equals("fire_protection") || 
                    enchName.equals("blast_protection") || enchName.equals("projectile_protection")) {
                    protectionCount++;
                }
            }
            
            if (protectionCount > 1) return false;
        }
        
        // 4. Boot Enchantments
        if (itemType.endsWith("_boots")) {
            boolean hasDepthStrider = false;
            boolean hasFrostWalker = false;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("depth_strider")) hasDepthStrider = true;
                if (enchName.equals("frost_walker")) hasFrostWalker = true;
            }
            
            if (hasDepthStrider && hasFrostWalker) return false;
        }
        
        // 5. Bow Enchantments
        if (material == Material.BOW) {
            boolean hasInfinity = false;
            boolean hasMending = false;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("infinity")) hasInfinity = true;
                if (enchName.equals("mending")) hasMending = true;
            }
            
            // Special condition: allow both only if source item already has both
            if (hasInfinity && hasMending) {
                boolean sourceHasBoth = false;
                Map<Enchantment, Integer> sourceEnchants = sourceItem.getEnchantments();
                boolean sourceHasInfinity = false;
                boolean sourceHasMending = false;
                
                for (Enchantment ench : sourceEnchants.keySet()) {
                    String enchName = getEnchantmentName(ench);
                    if (enchName.equals("infinity")) sourceHasInfinity = true;
                    if (enchName.equals("mending")) sourceHasMending = true;
                }
                
                sourceHasBoth = sourceHasInfinity && sourceHasMending;
                if (!sourceHasBoth) return false;
            }
        }
        
        // 6. Crossbow Enchantments
        if (material == Material.CROSSBOW) {
            boolean hasMultishot = false;
            boolean hasPiercing = false;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("multishot")) hasMultishot = true;
                if (enchName.equals("piercing")) hasPiercing = true;
            }
            
            if (hasMultishot && hasPiercing) return false;
        }
        
        // 7. Trident Enchantments
        if (material == Material.TRIDENT) {
            boolean hasRiptide = false;
            boolean hasLoyalty = false;
            boolean hasChanneling = false;
            
            for (Enchantment ench : enchants.keySet()) {
                String enchName = getEnchantmentName(ench);
                if (enchName.equals("riptide")) hasRiptide = true;
                if (enchName.equals("loyalty")) hasLoyalty = true;
                if (enchName.equals("channeling")) hasChanneling = true;
            }
            
            if (hasRiptide && (hasLoyalty || hasChanneling)) return false;
        }
        
        // All checks passed
        return true;
    }
    
    /**
     * Gets the standard name of an enchantment
     * @param enchantment The enchantment
     * @return The standard name of the enchantment
     */
    private static String getEnchantmentName(Enchantment enchantment) {
        try {
            NamespacedKey key = enchantment.getKey();
            return key.getKey().toLowerCase();
        } catch (Exception e) {
            // Fallback for older versions
            return enchantment.getName().toLowerCase();
        }
    }

    private static void stripAll(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        for (Enchantment ench : item.getEnchantments().keySet()) {
            meta.removeEnchant(ench);
        }
        item.setItemMeta(meta);
    }

    private static String classifyItem(Material mat) {
        String name = mat.name().toLowerCase();
        if (mat == Material.PAPER) {
            return "PAPER";
        }
        if (name.endsWith("_helmet") || name.endsWith("_chestplate")
                || name.endsWith("_leggings") || name.endsWith("_boots")) {
            return "ARMOR";
        }
        if (name.endsWith("_sword") || name.endsWith("_axe")) {
            return "WEAPON";
        }
        if (name.endsWith("_pickaxe") || name.endsWith("_shovel")
                || name.endsWith("_hoe") || name.equals("bow")
                || name.equals("crossbow") || name.equals("trident")
                || name.equals("fishing_rod") || name.contains("shears")) {
            return "TOOL";
        }
        return "OTHER";
    }
}