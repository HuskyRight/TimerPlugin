package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TimerPlugin extends JavaPlugin {

    private boolean timerRunning = false;
    private long timeElapsedSeconds = 0;
    private int requiredPlayers = 1;
    private boolean isWorldTrulyPaused = false; 

    private BossBar timerBossBar;
    private BukkitTask timerTask;
    private WorldManager worldManager;
    private final Set<String> excludedPlayers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public void onLoad() {
        this.worldManager = new WorldManager(this);
        // Process world reset, rollback or preset load before worlds are loaded by server
        this.worldManager.processPendingActionOnLoad();
    }

    @Override
    public void onEnable() {
        if (this.worldManager == null) {
            this.worldManager = new WorldManager(this);
        }

        timerBossBar = Bukkit.createBossBar("Timer", BarColor.BLUE, BarStyle.SOLID);

        TimerCommands timerCommands = new TimerCommands(this);
        if (getCommand("timer") != null) {
            getCommand("timer").setExecutor(timerCommands);
            getCommand("timer").setTabCompleter(timerCommands);
        }
        if (getCommand("timermode") != null) {
            getCommand("timermode").setExecutor(timerCommands);
            getCommand("timermode").setTabCompleter(timerCommands);
        }

        WorldCommands worldCommands = new WorldCommands(this, worldManager);
        if (getCommand("world") != null) {
            getCommand("world").setExecutor(worldCommands);
            getCommand("world").setTabCompleter(worldCommands);
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        loadTimerData();
        startMainTicker();
        getLogger().info("WorldTimer wurde erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (timerBossBar != null) {
            timerBossBar.removeAll();
        }
        saveTimerData();
        setWorldPaused(false);
        getLogger().info("WorldTimer wurde deaktiviert!");
    }

    public void loadTimerData() {
        File file = new File(getDataFolder(), "timer.yml");
        if (!file.exists()) {
            updateTimerDisplay();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.timeElapsedSeconds = config.getLong("timeElapsedSeconds", 0);
        this.timerRunning = config.getBoolean("timerRunning", false);
        this.requiredPlayers = config.getInt("requiredPlayers", 1);
        this.excludedPlayers.clear();
        List<String> excluded = config.getStringList("excludedPlayers");
        if (excluded != null) {
            this.excludedPlayers.addAll(excluded);
        }
        boolean visible = config.getBoolean("bossBarVisible", false);
        if (timerBossBar != null) {
            timerBossBar.setVisible(visible);
            updateTimerDisplay();
        }
    }

    public void saveTimerData() {
        File file = new File(getDataFolder(), "timer.yml");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("timeElapsedSeconds", timeElapsedSeconds);
        config.set("timerRunning", timerRunning);
        config.set("requiredPlayers", requiredPlayers);
        config.set("excludedPlayers", new ArrayList<>(excludedPlayers));
        config.set("bossBarVisible", timerBossBar != null && timerBossBar.isVisible());
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().severe("Konnte timer.yml nicht speichern: " + e.getMessage());
        }
    }

    private void startMainTicker() {
        timerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            checkConditionsAndPause();
            if (timerRunning) {
                timeElapsedSeconds++;
                updateTimerDisplay();
            }
            if (timerBossBar.isVisible()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!timerBossBar.getPlayers().contains(player)) {
                        timerBossBar.addPlayer(player);
                    }
                }
            }
        }, 0L, 20L);
    }

    public void checkConditionsAndPause() {
        long activePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !isPlayerExcluded(p))
                .count();
        boolean shouldBePaused = !timerRunning || activePlayers < requiredPlayers;
        setWorldPaused(shouldBePaused);
    }

    public boolean isPlayerExcluded(Player player) {
        if (player == null) return false;
        return isPlayerExcluded(player.getName());
    }

    public boolean isPlayerExcluded(String name) {
        if (name == null) return false;
        return excludedPlayers.contains(name);
    }

    public boolean addExcludedPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        boolean added = excludedPlayers.add(name);
        if (added) {
            saveTimerData();
            checkConditionsAndPause();
        }
        return added;
    }

    public boolean removeExcludedPlayer(String name) {
        if (name == null) return false;
        boolean removed = excludedPlayers.remove(name);
        if (removed) {
            saveTimerData();
            checkConditionsAndPause();
        }
        return removed;
    }

    public Set<String> getExcludedPlayers() {
        return Collections.unmodifiableSet(excludedPlayers);
    }

    public void setWorldPaused(boolean paused) {
        if (paused == this.isWorldTrulyPaused) {
            return;
        }
        this.isWorldTrulyPaused = paused;

        if (Bukkit.getWorlds().isEmpty()) {
            return;
        }
        World mainWorld = Bukkit.getWorlds().get(0);

        mainWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, !paused);
        mainWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, paused ? 0 : 3);
        
        for (LivingEntity entity : mainWorld.getLivingEntities()) {
            if (!(entity instanceof Player)) {
                entity.setAI(!paused); 
            }
        }
        
        if (paused) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Die Welt wurde pausiert!");
        } else {
            Bukkit.broadcastMessage(ChatColor.GREEN + "Die Welt läuft weiter!");
        }
    }

    public void updateTimerDisplay() {
        if (timerBossBar == null) return;
        long hours = timeElapsedSeconds / 3600;
        long minutes = (timeElapsedSeconds % 3600) / 60;
        long seconds = timeElapsedSeconds % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerBossBar.setTitle(ChatColor.AQUA + "Zeit: " + ChatColor.WHITE + timeString);
    }
    
    public boolean isTimerRunning() { return timerRunning; }
    public void setTimerRunning(boolean timerRunning) { this.timerRunning = timerRunning; }
    public long getTimeElapsedSeconds() { return timeElapsedSeconds; }
    public void setTimeElapsedSeconds(long timeElapsedSeconds) { this.timeElapsedSeconds = timeElapsedSeconds; }
    public int getRequiredPlayers() { return requiredPlayers; }
    public void setRequiredPlayers(int requiredPlayers) { this.requiredPlayers = requiredPlayers; }
    public BossBar getTimerBossBar() { return timerBossBar; }
    public boolean isWorldTrulyPaused() { return isWorldTrulyPaused; }
    public WorldManager getWorldManager() { return worldManager; }
}
