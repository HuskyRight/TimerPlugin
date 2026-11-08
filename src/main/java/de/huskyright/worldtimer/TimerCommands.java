package de.huskyright.worldtimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimerCommands implements CommandExecutor, TabCompleter {

    private final TimerPlugin plugin;

    public TimerCommands(TimerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("timer")) {
            return handleTimerCommand(sender, args);
        }
        if (command.getName().equalsIgnoreCase("timermode")) {
            return handleTimerModeCommand(sender, args);
        }
        return false;
    }

    private boolean handleTimerCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /timer <on|off|start|stop|reset>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                plugin.getTimerBossBar().setVisible(true);
                plugin.updateTimerDisplay();
                sender.sendMessage(ChatColor.GREEN + "Timer wird nun angezeigt.");
                break;
            case "off":
                plugin.getTimerBossBar().setVisible(false);
                plugin.setTimeElapsedSeconds(0);
                plugin.setTimerRunning(false);
                plugin.updateTimerDisplay();
                sender.sendMessage(ChatColor.GREEN + "Timer wurde ausgeblendet und zurückgesetzt.");
                break;
            case "start":
                if (plugin.isTimerRunning()) {
                    sender.sendMessage(ChatColor.RED + "Der Timer läuft bereits!");
                } else {
                    plugin.setTimerRunning(true);
                    sender.sendMessage(ChatColor.GREEN + "Timer gestartet!");
                }
                break;
            case "stop":
                 if (!plugin.isTimerRunning()) {
                    sender.sendMessage(ChatColor.RED + "Der Timer ist bereits gestoppt!");
                } else {
                    plugin.setTimerRunning(false);
                    sender.sendMessage(ChatColor.YELLOW + "Timer gestoppt!");
                }
                break;
            case "reset":
                plugin.setTimeElapsedSeconds(0);
                plugin.updateTimerDisplay();
                sender.sendMessage(ChatColor.GREEN + "Timer wurde zurückgesetzt.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl. Verwendung: /timer <on|off|start|stop|reset>");
                break;
        }
        plugin.checkConditionsAndPause();
        return true;
    }

    private boolean handleTimerModeCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /timermode set <Anzahl>");
            return true;
        }

        try {
            int numPlayers = Integer.parseInt(args[1]);
            if (numPlayers <= 0) {
                sender.sendMessage(ChatColor.RED + "Die Spieleranzahl muss positiv sein.");
                return true;
            }
            plugin.setRequiredPlayers(numPlayers);
            sender.sendMessage(ChatColor.GREEN + "Timer-Modus auf " + numPlayers + " Spieler gesetzt.");
            plugin.checkConditionsAndPause();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Das ist keine gültige Zahl.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("timer")) {
            if (args.length == 1) {
                List<String> subs = List.of("on", "off", "start", "stop", "reset");
                List<String> result = new ArrayList<>();
                for (String s : subs) {
                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                        result.add(s);
                    }
                }
                return result;
            }
        } else if (command.getName().equalsIgnoreCase("timermode")) {
            if (args.length == 1) {
                if ("set".startsWith(args[0].toLowerCase())) {
                    return List.of("set");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                return List.of("1", "2", "3", "4");
            }
        }
        return Collections.emptyList();
    }
}
