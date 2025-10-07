package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final TimerPlugin plugin;

    public PlayerListener(TimerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Füge den Spieler zur BossBar hinzu, wenn sie sichtbar ist
        if (plugin.getTimerBossBar().isVisible()) {
            plugin.getTimerBossBar().addPlayer(player);
        }
        // Wir warten einen Tick, damit die Spielerliste des Servers aktualisiert ist, bevor wir prüfen.
        Bukkit.getScheduler().runTaskLater(plugin, plugin::checkConditionsAndPause, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Wir warten wieder einen Tick, bevor wir prüfen.
        Bukkit.getScheduler().runTaskLater(plugin, plugin::checkConditionsAndPause, 1L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Pausiere den Timer, wenn ein Spieler stirbt
        if (plugin.isTimerRunning()) {
            plugin.setTimerRunning(false);
            plugin.checkConditionsAndPause();
            Bukkit.broadcastMessage(ChatColor.RED + event.getDeathMessage() + ". Der Timer wurde pausiert!");
            event.setDeathMessage(null); // Verhindert die doppelte Todesnachricht
        }
    }
}