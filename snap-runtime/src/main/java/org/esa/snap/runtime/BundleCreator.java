package org.esa.snap.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.file.FileVisitResult.*;

/**
 * This main class offers to create a bundle either from an existing SNAP installation, or from checked-out and build
 * source code. Such bundles can be used within 3rd-party software, typically in processing environments.
 *
 * @author Thomas Storm
 * @since SNAP 2.0
 */
public class BundleCreator {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("   BundleCreator TARGET-FILE (SNAP-INSTALLATION-DIR | SNAP-ENGINE-DIR [SNAP-TOOLBOXES-DIR]...)");
            System.exit(-1);
        }

        String targetFile = args[0];

        String arg2 = args[1];
        boolean isInstalledSnap = detectMode(arg2);

        if (isInstalledSnap) {
            createBundleFromInstalledSnap(arg2, targetFile);
        } else {
            createBundleFromBuiltSnap(args, targetFile);
        }

    }

    private static void createBundleFromInstalledSnap(String installationDir, String targetFile) {
        if (!targetFile.endsWith("zip")) {
            targetFile = targetFile + ".zip";
        }

        EngineConfig config = EngineConfig.instance();
        config.logger().info("Creating assembly file " + targetFile + " for SNAP installation '" + installationDir + "'...");

        Path installationPath = Paths.get(installationDir);
        config.installDir(installationPath);

        InstallationScanner.ScanResult scanResult = new InstallationScanner(config).scanInstallationDir();

        List<Path> classPathEntries = scanResult.classPathEntries;
        List<Path> libraryPathEntries = scanResult.libraryPathEntries;

        if (classPathEntries.isEmpty() && libraryPathEntries.isEmpty()) {
            config.logger().warning("Nothing found in directory '" + installationDir + "'. Please make sure a valid " +
                    "SNAP installation is provided.");
            System.exit(0);
        }

        Path nbUserDir = config.userDir().resolve("system");
        if (!Files.isDirectory(nbUserDir)) {
            nbUserDir = Paths.get(System.getProperty("user.home")).resolve("AppData").resolve("Roaming").resolve("SNAP");
        }

        Logger logger = config.logger();
        Path zipfile = Paths.get(targetFile);
        URI fileUri = zipfile.toUri();
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI zipUri;
        try {
            zipUri = new URI("jar:" + fileUri.getScheme(), fileUri.getRawPath(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, env)) {
            logger.fine("Adding classpath entries to zip...");
            for (Path classPathEntry : classPathEntries) {
                String targetFilename;
                if (classPathEntry.startsWith(installationPath) || classPathEntry.startsWith(nbUserDir)) {
                    targetFilename = getTargetFilename(classPathEntry.getParent().toString(), classPathEntry);
                } else {
                    logger.warning("Invalid classpath entry: '" + classPathEntry.toString());
                    continue;
                }
                targetFilename = targetFilename.replace(File.separator, "_");
                Files.copy(classPathEntry, zipfs.getPath(File.separator + targetFilename), StandardCopyOption.REPLACE_EXISTING);
                logger.fine("Added '" + classPathEntry.toString() + "'");
            }
            logger.fine("done. Adding shared objects entries to zip...");
            for (Path libraryPathEntry : libraryPathEntries) {
                Files.newDirectoryStream(libraryPathEntry).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        String targetFilename = path.toString().substring(path.toString().lastIndexOf(File.separator) + 1);
                        try {
                            Files.copy(path, zipfs.getPath("/" + targetFilename), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("...done.");
    }

    private static void createBundleFromBuiltSnap(String[] args, String targetFile) throws IOException {
        EngineConfig config = EngineConfig.instance();
        Logger logger = config.logger();
        logger.info("Creating assembly file " + targetFile + " for SNAP sources at '" + args[1] + "'...");

        JarVisitor jarVisitor = new JarVisitor();
        SoVisitor soVisitor = new SoVisitor();
        List<Path> classPathEntries = new ArrayList<>();
        List<Path> libraryPathEntries = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String inputDir = args[i];
            Files.walkFileTree(Paths.get(inputDir), jarVisitor);
            Files.walkFileTree(Paths.get(inputDir), soVisitor);
        }
        classPathEntries.addAll(jarVisitor.resultFiles);
        libraryPathEntries.addAll(soVisitor.resultFiles);

        Path zipfile = Paths.get(targetFile);
        URI fileUri = zipfile.toUri();
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI zipUri;
        try {
            zipUri = new URI("jar:" + fileUri.getScheme(), fileUri.getRawPath(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, env)) {
            logger.fine("Adding classpath entries to zip...");
            for (Path classPathEntry : classPathEntries) {
                String targetFilename;
                targetFilename = getTargetFilename(classPathEntry.getParent().toString(), classPathEntry);
                targetFilename = targetFilename.replace(File.separator, "_");
                Files.copy(classPathEntry, zipfs.getPath(File.separator + targetFilename), StandardCopyOption.REPLACE_EXISTING);
                logger.fine("Added '" + classPathEntry.toString() + "'");
            }
            logger.fine("done. Adding shared objects entries to zip...");
            for (Path libraryPathEntry : libraryPathEntries) {
                String targetFilename;
                targetFilename = getTargetFilename(libraryPathEntry.getParent().toString(), libraryPathEntry);
                targetFilename = targetFilename.replace(File.separator, "_");
                Files.copy(libraryPathEntry, zipfs.getPath(File.separator + targetFilename), StandardCopyOption.REPLACE_EXISTING);
                logger.fine("Added '" + libraryPathEntry.toString() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("...done.");


    }

    private static boolean detectMode(String arg2) {
        Path maybeInstallationDir = Paths.get(arg2);
        Path binPath = maybeInstallationDir.resolve("bin");
        boolean windowsExecutableExists = Files.exists(binPath.resolve("gpt.exe"));
        boolean linuxExecutableExists = Files.exists(binPath.resolve("gpt"));
        boolean macExecutableExists = Files.exists(binPath.resolve("gpt.command"));

        return windowsExecutableExists || linuxExecutableExists || macExecutableExists;
    }

    private static String getTargetFilename(String directory, Path classPathEntry) {
        return classPathEntry.toString().substring(classPathEntry.toString().indexOf(directory) + directory.length() + 1);
    }

    private static class JarVisitor extends SimpleFileVisitor<Path> {

        List<Path> resultFiles;

        JarVisitor() {
            super();
            resultFiles = new ArrayList<>();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.toString().contains(File.separator + "target" + File.separator + "nbm" + File.separator) &&
                    file.toString().endsWith(".jar")) {
                resultFiles.add(file);
            }
            return CONTINUE;
        }

    }

    private static class SoVisitor extends SimpleFileVisitor<Path> {

        List<Path> resultFiles;

        SoVisitor() {
            super();
            resultFiles = new ArrayList<>();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            boolean isSharedObject = file.toString().endsWith(".so");
            if (isSharedObject) {
                resultFiles.add(file);
            }
            return CONTINUE;
        }

    }


}
