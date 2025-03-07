package me.minimize.listeners;

import me.minimize.EnchantTransferPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InventoryCloseListener implements Listener {

    private final EnchantTransferPlugin plugin;

    public InventoryCloseListener(EnchantTransferPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String guiTitle = plugin.getConfig().getString("gui.title", "&bEnchant Transfer")
                .replace('&', '§');
        if (!event.getView().getTitle().equals(guiTitle)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory topInv = event.getView().getTopInventory();

        int item1Slot = plugin.getConfig().getInt("gui.slot-item1", 2);
        int item2Slot = plugin.getConfig().getInt("gui.slot-item2", 6);

        ItemStack item1 = topInv.getItem(item1Slot);
        ItemStack item2 = topInv.getItem(item2Slot);

        if (item1 != null && !item1.getType().isAir()) {
            giveOrDrop(player, item1);
            topInv.setItem(item1Slot, null);
        }
        if (item2 != null && !item2.getType().isAir()) {
            giveOrDrop(player, item2);
            topInv.setItem(item2Slot, null);
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(st ->
                    player.getWorld().dropItemNaturally(player.getLocation(), st)
            );
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full; leftover items dropped at your feet.");
        }
    }
}
