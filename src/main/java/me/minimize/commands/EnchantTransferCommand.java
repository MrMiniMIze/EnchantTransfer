package me.minimize.commands;

import me.minimize.EnchantTransferPlugin;
import me.minimize.gui.EnchantTransferGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class EnchantTransferCommand implements CommandExecutor, TabCompleter {

    private final EnchantTransferPlugin plugin;

    public EnchantTransferCommand(EnchantTransferPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        // Permission check
        if (!player.hasPermission("enchanttransfer.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /etransfer.");
            return true;
        }

        // Open the GUI
        EnchantTransferGUI.openGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String label, String[] args) {
        return Collections.emptyList();
    }
}
