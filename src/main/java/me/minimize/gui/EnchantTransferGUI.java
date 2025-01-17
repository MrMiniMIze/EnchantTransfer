package me.minimize.gui;

import me.minimize.EnchantTransferPlugin;
import me.minimize.manager.EnchantTransferManager;
import me.minimize.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

public class EnchantTransferGUI implements Listener {

    private static EnchantTransferPlugin plugin;
    private static String guiTitle;
    private static int guiSize;

    public EnchantTransferGUI(EnchantTransferPlugin pluginInstance) {
        plugin = pluginInstance;
        guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&bPaper Enchant Transfer"));
        guiSize = plugin.getConfig().getInt("gui.size", 9);
    }

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, guiSize, guiTitle);

        fillBackground(inv);

        int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
        inv.setItem(infoSlot, createInfoItem(player, null));

        int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);
        inv.setItem(confirmSlot, createConfirmItem(player, null));

        player.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ConfigurationSection bgSec = plugin.getConfig().getConfigurationSection("background");
        Material bgMat = Material.BLACK_STAINED_GLASS_PANE;
        if (bgSec != null) {
            try {
                bgMat = Material.valueOf(bgSec.getString("material", "BLACK_STAINED_GLASS_PANE").toUpperCase());
            } catch (Exception ignored) {}
        }
        ItemStack background = new ItemStack(bgMat);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null && bgSec != null) {
            String name = bgSec.getString("name", "&r");
            bgMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            bgMeta.setLore(bgSec.getStringList("lore"));
            background.setItemMeta(bgMeta);
        }

        int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
        int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);
        int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
        int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);

        for (int i = 0; i < inv.getSize(); i++) {
            if (i == item1Slot || i == item2Slot || i == infoSlot || i == confirmSlot) {
                continue;
            }
            inv.setItem(i, background);
        }
    }

    private static ItemStack createInfoItem(Player player, ItemStack item1) {
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("info-item");
        Material mat = Material.PAPER;
        if (cs != null) {
            try {
                mat = Material.valueOf(cs.getString("material", "PAPER").toUpperCase());
            } catch (Exception ignored) {}
        }

        ItemStack info = new ItemStack(mat);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            String name = MessageUtil.getMsg("info-item-name");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            int enchantCount = EnchantTransferManager.getTotalEnchants(item1);
            double cost = EnchantTransferManager.calculateTotalCost(player, item1);

            meta.setLore(MessageUtil.getList("info-item-lore",
                    "%count%", String.valueOf(enchantCount),
                    "%cost%", String.valueOf(cost))
            );
            info.setItemMeta(meta);
        }
        return info;
    }

    private static ItemStack createConfirmItem(Player player, ItemStack item1) {
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("confirm-item");
        Material mat = Material.EMERALD;
        if (cs != null) {
            try {
                mat = Material.valueOf(cs.getString("material", "EMERALD").toUpperCase());
            } catch (Exception ignored) {}
        }

        ItemStack confirm = new ItemStack(mat);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            String name = MessageUtil.getMsg("confirm-button-name");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            double cost = (item1 != null)
                    ? EnchantTransferManager.calculateTotalCost(player, item1)
                    : 0.0;

            meta.setLore(MessageUtil.getList("confirm-button-lore",
                    "%cost%", String.valueOf(cost))
            );
            confirm.setItemMeta(meta);
        }
        return confirm;
    }

    /**
     * We ONLY allow paper in item1 and item2.
     */
    private static boolean isAllowedPaper(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return (item.getType() == Material.PAPER);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(guiTitle)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // If they clicked their own bottom inventory, do nothing
        if (!clickedInv.equals(topInv)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
        int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);
        int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
        int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);

        if (slot == confirmSlot) {
            event.setCancelled(true);

            ItemStack item1 = topInv.getItem(item1Slot);
            ItemStack item2 = topInv.getItem(item2Slot);

            boolean success = EnchantTransferManager.handleTransfer(player, item1, item2);
            if (success) {
                topInv.setItem(item1Slot, null);
                topInv.setItem(item2Slot, null);

                if (item1 != null && item1.getType() != Material.AIR) {
                    giveOrDrop(player, item1);
                }
                if (item2 != null && item2.getType() != Material.AIR) {
                    giveOrDrop(player, item2);
                }

                if (plugin.getConfig().getBoolean("close-on-success", true)) {
                    player.closeInventory();
                }
            }
        }
        else if (slot == infoSlot || isBackgroundSlot(slot, item1Slot, item2Slot, infoSlot, confirmSlot)) {
            event.setCancelled(true);
        }
        else {
            // They clicked item1Slot or item2Slot
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack newItem1 = topInv.getItem(item1Slot);
                ItemStack newItem2 = topInv.getItem(item2Slot);

                // If item1 not paper, revert
                if (newItem1 != null && !isAllowedPaper(newItem1)) {
                    topInv.setItem(item1Slot, null);
                    giveOrDrop(player, newItem1);
                    player.sendMessage(ChatColor.RED + "Only PAPER is allowed here!");
                }
                // If item2 not paper, revert
                if (newItem2 != null && !isAllowedPaper(newItem2)) {
                    topInv.setItem(item2Slot, null);
                    giveOrDrop(player, newItem2);
                    player.sendMessage(ChatColor.RED + "Only PAPER is allowed here!");
                }

                // Re-fetch item1
                newItem1 = topInv.getItem(item1Slot);

                topInv.setItem(infoSlot, createInfoItem(player, newItem1));
                topInv.setItem(confirmSlot, createConfirmItem(player, newItem1));
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(guiTitle)) return;

        Inventory topInv = event.getView().getTopInventory();
        boolean draggingOverTop = event.getRawSlots().stream()
                .anyMatch(slotId -> slotId < topInv.getSize());

        if (!draggingOverTop) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            HumanEntity he = event.getWhoClicked();
            if (he instanceof Player) {
                Player player = (Player) he;
                int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
                int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);
                int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
                int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);

                ItemStack newItem1 = topInv.getItem(item1Slot);
                ItemStack newItem2 = topInv.getItem(item2Slot);

                if (newItem1 != null && !isAllowedPaper(newItem1)) {
                    topInv.setItem(item1Slot, null);
                    giveOrDrop(player, newItem1);
                    player.sendMessage(ChatColor.RED + "Only PAPER is allowed here!");
                }
                if (newItem2 != null && !isAllowedPaper(newItem2)) {
                    topInv.setItem(item2Slot, null);
                    giveOrDrop(player, newItem2);
                    player.sendMessage(ChatColor.RED + "Only PAPER is allowed here!");
                }

                newItem1 = topInv.getItem(item1Slot);
                topInv.setItem(infoSlot, createInfoItem(player, newItem1));
                topInv.setItem(confirmSlot, createConfirmItem(player, newItem1));
            }
        }, 1L);
    }

    private boolean isBackgroundSlot(int slot, int item1Slot, int item2Slot,
                                     int infoSlot, int confirmSlot) {
        return (slot != item1Slot && slot != item2Slot
                && slot != infoSlot && slot != confirmSlot);
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(st ->
                    player.getWorld().dropItemNaturally(player.getLocation(), st)
            );
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full; item dropped at your feet.");
        }
    }
}
