package me.minimize.commands;

import me.minimize.EnchantTransferPlugin;
import me.minimize.gui.EnchantTransferGUI;
import me.minimize.util.MessageUtil;
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

        // /etransfer reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("enchanttransfer.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload this plugin.");
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(MessageUtil.getMsg("reload-complete"));
            return true;
        }

        // /etransfer -> open GUI
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /etransfer for the GUI.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("enchanttransfer.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /etransfer.");
            return true;
        }

        EnchantTransferGUI.openGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("enchanttransfer.reload")) {
                return Collections.singletonList("reload");
            }
        }
        return Collections.emptyList();
    }
}
