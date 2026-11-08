package de.huskyright.worldtimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldCommands implements CommandExecutor, TabCompleter {

    private final TimerPlugin plugin;
    private final WorldManager worldManager;

    public WorldCommands(TimerPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "Dazu hast du keine Rechte!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reset":
                return handleReset(sender);
            case "rollback":
                return handleRollback(sender);
            case "save":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /world save <Name>");
                    return true;
                }
                return handleSave(sender, args[1]);
            case "load":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /world load <Name>");
                    return true;
                }
                return handleLoad(sender, args[1]);
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /world delete <Name>");
                    return true;
                }
                return handleDelete(sender, args[1]);
            case "list":
                return handleList(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReset(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "[WorldTimer] Welt-Reset eingeleitet...");
        worldManager.scheduleReset(sender);
        return true;
    }

    private boolean handleRollback(CommandSender sender) {
        if (!worldManager.hasRollback()) {
            sender.sendMessage(ChatColor.RED + "[WorldTimer] Es existiert noch kein Rollback-Speicherpunkt!");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "[WorldTimer] Rollback wird vorbereitet...");
        worldManager.scheduleRollback(sender);
        return true;
    }

    private boolean handleSave(CommandSender sender, String name) {
        if (name.equalsIgnoreCase("_rollback") || !name.matches("^[a-zA-Z0-9_-]+$")) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Preset-Name! Bitte nur Buchstaben, Zahlen, '-' und '_' verwenden.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Speichere Welt und Spielerdaten unter dem Preset '" + ChatColor.GOLD + name + ChatColor.YELLOW + "'...");
        boolean success = worldManager.saveWorldState(name, sender.getName(), false);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Welt-Preset '" + ChatColor.YELLOW + name + ChatColor.GREEN + "' wurde erfolgreich gespeichert!");
        } else {
            sender.sendMessage(ChatColor.RED + "Fehler beim Speichern des Presets '" + name + "'. Siehe Server-Konsole.");
        }
        return true;
    }

    private boolean handleLoad(CommandSender sender, String name) {
        boolean success = worldManager.scheduleLoad(name, sender);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "[WorldTimer] Preset '" + name + "' wird geladen...");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String name) {
        boolean success = worldManager.deletePreset(name);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Preset '" + ChatColor.YELLOW + name + ChatColor.GREEN + "' wurde erfolgreich gelöscht.");
        } else {
            sender.sendMessage(ChatColor.RED + "Preset '" + name + "' konnte nicht gefunden oder gelöscht werden.");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<WorldPreset> presets = worldManager.listPresets();
        sender.sendMessage(ChatColor.GOLD + "========== [ Gespeicherte Welt-Presets ] ==========");

        if (presets.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Keine Presets gespeichert. Nutze " + ChatColor.YELLOW + "/world save <Name>" + ChatColor.GRAY + ", um eines zu erstellen.");
        } else {
            int index = 1;
            for (WorldPreset preset : presets) {
                sender.sendMessage(ChatColor.YELLOW + "" + index + ". " + ChatColor.WHITE + preset.getName() +
                        ChatColor.GRAY + " (" + ChatColor.AQUA + preset.getFormattedDate() + ChatColor.GRAY + ") " +
                        ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Timer: " + ChatColor.GREEN + preset.getFormattedTimer() +
                        ChatColor.DARK_GRAY + " (" + preset.getSavedBy() + ")");
                index++;
            }
        }

        if (worldManager.hasRollback()) {
            sender.sendMessage(ChatColor.DARK_AQUA + "Rollback-Punkt verfügbar! " + ChatColor.GRAY + "Nutze " + ChatColor.YELLOW + "/world rollback" + ChatColor.GRAY + ", um den Stand vor dem Reset wiederherzustellen.");
        }
        sender.sendMessage(ChatColor.GOLD + "===================================================");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== [ WorldTimer: /world Befehle ] ==========");
        sender.sendMessage(ChatColor.YELLOW + "/world reset " + ChatColor.GRAY + "- Generiert Overworld, Nether und End neu (+ Inventar-Reset, inkl. Auto-Rollback)");
        sender.sendMessage(ChatColor.YELLOW + "/world rollback " + ChatColor.GRAY + "- Lädt die Welt vor dem letzten Reset + Items wieder hoch");
        sender.sendMessage(ChatColor.YELLOW + "/world save <Name> " + ChatColor.GRAY + "- Speichert die aktuelle Welt + Items manuell");
        sender.sendMessage(ChatColor.YELLOW + "/world load <Name> " + ChatColor.GRAY + "- Lädt einen gespeicherten Stand wieder hoch");
        sender.sendMessage(ChatColor.YELLOW + "/world delete <Name> " + ChatColor.GRAY + "- Löscht ein gespeichertes Preset");
        sender.sendMessage(ChatColor.YELLOW + "/world list " + ChatColor.GRAY + "- Zählt alle Presets auf (nach Speicherdatum sortiert)");
        sender.sendMessage(ChatColor.GOLD + "===================================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("worldtimer.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = List.of("reset", "rollback", "save", "load", "delete", "list");
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("delete")) {
                List<WorldPreset> presets = worldManager.listPresets();
                for (WorldPreset p : presets) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
                return completions;
            }
        }
        return Collections.emptyList();
    }
}

