package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpopCommand implements CommandExecutor {

    private static final String PERMISSION = "ultimatedonutsmp.helpop";

    private final UltimateDonutSmp plugin;

    public HelpopCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "HELPOP.PLAYER_ONLY",
                    "&cOnly players can use helpop."
            )));
            return true;
        }

        if (!PermissionUtils.has(player, PERMISSION)) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "HELPOP.NO_PERMISSION",
                    "&cYou do not have permission to request staff assistance."
            )));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "HELPOP.USAGE",
                    "&cUsage: /helpop <message>"
            )));
            return true;
        }

        String message = String.join(" ", args);

        String formatted = plugin.getConfigManager().getMessageOrDefault(
                "HELPOP.ALERT",
                "&9[Helpop] &f{player}&9: &7{message}"
        ).replace("{player}", player.getName()).replace("{message}", message);

        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("ultimatedonutsmp.staff.mode")) {
                staff.sendMessage(ColorUtils.toComponent(formatted));
            }
        }

        player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                "HELPOP.SENT",
                "&aYour help request has been sent to staff."
        )));
        return true;
    }
}
