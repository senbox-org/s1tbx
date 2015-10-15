package org.esa.snap.core.util.io;

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

import static java.nio.file.FileVisitResult.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * A {@code FileVisitor} that copies a file tree.
 */
public class TreeCopier implements FileVisitor<Path> {
    private final Path source;
    private final Path target;
    private IOException exception;

    /**
     * Deeply copies the directory given by {@code source} into a directory with the same name located in an existing
     * parent directory given by {@code targetParent}. A new directory will be created if it does not exists yet,
     * otherwise its contents will be overwritten.
     *
     * @param source       The source directory.
     * @param targetParent The target's parent directory. Must already exist.
     * @throws IOException
     */
    public static Path copyDir(Path source, Path targetParent) throws IOException {
        Path target = targetParent.resolve(source.getFileName().toString());
        if (!Files.exists(target)) {
            target = Files.createDirectory(target);
        }
        copy(source, target);
        return target;
    }

    /**
     * Deeply copies the contents of the directory given by {@code source} into the existing
     * directory given by {@code target}. The method overwrites any existing content.
     *
     * @param source The source directory.
     * @param target The target directory. Must already exist.
     * @throws IOException
     */
    public static void copy(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            throw new NoSuchFileException(target.toString());
        }
        if (!Files.isDirectory(target)) {
            throw new NotDirectoryException(target.toString());
        }
        //Path dir = target.resolve(source.getFileName().toString());
        TreeCopier treeCopier = new TreeCopier(source, target);
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, treeCopier);
        if (treeCopier.exception != null) {
            throw treeCopier.exception;
        }
    }

    TreeCopier(Path source, Path target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        Files.copy(file, target.resolve(source.relativize(file).toString()), COPY_ATTRIBUTES, REPLACE_EXISTING);
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
        try {
            Files.copy(dir, target.resolve(source.relativize(dir).toString()), COPY_ATTRIBUTES);
        } catch (FileAlreadyExistsException x) {
            // ignore
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
        if (exception == null) {
            Path newDir = target.resolve(source.relativize(dir).toString());
            FileTime time = Files.getLastModifiedTime(dir);
            Files.setLastModifiedTime(newDir, time);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exception) {
        this.exception = exception;
        return TERMINATE;
    }
}
