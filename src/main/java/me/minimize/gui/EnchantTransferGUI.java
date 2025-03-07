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
import org.bukkit.event.*;
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
        // Read GUI title & size from config
        guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&bEnchant Transfer"));
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

    /**
     * Fills all non-special slots with a background item from config.
     * Supports custom-model-data for the background if defined.
     */
    private static void fillBackground(Inventory inv) {
        ConfigurationSection bgSec = plugin.getConfig().getConfigurationSection("background");
        Material bgMat = Material.BLACK_STAINED_GLASS_PANE;
        if (bgSec != null) {
            try {
                bgMat = Material.valueOf(bgSec.getString("material", "BLACK_STAINED_GLASS_PANE").toUpperCase());
            } catch (Exception ignored) {}
        }
        ItemStack background = new ItemStack(bgMat);

        if (bgSec != null) {
            ItemMeta bgMeta = background.getItemMeta();
            if (bgMeta != null) {
                // name
                String name = bgSec.getString("name", "&r");
                bgMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                // lore
                bgMeta.setLore(bgSec.getStringList("lore"));
                // custom model data
                if (bgSec.contains("custom-model-data")) {
                    bgMeta.setCustomModelData(bgSec.getInt("custom-model-data"));
                }
                background.setItemMeta(bgMeta);
            }
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

    /**
     * Creates the info (paper) item from config, with optional custom-model-data.
     */
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

            int enchantCount = (item1 == null || item1.getType().isAir())
                    ? 0 : EnchantTransferManager.getTotalEnchants(item1);
            double cost = (item1 == null || item1.getType().isAir())
                    ? 0.0 : EnchantTransferManager.calculateTotalCost(player, item1);

            meta.setLore(MessageUtil.getList("info-item-lore",
                    "%count%", String.valueOf(enchantCount),
                    "%cost%", String.valueOf(cost))
            );

            // custom-model-data if present
            if (cs != null && cs.contains("custom-model-data")) {
                meta.setCustomModelData(cs.getInt("custom-model-data"));
            }

            info.setItemMeta(meta);
        }
        return info;
    }

    /**
     * Creates the confirm (emerald) item from config, with optional custom-model-data.
     */
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

            double cost = (item1 == null || item1.getType().isAir())
                    ? 0.0 : EnchantTransferManager.calculateTotalCost(player, item1);

            meta.setLore(MessageUtil.getList("confirm-button-lore",
                    "%cost%", String.valueOf(cost))
            );

            // custom-model-data if present
            if (cs != null && cs.contains("custom-model-data")) {
                meta.setCustomModelData(cs.getInt("custom-model-data"));
            }

            confirm.setItemMeta(meta);
        }
        return confirm;
    }

    /**
     * We allow paper or armor/weapons/tools in item1 & item2.
     * No random items like dirt, etc.
     */
    private static boolean isAllowedPaperOrArmorTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material mat = item.getType();
        String name = mat.name().toLowerCase();

        if (mat == Material.PAPER) {
            return true;
        }
        // armor
        if (name.endsWith("_helmet") || name.endsWith("_chestplate")
                || name.endsWith("_leggings") || name.endsWith("_boots")) {
            return true;
        }
        // weapons
        if (name.endsWith("_sword") || name.endsWith("_axe")) {
            return true;
        }
        // tools
        if (name.endsWith("_pickaxe") || name.endsWith("_shovel")
                || name.endsWith("_hoe") || name.equals("bow")
                || name.equals("crossbow") || name.equals("trident")
                || name.equals("fishing_rod") || name.contains("shears")) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if it's our custom GUI
        if (!event.getView().getTitle().equals(guiTitle)) return;
        if (event.getClickedInventory() == null) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
        int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);
        int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
        int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);

        // if clicking top inventory
        if (clickedInv.equals(topInv)) {
            // SHIFT-click in top
            if (event.getClick().isShiftClick()) {
                // only allow shift-click if slot is item1 or item2
                if (slot != item1Slot && slot != item2Slot) {
                    event.setCancelled(true);
                    return;
                }
            }

            // if info slot or background => block
            if (slot == infoSlot || isBackgroundSlot(slot, item1Slot, item2Slot, infoSlot, confirmSlot)) {
                event.setCancelled(true);
                return;
            }

            // if confirm
            if (slot == confirmSlot) {
                event.setCancelled(true);

                ItemStack item1 = topInv.getItem(item1Slot);
                ItemStack item2 = topInv.getItem(item2Slot);

                // if item1 or item2 is null => no crash
                if (item1 == null || item1.getType().isAir()
                        || item2 == null || item2.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + "Please place valid items before confirming!");
                    return;
                }

                boolean success = EnchantTransferManager.handleTransfer(player, item1, item2);
                if (success) {
                    topInv.setItem(item1Slot, null);
                    topInv.setItem(item2Slot, null);

                    if (!item1.getType().isAir()) {
                        giveOrDrop(player, item1);
                    }
                    if (!item2.getType().isAir()) {
                        giveOrDrop(player, item2);
                    }

                    if (plugin.getConfig().getBoolean("close-on-success", true)) {
                        player.closeInventory();
                    }
                }
            }
            else {
                // clicked item1 or item2
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    handlePostClickUpdate(player, topInv);
                }, 1L);
            }
        }
        else {
            // clicking bottom inventory
            // if SHIFT-click => item might move to item1 or item2 => re-check
            if (event.getClick().isShiftClick()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    handlePostClickUpdate(player, topInv);
                }, 1L);
            }
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
                handlePostClickUpdate((Player) he, topInv);
            }
        }, 1L);
    }

    private static void handlePostClickUpdate(Player player, Inventory topInv) {
        if (topInv == null) return;
        if (!player.getOpenInventory().getTopInventory().equals(topInv)) {
            return; // maybe they closed the GUI
        }

        int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
        int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);
        int infoSlot = plugin.getConfig().getInt("gui.slot-info", 4);
        int confirmSlot = plugin.getConfig().getInt("gui.slot-confirm", 7);

        ItemStack newItem1 = topInv.getItem(item1Slot);
        ItemStack newItem2 = topInv.getItem(item2Slot);

        // if item1 is disallowed
        if (newItem1 != null && !newItem1.getType().isAir()
                && !isAllowedPaperOrArmorTool(newItem1)) {
            topInv.setItem(item1Slot, null);
            giveOrDrop(player, newItem1);
            player.sendMessage(MessageUtil.getMsg("disallowed-item"));
        }
        // if item2 is disallowed
        if (newItem2 != null && !newItem2.getType().isAir()
                && !isAllowedPaperOrArmorTool(newItem2)) {
            topInv.setItem(item2Slot, null);
            giveOrDrop(player, newItem2);
            player.sendMessage(MessageUtil.getMsg("disallowed-item"));
        }

        // re-fetch
        newItem1 = topInv.getItem(item1Slot);
        newItem2 = topInv.getItem(item2Slot);

        // enforce paper->paper or normal->normal
        boolean item1Paper = (newItem1 != null && newItem1.getType() == Material.PAPER);
        boolean item2Paper = (newItem2 != null && newItem2.getType() == Material.PAPER);

        if (item1Paper && newItem2 != null && !item2Paper) {
            topInv.setItem(item2Slot, null);
            giveOrDrop(player, newItem2);
            player.sendMessage(ChatColor.RED + "Paper can only be transferred to another paper!");
        }
        else if (!item1Paper && item1IsValid(newItem1) && item2Paper && newItem2 != null) {
            topInv.setItem(item2Slot, null);
            giveOrDrop(player, newItem2);
            player.sendMessage(ChatColor.RED + "This item can't be transferred onto paper!");
        }

        // finally, update info & confirm
        newItem1 = topInv.getItem(item1Slot);
        topInv.setItem(infoSlot, createInfoItem(player, newItem1));
        topInv.setItem(confirmSlot, createConfirmItem(player, newItem1));
    }

    private boolean isBackgroundSlot(int slot, int item1Slot, int item2Slot,
                                     int infoSlot, int confirmSlot) {
        return (slot != item1Slot && slot != item2Slot
                && slot != infoSlot && slot != confirmSlot);
    }

    private static boolean item1IsValid(ItemStack item) {
        return (item != null && !item.getType().isAir());
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(st ->
                    player.getWorld().dropItemNaturally(player.getLocation(), st)
            );
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full; leftover items dropped at your feet.");
        }
    }
}
