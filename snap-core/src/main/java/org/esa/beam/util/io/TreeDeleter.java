package org.esa.beam.util.io;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A {@code FileVisitor} that deletes a file tree.
 */
public class TreeDeleter implements FileVisitor<Path> {
    private IOException exception;

    /**
     * Deeply deletes the directory given by {@code source}.
     *
     * @param source       The source directory.
     * @throws IOException
     */
    public static void deleteDir(Path source) throws IOException {
        TreeDeleter treeDeleter = new TreeDeleter();
        Files.walkFileTree(source, treeDeleter);
        if (treeDeleter.exception != null) {
            throw treeDeleter.exception;
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        Files.delete(file);
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
        if (exception == null) {
            Files.delete(dir);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exception) {
        this.exception = exception;
        return TERMINATE;
    }
}
