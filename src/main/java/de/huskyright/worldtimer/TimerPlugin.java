package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TimerPlugin extends JavaPlugin {

    // Zustandvariablen für unser Plugin
    private boolean timerRunning = false;
    private long timeElapsedSeconds = 0;
    private int requiredPlayers = 1;

    // Objekte für die Anzeige und den Ticker
    private BossBar timerBossBar;
    private BukkitTask timerTask;

    @Override
    public void onEnable() {
        // BossBar initialisieren, aber noch nicht anzeigen
        timerBossBar = Bukkit.createBossBar("Timer", BarColor.BLUE, BarStyle.SOLID);

        // Befehle und Events registrieren
        getCommand("timer").setExecutor(new TimerCommands(this));
        getCommand("timermode").setExecutor(new TimerCommands(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Starte den Haupt-Ticker, der jede Sekunde läuft (20 Ticks = 1 Sekunde)
        startMainTicker();

        getLogger().info("WorldTimer wurde erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        // Aufräumen, wenn der Server stoppt
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerBossBar.removeAll(); // Alle Spieler von der BossBar entfernen
        setWorldPaused(false); // Sicherstellen, dass die Welt nicht pausiert bleibt
        getLogger().info("WorldTimer wurde deaktiviert!");
    }

    private void startMainTicker() {
        timerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Diese Logik wird jede Sekunde ausgeführt

            // Zuerst prüfen wir, ob die Bedingungen für einen laufenden Timer erfüllt sind
            checkConditionsAndPause();

            // Wenn der Timer läuft, erhöhen wir die Zeit und aktualisieren die Anzeige
            if (timerRunning) {
                timeElapsedSeconds++;
                updateTimerDisplay();
            }

            // Sicherstellen, dass alle Online-Spieler die BossBar sehen (falls sie sichtbar ist)
            if (timerBossBar.isVisible()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!timerBossBar.getPlayers().contains(player)) {
                        timerBossBar.addPlayer(player);
                    }
                }
            }

        }, 0L, 20L); // Startet sofort (0L) und wiederholt alle 20 Ticks (1 Sekunde)
    }

    // Zentrale Methode, um zu prüfen, ob die Welt pausiert werden soll
    public void checkConditionsAndPause() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        boolean shouldBePaused = !timerRunning || onlinePlayers < requiredPlayers;
        setWorldPaused(shouldBePaused);
    }

    // Setzt den Pausenzustand der Welt
    public void setWorldPaused(boolean paused) {
        World world = Bukkit.getWorlds().get(0); // Holt die Hauptwelt (Overworld)
        if (world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED) == 0 && !paused) {
            // Welt ist pausiert, soll aber weiterlaufen
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3); // Standardwert ist 3
            Bukkit.broadcastMessage(ChatColor.GREEN + "Die Welt läuft weiter!");
        } else if (world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED) != 0 && paused) {
            // Welt läuft, soll aber pausiert werden
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Die Welt wurde pausiert!");
        }
    }

    // Aktualisiert den Text der BossBar
    public void updateTimerDisplay() {
        long hours = timeElapsedSeconds / 3600;
        long minutes = (timeElapsedSeconds % 3600) / 60;
        long seconds = timeElapsedSeconds % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerBossBar.setTitle(ChatColor.AQUA + "Zeit: " + ChatColor.WHITE + timeString);
    }

    // Getter und Setter, damit andere Klassen darauf zugreifen können
    public boolean isTimerRunning() {
        return timerRunning;
    }

    public void setTimerRunning(boolean timerRunning) {
        this.timerRunning = timerRunning;
    }

    public void setTimeElapsedSeconds(long timeElapsedSeconds) {
        this.timeElapsedSeconds = timeElapsedSeconds;
    }

    public void setRequiredPlayers(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
    }

    public BossBar getTimerBossBar() {
        return timerBossBar;
    }
}