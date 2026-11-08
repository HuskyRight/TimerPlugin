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
            case "exclude":
                return handleExcludeCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl. Verwendung: /timer <on|off|start|stop|reset|exclude>");
                break;
        }
        plugin.checkConditionsAndPause();
        return true;
    }

    private boolean handleExcludeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /timer exclude <Spieler|list|add|remove>");
            return true;
        }

        String sub = args[1];
        if (sub.equalsIgnoreCase("list")) {
            java.util.Set<String> excluded = plugin.getExcludedPlayers();
            if (excluded.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Es sind derzeit keine Spieler vom Timer ausgeschlossen.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "=== Vom Timer ausgeschlossene Spieler ===");
                for (String name : excluded) {
                    sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + name);
                }
            }
            return true;
        }

        if (sub.equalsIgnoreCase("add")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Verwendung: /timer exclude add <Spieler>");
                return true;
            }
            String target = args[2];
            if (plugin.addExcludedPlayer(target)) {
                sender.sendMessage(ChatColor.GREEN + "Spieler '" + ChatColor.YELLOW + target + ChatColor.GREEN + "' wurde zur Ausschlussliste hinzugefügt (kann sich bei Pause frei bewegen).");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Spieler '" + target + "' ist bereits ausgeschlossen.");
            }
            return true;
        }

        if (sub.equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Verwendung: /timer exclude remove <Spieler>");
                return true;
            }
            String target = args[2];
            if (plugin.removeExcludedPlayer(target)) {
                sender.sendMessage(ChatColor.GREEN + "Spieler '" + ChatColor.YELLOW + target + ChatColor.GREEN + "' wurde von der Ausschlussliste entfernt.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Spieler '" + target + "' war nicht ausgeschlossen.");
            }
            return true;
        }

        // Direct toggle: /timer exclude <username>
        String target = sub;
        if (plugin.isPlayerExcluded(target)) {
            plugin.removeExcludedPlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Spieler '" + ChatColor.YELLOW + target + ChatColor.GREEN + "' ist nun nicht mehr vom Timer befreit.");
        } else {
            plugin.addExcludedPlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Spieler '" + ChatColor.YELLOW + target + ChatColor.GREEN + "' wurde vom Timer befreit (kann sich bei Pause frei bewegen).");
        }
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
                List<String> subs = List.of("on", "off", "start", "stop", "reset", "exclude");
                List<String> result = new ArrayList<>();
                for (String s : subs) {
                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                        result.add(s);
                    }
                }
                return result;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("exclude")) {
                List<String> options = new ArrayList<>(List.of("list", "add", "remove"));
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    options.add(p.getName());
                }
                List<String> result = new ArrayList<>();
                for (String opt : options) {
                    if (opt.toLowerCase().startsWith(args[1].toLowerCase())) {
                        result.add(opt);
                    }
                }
                return result;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("exclude")) {
                if (args[1].equalsIgnoreCase("add")) {
                    List<String> result = new ArrayList<>();
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (!plugin.isPlayerExcluded(p) && p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            result.add(p.getName());
                        }
                    }
                    return result;
                } else if (args[1].equalsIgnoreCase("remove")) {
                    List<String> result = new ArrayList<>();
                    for (String name : plugin.getExcludedPlayers()) {
                        if (name.toLowerCase().startsWith(args[2].toLowerCase())) {
                            result.add(name);
                        }
                    }
                    return result;
                }
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
