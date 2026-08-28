package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.menus.MediaMenu;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SocialCommand implements CommandExecutor {

    private final UltimateDonutSmp plugin;

    public SocialCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Player only."); return true; }
        Player player = (Player) sender;
        if (!plugin.getConfigManager().isCommandEnabled("SOCIAL")) {
            player.sendMessage(ColorUtils.toComponent("&cSocial commands are currently disabled."));
            return true;
        }

        String normalizedLabel = label.toLowerCase();
        if (normalizedLabel.equals("media")) {
            new MediaMenu(plugin).open(player);
            return true;
        }

        String key;
        switch (normalizedLabel) {
            case "twitter":
                key = "SOCIAL.TWITTER";
                break;
            case "store":
                key = "SOCIAL.STORE";
                break;
            default:
                key = null;
                break;
        }

        if (key != null) {
            List<String> lines = plugin.getConfigManager().getMessages().getStringList(key);
            for (String line : lines) player.sendMessage(ColorUtils.toComponent(line));
        } else {
            // /social - show all
            for (String section : new java.util.ArrayList<>(java.util.Arrays.asList("SOCIAL.TWITTER",  "SOCIAL.STORE"))) {
                List<String> lines = plugin.getConfigManager().getMessages().getStringList(section);
                for (String line : lines) player.sendMessage(ColorUtils.toComponent(line));
            }
        }
        return true;
    }
}
