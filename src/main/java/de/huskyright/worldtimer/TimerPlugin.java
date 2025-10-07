package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TimerPlugin extends JavaPlugin {

    private boolean timerRunning = false;
    private long timeElapsedSeconds = 0;
    private int requiredPlayers = 1;
    private boolean isWorldTrulyPaused = false; 

    private BossBar timerBossBar;
    private BukkitTask timerTask;

    @Override
    public void onEnable() {
        timerBossBar = Bukkit.createBossBar("Timer", BarColor.BLUE, BarStyle.SOLID);
        getCommand("timer").setExecutor(new TimerCommands(this));
        getCommand("timermode").setExecutor(new TimerCommands(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
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
        setWorldPaused(false);
        getLogger().info("WorldTimer wurde deaktiviert!");
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
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        boolean shouldBePaused = !timerRunning || onlinePlayers < requiredPlayers;
        setWorldPaused(shouldBePaused);
    }

    public void setWorldPaused(boolean paused) {
        if (paused == this.isWorldTrulyPaused) {
            return;
        }
        this.isWorldTrulyPaused = paused;

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
        long hours = timeElapsedSeconds / 3600;
        long minutes = (timeElapsedSeconds % 3600) / 60;
        long seconds = timeElapsedSeconds % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerBossBar.setTitle(ChatColor.AQUA + "Zeit: " + ChatColor.WHITE + timeString);
    }
    
    public boolean isTimerRunning() { return timerRunning; }
    public void setTimerRunning(boolean timerRunning) { this.timerRunning = timerRunning; }
    public void setTimeElapsedSeconds(long timeElapsedSeconds) { this.timeElapsedSeconds = timeElapsedSeconds; }
    public void setRequiredPlayers(int requiredPlayers) { this.requiredPlayers = requiredPlayers; }
    public BossBar getTimerBossBar() { return timerBossBar; }
    public boolean isWorldTrulyPaused() { return isWorldTrulyPaused; }
}