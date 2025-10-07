package de.huskyright.worldtimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final TimerPlugin plugin;

    public PlayerListener(TimerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTimerBossBar().isVisible()) {
            plugin.getTimerBossBar().addPlayer(player);
        }
        Bukkit.getScheduler().runTaskLater(plugin, plugin::checkConditionsAndPause, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, plugin::checkConditionsAndPause, 1L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.isTimerRunning()) {
            plugin.setTimerRunning(false);
            plugin.checkConditionsAndPause();
            Bukkit.broadcastMessage(ChatColor.RED + event.getDeathMessage() + ". Der Timer wurde pausiert!");
            event.setDeathMessage(null);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.isWorldTrulyPaused()) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() 
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) 
            {
                event.setCancelled(true);
            }
        }
    }
}