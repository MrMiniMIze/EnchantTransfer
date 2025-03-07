// MessageUtil.java
package me.minimize.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MessageUtil {

    private static FileConfiguration messages;

    private static void loadMessages() {
        if (messages == null) {
            messages = YamlLoader.getMessagesYaml();
        }
    }

    public static String getMsg(String path) {
        loadMessages();
        if (!messages.contains(path)) {
            return ChatColor.RED + "Missing message: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', messages.getString(path, path));
    }

    public static List<String> getList(String path, String... replacements) {
        loadMessages();
        List<String> rawList = messages.getStringList(path);
        if (rawList.isEmpty()) {
            rawList.add("&cMissing list: " + path);
        }
        List<String> translated = new ArrayList<>();
        for (String line : rawList) {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }
            translated.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return translated;
    }

    public static void forceReload() {
        messages = null;
        loadMessages();
    }
}
