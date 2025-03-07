// YamlLoader.java
package me.minimize.util;

import me.minimize.EnchantTransferPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlLoader {

    private static FileConfiguration messagesConfig;

    public static FileConfiguration getMessagesYaml() {
        if (messagesConfig == null) {
            loadMessages();
        }
        return messagesConfig;
    }

    private static void loadMessages() {
        EnchantTransferPlugin plugin = EnchantTransferPlugin.getInstance();
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void reloadMessages() {
        messagesConfig = null;
        loadMessages();
        MessageUtil.forceReload();
    }
}
