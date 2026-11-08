package de.huskyright.worldtimer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorldPreset {

    private final String name;
    private final long timestamp;
    private final String formattedDate;
    private final long timeElapsedSeconds;
    private final boolean timerRunning;
    private final int requiredPlayers;
    private final String savedBy;

    public WorldPreset(String name, long timestamp, String formattedDate, long timeElapsedSeconds,
                       boolean timerRunning, int requiredPlayers, String savedBy) {
        this.name = name;
        this.timestamp = timestamp;
        this.formattedDate = formattedDate;
        this.timeElapsedSeconds = timeElapsedSeconds;
        this.timerRunning = timerRunning;
        this.requiredPlayers = requiredPlayers;
        this.savedBy = savedBy;
    }

    public static String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new Date(millis));
    }

    public String getFormattedTimer() {
        long hours = timeElapsedSeconds / 3600;
        long minutes = (timeElapsedSeconds % 3600) / 60;
        long seconds = timeElapsedSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return formattedDate != null ? formattedDate : formatDate(timestamp);
    }

    public long getTimeElapsedSeconds() {
        return timeElapsedSeconds;
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public String getSavedBy() {
        return savedBy;
    }
}

