package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WorldManager {

    private final TimerPlugin plugin;
    private final File presetsDir;
    private final File pendingActionFile;

    public WorldManager(TimerPlugin plugin) {
        this.plugin = plugin;
        this.presetsDir = new File(plugin.getDataFolder(), "presets");
        this.pendingActionFile = new File(plugin.getDataFolder(), "pending_action.yml");
    }

    public File getPresetsDir() {
        if (!presetsDir.exists()) {
            presetsDir.mkdirs();
        }
        return presetsDir;
    }

    public String getLevelNameFromProperties() {
        File container = plugin.getServer().getWorldContainer();
        File serverProps = new File(container, "server.properties");
        if (serverProps.exists()) {
            try (InputStream in = new FileInputStream(serverProps)) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty("level-name", "world");
            } catch (Exception e) {
                plugin.getLogger().warning("Konnte server.properties nicht lesen, verwende 'world': " + e.getMessage());
            }
        }
        return "world";
    }

    public String getPrimaryWorldName() {
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0).getName();
        }
        return getLevelNameFromProperties();
    }

    public boolean saveWorldState(String presetName, String savedBy, boolean isRollback) {
        try {
            // 1. Force flush of player data and world chunks
            Bukkit.savePlayers();
            for (World world : Bukkit.getWorlds()) {
                world.save();
            }

            File container = plugin.getServer().getWorldContainer();
            String levelName = getPrimaryWorldName();
            String netherName = levelName + "_nether";
            String endName = levelName + "_the_end";

            File targetDir = new File(getPresetsDir(), isRollback ? "_rollback" : presetName);
            if (targetDir.exists()) {
                FileUtils.deleteDirectory(targetDir.toPath());
            }
            targetDir.mkdirs();

            List<String> ignoredFiles = List.of("session.lock", "uid.dat");

            // Copy Overworld
            File worldDir = new File(container, levelName);
            if (worldDir.exists()) {
                FileUtils.copyDirectory(worldDir.toPath(), targetDir.toPath().resolve(levelName), ignoredFiles);
            }

            // Copy Nether
            File netherDir = new File(container, netherName);
            if (netherDir.exists()) {
                FileUtils.copyDirectory(netherDir.toPath(), targetDir.toPath().resolve(netherName), ignoredFiles);
            }

            // Copy End
            File endDir = new File(container, endName);
            if (endDir.exists()) {
                FileUtils.copyDirectory(endDir.toPath(), targetDir.toPath().resolve(endName), ignoredFiles);
            }

            // Save metadata
            File metaFile = new File(targetDir, "meta.yml");
            YamlConfiguration meta = new YamlConfiguration();
            long now = System.currentTimeMillis();
            meta.set("name", presetName);
            meta.set("timestamp", now);
            meta.set("formattedDate", WorldPreset.formatDate(now));
            meta.set("timeElapsedSeconds", plugin.getTimeElapsedSeconds());
            meta.set("timerRunning", plugin.isTimerRunning());
            meta.set("requiredPlayers", plugin.getRequiredPlayers());
            meta.set("savedBy", savedBy);
            meta.save(metaFile);

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Speichern des Presets '" + presetName + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void scheduleReset(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Erstelle automatisches Rollback-Backup vor dem Reset...");
        boolean saved = saveWorldState("_rollback", sender.getName(), true);
        if (!saved) {
            sender.sendMessage(ChatColor.RED + "Warnung: Rollback-Speicherpunkt konnte nicht vollständig gespeichert werden!");
        }

        writePendingAction("RESET", null);

        kickAllPlayers(ChatColor.RED + "Welt wird zurückgesetzt!\n" +
                ChatColor.YELLOW + "Ein Rollback-Speicherpunkt wurde erstellt.\n" +
                ChatColor.GRAY + "Der Server startet neu...");

        restartServer();
    }

    public boolean scheduleRollback(CommandSender sender) {
        File rollbackDir = new File(getPresetsDir(), "_rollback");
        if (!rollbackDir.exists()) {
            sender.sendMessage(ChatColor.RED + "Es existiert kein Rollback-Speicherpunkt!");
            return false;
        }

        writePendingAction("ROLLBACK", null);

        kickAllPlayers(ChatColor.GOLD + "Rollback wird durchgeführt...\n" +
                ChatColor.YELLOW + "Die vorherige Welt und alle Items werden wiederhergestellt.\n" +
                ChatColor.GRAY + "Der Server startet neu...");

        restartServer();
        return true;
    }

    public boolean scheduleLoad(String presetName, CommandSender sender) {
        File presetDir = new File(getPresetsDir(), presetName);
        if (!presetDir.exists() || presetName.equalsIgnoreCase("_rollback")) {
            sender.sendMessage(ChatColor.RED + "Das Preset '" + presetName + "' existiert nicht! Nutze /world list.");
            return false;
        }

        // Safety backup of the current world into rollback
        sender.sendMessage(ChatColor.YELLOW + "Erstelle Sicherheits-Rollback vor dem Laden...");
        saveWorldState("_rollback", sender.getName(), true);

        writePendingAction("LOAD", presetName);

        kickAllPlayers(ChatColor.GOLD + "Preset '" + presetName + "' wird geladen...\n" +
                ChatColor.YELLOW + "Welt und Spielerdaten werden wiederhergestellt.\n" +
                ChatColor.GRAY + "Der Server startet neu...");

        restartServer();
        return true;
    }

    public List<WorldPreset> listPresets() {
        List<WorldPreset> list = new ArrayList<>();
        File dir = getPresetsDir();
        File[] files = dir.listFiles();
        if (files == null) {
            return list;
        }

        for (File file : files) {
            if (!file.isDirectory() || file.getName().startsWith("_")) {
                continue;
            }
            File metaFile = new File(file, "meta.yml");
            if (metaFile.exists()) {
                YamlConfiguration meta = YamlConfiguration.loadConfiguration(metaFile);
                String name = meta.getString("name", file.getName());
                long timestamp = meta.getLong("timestamp", file.lastModified());
                String formattedDate = meta.getString("formattedDate", WorldPreset.formatDate(timestamp));
                long time = meta.getLong("timeElapsedSeconds", 0);
                boolean running = meta.getBoolean("timerRunning", false);
                int players = meta.getInt("requiredPlayers", 1);
                String author = meta.getString("savedBy", "Unbekannt");
                list.add(new WorldPreset(name, timestamp, formattedDate, time, running, players, author));
            } else {
                long lastMod = file.lastModified();
                list.add(new WorldPreset(file.getName(), lastMod, WorldPreset.formatDate(lastMod), 0, false, 1, "Manuell"));
            }
        }

        // Sort by timestamp descending (newest first)
        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return list;
    }

    public boolean deletePreset(String presetName) {
        if (presetName == null || presetName.equalsIgnoreCase("_rollback") || presetName.contains("..")) {
            return false;
        }
        File presetDir = new File(getPresetsDir(), presetName);
        if (!presetDir.exists() || !presetDir.isDirectory()) {
            return false;
        }
        try {
            FileUtils.deleteDirectory(presetDir.toPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Löschen des Presets '" + presetName + "': " + e.getMessage());
            return false;
        }
    }

    public boolean hasRollback() {
        return new File(getPresetsDir(), "_rollback").exists();
    }

    private void writePendingAction(String action, String preset) {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            YamlConfiguration config = new YamlConfiguration();
            config.set("action", action);
            if (preset != null) {
                config.set("preset", preset);
            }
            config.save(pendingActionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte pending_action.yml nicht schreiben: " + e.getMessage());
        }
    }

    public void processPendingActionOnLoad() {
        if (!pendingActionFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(pendingActionFile);
        String action = config.getString("action");
        String preset = config.getString("preset");

        if (action == null) {
            pendingActionFile.delete();
            return;
        }

        File container = plugin.getServer().getWorldContainer();
        String levelName = getLevelNameFromProperties();
        String netherName = levelName + "_nether";
        String endName = levelName + "_the_end";

        File worldDir = new File(container, levelName);
        File netherDir = new File(container, netherName);
        File endDir = new File(container, endName);

        plugin.getLogger().info("=================================================");
        plugin.getLogger().info("Führe anstehende Welt-Aktion aus: " + action + (preset != null ? " (" + preset + ")" : ""));

        try {
            if (action.equalsIgnoreCase("RESET")) {
                // Wipe world directories
                FileUtils.deleteDirectory(worldDir.toPath());
                FileUtils.deleteDirectory(netherDir.toPath());
                FileUtils.deleteDirectory(endDir.toPath());

                // Reset timer data in timer.yml
                writeTimerConfig(0, false, 1);
                plugin.getLogger().info("Welten (" + levelName + ", " + netherName + ", " + endName + ") wurden für Neugenerierung gelöscht.");
                plugin.getLogger().info("Timer und Spielerdaten wurden zurückgesetzt.");
            } else if (action.equalsIgnoreCase("ROLLBACK") || action.equalsIgnoreCase("LOAD")) {
                String sourceFolder = action.equalsIgnoreCase("ROLLBACK") ? "_rollback" : preset;
                File srcDir = new File(getPresetsDir(), sourceFolder);

                if (!srcDir.exists()) {
                    plugin.getLogger().severe("Quellverzeichnis existiert nicht: " + srcDir.getAbsolutePath());
                } else {
                    // Delete current worlds first
                    FileUtils.deleteDirectory(worldDir.toPath());
                    FileUtils.deleteDirectory(netherDir.toPath());
                    FileUtils.deleteDirectory(endDir.toPath());

                    List<String> ignored = List.of("session.lock", "uid.dat");

                    // Restore worlds
                    File srcWorld = new File(srcDir, levelName);
                    if (srcWorld.exists()) {
                        FileUtils.copyDirectory(srcWorld.toPath(), worldDir.toPath(), ignored);
                        FileUtils.cleanWorldLockFiles(worldDir.toPath());
                    }

                    File srcNether = new File(srcDir, netherName);
                    if (srcNether.exists()) {
                        FileUtils.copyDirectory(srcNether.toPath(), netherDir.toPath(), ignored);
                        FileUtils.cleanWorldLockFiles(netherDir.toPath());
                    }

                    File srcEnd = new File(srcDir, endName);
                    if (srcEnd.exists()) {
                        FileUtils.copyDirectory(srcEnd.toPath(), endDir.toPath(), ignored);
                        FileUtils.cleanWorldLockFiles(endDir.toPath());
                    }

                    // Restore timer from meta.yml
                    File metaFile = new File(srcDir, "meta.yml");
                    if (metaFile.exists()) {
                        YamlConfiguration meta = YamlConfiguration.loadConfiguration(metaFile);
                        long time = meta.getLong("timeElapsedSeconds", 0);
                        boolean running = meta.getBoolean("timerRunning", false);
                        int players = meta.getInt("requiredPlayers", 1);
                        writeTimerConfig(time, running, players);
                    }

                    plugin.getLogger().info("Welten und Spielerdaten erfolgreich von '" + sourceFolder + "' wiederhergestellt.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Ausführen der Welt-Aktion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pendingActionFile.delete();
            plugin.getLogger().info("Welt-Aktion abgeschlossen. Server lädt nun die Welten.");
            plugin.getLogger().info("=================================================");
        }
    }

    private void writeTimerConfig(long seconds, boolean running, int requiredPlayers) {
        try {
            File timerFile = new File(plugin.getDataFolder(), "timer.yml");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            YamlConfiguration timerConfig = timerFile.exists() ? YamlConfiguration.loadConfiguration(timerFile) : new YamlConfiguration();
            timerConfig.set("timeElapsedSeconds", seconds);
            timerConfig.set("timerRunning", running);
            timerConfig.set("requiredPlayers", requiredPlayers);
            timerConfig.save(timerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte timer.yml nicht aktualisieren: " + e.getMessage());
        }
    }

    public void kickAllPlayers(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(message);
        }
    }

    public void restartServer() {
        plugin.getLogger().info("Server-Neustart zur Durchführung der Welt-Änderung wird eingeleitet...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Bukkit.spigot().restart();
            } catch (Throwable t) {
                plugin.getLogger().warning("Bukkit.spigot().restart() nicht verfügbar, fahre Server herunter (shutdown): " + t.getMessage());
                Bukkit.shutdown();
            }
        }, 1L);
    }
}

