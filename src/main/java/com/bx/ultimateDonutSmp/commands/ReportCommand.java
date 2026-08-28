package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReportCommand implements TabExecutor {

    private static final String PERMISSION = "ultimatedonutsmp.report";

    private final UltimateDonutSmp plugin;

    public ReportCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "REPORT.PLAYER_ONLY",
                    "&cOnly players can use report."
            )));
            return true;
        }

        Player reporter = (Player) sender;
        if (!PermissionUtils.has(reporter, PERMISSION)) {
            reporter.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "REPORT.NO_PERMISSION",
                    "&cYou do not have permission to report players."
            )));
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "REPORT.USAGE",
                    "&cUsage: /report <player> <reason>"
            )));
            return true;
        }

        Player reported = plugin.getHideManager().findOnlinePlayer(reporter, args[0]);

        if (reported == null) {
            reporter.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                    "REPORT.PLAYER_NOT_FOUND",
                    "&cPlayer not found."
            )));
            return true;
        }

        String message = joinArgs(args, 1);

        String formatted = plugin.getConfigManager().getMessageOrDefault(
                "REPORT.ALERT",
                "&c[Report] &f{reporter}&c reported &f{reported}&c: &7{message}"
        ).replace("{reporter}", reporter.getName())
                .replace("{reported}", reported.getName())
                .replace("{message}", message);

        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("ultimatedonutsmp.staff.mode")) {
                staff.sendMessage(ColorUtils.toComponent(formatted));
            }
        }

        reporter.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                "REPORT.SENT",
                "&aYour report has been submitted to staff."
        )));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !PermissionUtils.has(sender, PERMISSION)) {
            return java.util.Collections.emptyList();
        }

        String input = args[0].toLowerCase();
        List<String> suggestions = new ArrayList<>();
        for (String name : plugin.getHideManager().onlineNames(sender)) {
            Player player = plugin.getHideManager().findOnlinePlayer(sender, name);
            if (player == null
                    || sender instanceof Player && ((Player) sender).getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (name.toLowerCase().startsWith(input)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
