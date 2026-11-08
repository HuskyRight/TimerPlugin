package de.huskyright.worldtimer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    public void testCopyDirectoryWithIgnoredFiles() throws IOException {
        Path source = tempDir.resolve("sourceWorld");
        Path target = tempDir.resolve("targetWorld");
        Files.createDirectories(source.resolve("region"));
        Files.createDirectories(source.resolve("playerdata"));

        Files.writeString(source.resolve("level.dat"), "level_data_content");
        Files.writeString(source.resolve("session.lock"), "lock_content");
        Files.writeString(source.resolve("uid.dat"), "uid_content");
        Files.writeString(source.resolve("region").resolve("r.0.0.mca"), "region_chunk_data");
        Files.writeString(source.resolve("playerdata").resolve("uuid.dat"), "player_inventory_data");

        FileUtils.copyDirectory(source, target, List.of("session.lock", "uid.dat"));

        assertTrue(Files.exists(target.resolve("level.dat")));
        assertTrue(Files.exists(target.resolve("region").resolve("r.0.0.mca")));
        assertTrue(Files.exists(target.resolve("playerdata").resolve("uuid.dat")));

        // session.lock and uid.dat should be excluded
        assertFalse(Files.exists(target.resolve("session.lock")));
        assertFalse(Files.exists(target.resolve("uid.dat")));

        assertEquals("level_data_content", Files.readString(target.resolve("level.dat")));
        assertEquals("player_inventory_data", Files.readString(target.resolve("playerdata").resolve("uuid.dat")));
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        Path toDelete = tempDir.resolve("toDelete");
        Files.createDirectories(toDelete.resolve("sub/deep"));
        Files.writeString(toDelete.resolve("file1.txt"), "hello");
        Files.writeString(toDelete.resolve("sub").resolve("file2.txt"), "world");
        Files.writeString(toDelete.resolve("sub/deep").resolve("file3.txt"), "foo");

        assertTrue(Files.exists(toDelete));
        FileUtils.deleteDirectory(toDelete);
        assertFalse(Files.exists(toDelete));
    }

    @Test
    public void testCleanWorldLockFiles() throws IOException {
        Path worldDir = tempDir.resolve("world");
        Files.createDirectories(worldDir);
        Files.writeString(worldDir.resolve("session.lock"), "lock");
        Files.writeString(worldDir.resolve("uid.dat"), "uid");
        Files.writeString(worldDir.resolve("level.dat"), "level");

        FileUtils.cleanWorldLockFiles(worldDir);

        assertFalse(Files.exists(worldDir.resolve("session.lock")));
        assertFalse(Files.exists(worldDir.resolve("uid.dat")));
        assertTrue(Files.exists(worldDir.resolve("level.dat")));
    }
}

