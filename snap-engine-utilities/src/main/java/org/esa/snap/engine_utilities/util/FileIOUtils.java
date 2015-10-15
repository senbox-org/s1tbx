/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.*;

/**

 */
public class FileIOUtils {

    /**
     * Reads a text file and replaces all outText with newText
     *
     * @param inFile  input file
     * @param outFile output file
     * @param oldText text to replace
     * @param newText replacement text
     * @throws IOException on io error
     */
    public static void replaceText(final File inFile, final File outFile,
                                   final String oldText, final String newText) throws IOException {
        final List<String> lines;
        try (FileReader fileReader = new FileReader(inFile)) {
            lines = IOUtils.readLines(fileReader);

            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                if (line.contains(oldText)) {
                    lines.set(i, line.replaceAll(oldText, newText));
                }
            }
        }

        if (!lines.isEmpty()) {
            try (FileWriter fileWriter = new FileWriter(outFile)) {
                IOUtils.writeLines(lines, "\n", fileWriter);
            }

        }
    }

    static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path source;
        private final Path target;
        private final boolean isMove;

        CopyDirVisitor(final Path source, final Path target, boolean move) {
            this.source = source;
            this.target = target;
            this.isMove = move;
        }

        private boolean copyFile(final Path source, final Path target) throws IOException {
            try {
                if (isMove)
                    Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING);
                else
                    Files.copy(source, target, COPY_ATTRIBUTES, REPLACE_EXISTING);
            } catch (FileAlreadyExistsException x) {
                // ignore
            } catch (NoSuchFileException x) {
                // ignore
            }
            return true;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = target.resolve(source.relativize(dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            copyFile(file, target.resolve(source.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }

    public static void copyFolder(final Path source, final Path target) throws IOException {
        // follow links when copying files
        final EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        final CopyDirVisitor tc = new CopyDirVisitor(source, target, false);
        Files.walkFileTree(source, opts, Integer.MAX_VALUE, tc);
    }

    public static void moveFolder(final Path source, final Path target) throws IOException {
        // follow links when copying files
        final EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        final CopyDirVisitor tc = new CopyDirVisitor(source, target, true);
        Files.walkFileTree(source, opts, Integer.MAX_VALUE, tc);
    }

    public static void deleteFolder(final Path source) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }
}
