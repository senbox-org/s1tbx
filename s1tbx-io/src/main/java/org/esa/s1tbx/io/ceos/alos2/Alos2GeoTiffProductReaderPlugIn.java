/*
 * Copyright (C) 2019 Skywatch. https://www.skywatch.co
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
package org.esa.s1tbx.io.ceos.alos2;

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Alos2GeoTiffProductReaderPlugIn implements ProductReaderPlugIn {
    private static final String[] FORMAT_NAMES = new String[]{"ALOS-2 GeoTIFF"};

    private static final ZipFileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = getZipFileSystemProvider();
    private static final Constructor<ZipFileSystem> ZIP_FILE_SYSTEM_CONSTRUCTOR;

    static {
        try {
            Constructor<ZipFileSystem> constructor = ZipFileSystem.class.getDeclaredConstructor(ZipFileSystemProvider.class, Path.class, Map.class);
            constructor.setAccessible(true);
            ZIP_FILE_SYSTEM_CONSTRUCTOR = constructor;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static ZipFileSystemProvider getZipFileSystemProvider() {
        for (FileSystemProvider fsr : FileSystemProvider.installedProviders()) {
            if (fsr instanceof ZipFileSystemProvider)
                return (ZipFileSystemProvider) fsr;
        }
        throw new FileSystemNotFoundException("The zip file system provider is not installed!");
    }

    private static FileSystem newZipFileSystem(Path zipPath) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (zipPath.getFileSystem() instanceof ZipFileSystem) {
            throw new IllegalArgumentException("Can't create a ZIP file system nested in a ZIP file system. (" + zipPath + " is nested in " + zipPath.getFileSystem() + ")");
        }
        return ZIP_FILE_SYSTEM_CONSTRUCTOR.newInstance(ZIP_FILE_SYSTEM_PROVIDER, zipPath, Collections.emptyMap());
    }

    public static Path convertInputToPath(Object input) {
        if (input == null) {
            throw new NullPointerException();
        } else if (input instanceof File) {
            return ((File) input).toPath();
        } else if (input instanceof Path) {
            return (Path) input;
        } else if (input instanceof String) {
            return Paths.get((String) input);
        } else {
            throw new IllegalArgumentException("Unknown input '" + input + "'.");
        }
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        try{
            final File inputFile;
            if (input instanceof String){
                inputFile = new File((String) input);
            } else if (input instanceof File){
                inputFile = (File)input;
            } else{
                return DecodeQualification.UNABLE;
            }
            if(input instanceof String || input instanceof File){
                final String extension = FileUtils.getExtension(inputFile);

                if (extension != null && extension.toUpperCase().equals(".ZIP")){
                    return checkZIPFile(inputFile);
                }
                return checkFileName(inputFile);
            }

            return DecodeQualification.UNABLE;

        }catch (Exception e){
            e.printStackTrace();
            return DecodeQualification.UNABLE;
        }
    }

    // Additional helper functions for getDecodeQualification
    private DecodeQualification checkFileName(File inputFile){
        boolean hasValidImage = false;
        boolean hasMetadata = false;

        final File[] files = inputFile.getParentFile().listFiles();
        if(files != null) {
            for (File f : files) {
                String name = f.getName().toUpperCase();
                if (name.equals("SUMMARY.TXT")) {
                    // File name contains the right keywords, and the folder contains the metadata file.
                    hasMetadata = true;
                }
                if (name.contains("ALOS2") && (name.endsWith("TIF") || name.endsWith("TIFF")) &&
                        (name.contains("IMG-") &&
                                (name.contains("-HH-") || name.contains("-HV-") || name.contains("-VH-") || name.contains("-VV-")))){
                    hasValidImage = true;
                }
            }
        }
        if(hasMetadata && hasValidImage)
            return DecodeQualification.INTENDED;

        return DecodeQualification.UNABLE;
    }

    private List<String> listFilesFromZipArchive(Path baseFile) throws IOException {
        List<String> filesAndFolders = new ArrayList<>();
        try (FileSystem fileSystem = newZipFileSystem(baseFile)) {
            for (Path root : fileSystem.getRootDirectories()) {
                FileVisitor<Path> visitor = new ListFilesAndFolderVisitor() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (root.equals(dir)) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            String zipEntryPath = remoteFirstSeparatorIfExists(root.relativize(dir).toString());
                            filesAndFolders.add(zipEntryPath);
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    private String remoteFirstSeparatorIfExists(String zipEntryPath) {
                        if (zipEntryPath.startsWith("/")) {
                            return zipEntryPath.substring(1);
                        }
                        return zipEntryPath;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String zipEntryPath = remoteFirstSeparatorIfExists(file.toString());
                        filesAndFolders.add(zipEntryPath);
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(root, visitor);
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
        return filesAndFolders;
    }

    private DecodeQualification checkZIPFile(Object input) throws IOException {
        Path imageIOInputPath = convertInputToPath(input);
        List<String> zipContents = listFilesFromZipArchive(imageIOInputPath);
        boolean hasValidImage = false;
        boolean hasMetadata = false;

        for (String zipContent : zipContents) {
            String name = zipContent.toUpperCase();
            if ((name.endsWith("TIF") || name.endsWith("TIFF")) &&
                    (name.startsWith("IMG-") &&
                            (name.contains("-HH-") ||
                                    name.contains("-HV-") ||
                                    name.contains("-VH-") ||
                                    name.contains("-VV-")))) {
                hasValidImage = true;
            }
            if (name.contains("SUMMARY.TXT")){
                hasMetadata = true;
            }

        }
        if (hasMetadata && hasValidImage){
            return DecodeQualification.INTENDED;
        } else{
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        Class [] returnClass = new Class[2];

        returnClass[0] = String.class;
        returnClass[1] = File.class;
        return returnClass;
    }

    @Override
    public ProductReader createReaderInstance()  {
        return new Alos2GeoTiffProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        final String [] extensions = new String[3];
        extensions[0] = "tif";
        extensions[1] = "tiff";
        extensions[2] = "zip";
        return extensions;
    }

    @Override
    public String getDescription(Locale locale) {
        return "ALOS-2 GeoTIFF data product.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));

    }

    private abstract class ListFilesAndFolderVisitor implements FileVisitor<Path> {

        ListFilesAndFolderVisitor() {
        }

        @Override
        public final FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public final FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}
