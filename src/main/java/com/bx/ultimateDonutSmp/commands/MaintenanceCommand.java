package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.MaintenanceManager;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private final UltimateDonutSmp plugin;

    public MaintenanceCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.maintenance")) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to manage maintenance mode."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " <on|off|status|setlobby [server]>"));
            return true;
        }

        MaintenanceManager mm = plugin.getMaintenanceManager();
        if (mm == null) {
            sender.sendMessage(ColorUtils.toComponent("&cMaintenance manager is not available."));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {        case "on": case "start": case "enable": {

                            if (mm.isMaintenanceActive()) {
                                sender.sendMessage(ColorUtils.toComponent("&eMaintenance mode is already active."));
                                return true;
                            }
                            mm.startMaintenance();
                            sender.sendMessage(ColorUtils.toComponent("&aMaintenance mode has been enabled. Players are being redirected."));
                        break;        }        case "off": case "stop": case "disable": {

                            if (!mm.isMaintenanceActive()) {
                                sender.sendMessage(ColorUtils.toComponent("&eMaintenance mode is not active."));
                                return true;
                            }
                            mm.stopMaintenance();
                            sender.sendMessage(ColorUtils.toComponent("&aMaintenance mode has been disabled. Reconnect signal sent."));
                        break;        }        case "status": {

                            boolean active = mm.isMaintenanceActive();
                            String lobby = mm.getLobbyServer();
                            sender.sendMessage(ColorUtils.toComponent("&d&lMaintenance status:"));
                            sender.sendMessage(ColorUtils.toComponent("  &fActive: " + (active ? "&aYes" : "&cNo")));
                            sender.sendMessage(ColorUtils.toComponent("  &fLobby server: &b" + lobby));
                        break;        }        case "setlobby": {

                            if (args.length < 2) {
                                sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " setlobby <server>"));
                                return true;
                            }
                            String lobby = args[1];
                            mm.setLobbyServer(lobby);
                            sender.sendMessage(ColorUtils.toComponent("&aLobby server set to &b" + lobby + "&a."));
                        break;        }        default: sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " <on|off|status|setlobby [server]>"));            break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.maintenance")) {
            return java.util.Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], new java.util.ArrayList<>(java.util.Arrays.asList("on",  "off",  "status",  "setlobby")), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setlobby")) {
            List<String> servers = new ArrayList<>();
            ConfigurationSection sec = plugin.getConfigManager().getNetwork().getConfigurationSection("NETWORK-STATUS.SERVERS");
            if (sec != null) {
                servers.addAll(sec.getKeys(false));
            }
            return StringUtil.copyPartialMatches(args[1], servers, new ArrayList<>());
        }

        return java.util.Collections.emptyList();
    }
}
