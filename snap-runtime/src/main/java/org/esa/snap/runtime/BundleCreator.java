package org.esa.snap.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This main class offers to create a bundle from the existing SNAP installation. Such bundles can be used within 3rd-party
 * software, typically in processing environments.
 *
 * @author Thomas Storm
 * @since SNAP 2.0
 */
public class BundleCreator {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("   BundleCreator <snap-installation-dir> <target-file>");
            System.exit(-1);
        }

        String installationDir = args[0];
        String targetFile = args[1];
        if (!targetFile.endsWith("zip")) {
            targetFile = targetFile + ".zip";
        }

        EngineConfig config = EngineConfig.instance();
        config.logger().info("Creating assembly file " + targetFile + " for SNAP installation '" + installationDir + "'...");

        config.installDir(Paths.get(installationDir));

        Path userDir = config.userDir();
        InstallationScanner.ScanResult scanResult = new InstallationScanner(config).scanInstallationDir();

        List<Path> classPathEntries = scanResult.classPathEntries;
        List<Path> libraryPathEntries = scanResult.libraryPathEntries;

        if (classPathEntries.isEmpty() && libraryPathEntries.isEmpty()) {
            config.logger().warning("Nothing found in directory '" + installationDir + "'. Please make sure a valid " +
                    "SNAP installation is provided.");
            System.exit(0);
        }

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + targetFile);

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            config.logger().fine("Adding classpath entries to zip...");
            for (Path classPathEntry : classPathEntries) {
                String targetFilename;
                if (classPathEntry.toString().contains(installationDir)) {
                    targetFilename = getTargetFilename(installationDir, classPathEntry);
                } else if (classPathEntry.toString().contains(userDir.toString())) {
                    targetFilename = getTargetFilename(userDir.toString(), classPathEntry);
                } else {
                    config.logger().warning("Invalid classpath entry: '" + classPathEntry.toString());
                    continue;
                }
                targetFilename = targetFilename.replace("/", "_");
                Files.copy(classPathEntry, zipfs.getPath("/" + targetFilename), StandardCopyOption.REPLACE_EXISTING);
                config.logger().fine("Added '" + classPathEntry.toString() + "'");
            }
            config.logger().fine("done. Adding shared objects entries to zip...");
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
        config.logger().info("...done.");
    }

    private static String getTargetFilename(String directory, Path classPathEntry) {
        return classPathEntry.toString().substring(classPathEntry.toString().indexOf(directory) + directory.length() + 1);
    }

}
