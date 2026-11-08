package de.huskyright.worldtimer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WorldPresetTest {

    @Test
    public void testFormattedTimer() {
        WorldPreset preset1 = new WorldPreset("test1", 1000L, "08.11.2026 12:00:00", 3665L, true, 2, "Admin");
        assertEquals("01:01:05", preset1.getFormattedTimer());
        assertEquals("test1", preset1.getName());
        assertEquals(3665L, preset1.getTimeElapsedSeconds());
        assertTrue(preset1.isTimerRunning());
        assertEquals(2, preset1.getRequiredPlayers());
        assertEquals("Admin", preset1.getSavedBy());

        WorldPreset presetZero = new WorldPreset("zero", 1000L, "08.11.2026 12:00:00", 0L, false, 1, "Player");
        assertEquals("00:00:00", presetZero.getFormattedTimer());
    }

    @Test
    public void testPresetSortingByTimestamp() {
        List<WorldPreset> presets = new ArrayList<>();
        presets.add(new WorldPreset("old", 1000L, "old_date", 10, false, 1, "A"));
        presets.add(new WorldPreset("newest", 5000L, "newest_date", 20, false, 1, "B"));
        presets.add(new WorldPreset("middle", 3000L, "middle_date", 30, false, 1, "C"));

        presets.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        assertEquals("newest", presets.get(0).getName());
        assertEquals("middle", presets.get(1).getName());
        assertEquals("old", presets.get(2).getName());
    }
}

