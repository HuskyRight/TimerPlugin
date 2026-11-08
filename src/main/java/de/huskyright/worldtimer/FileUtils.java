package de.huskyright.worldtimer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class FileUtils {

    public static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    // Try to make writable and delete again if file was read-only
                    File f = file.toFile();
                    if (!f.canWrite()) {
                        f.setWritable(true);
                    }
                    Files.deleteIfExists(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.deleteIfExists(dir);
                } catch (IOException e) {
                    File d = dir.toFile();
                    if (!d.canWrite()) {
                        d.setWritable(true);
                    }
                    Files.deleteIfExists(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyDirectory(Path source, Path target, List<String> ignoredNames) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (ignoredNames != null && ignoredNames.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (ignoredNames != null && ignoredNames.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = target.resolve(source.relativize(file));
                try {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // In case destination was read-only or in use, log or retry
                    File f = targetFile.toFile();
                    if (f.exists() && !f.canWrite()) {
                        f.setWritable(true);
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        throw e;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void cleanWorldLockFiles(Path worldDir) {
        if (!Files.exists(worldDir)) {
            return;
        }
        try {
            Files.deleteIfExists(worldDir.resolve("session.lock"));
            Files.deleteIfExists(worldDir.resolve("uid.dat"));
        } catch (IOException ignored) {
        }
    }
}

