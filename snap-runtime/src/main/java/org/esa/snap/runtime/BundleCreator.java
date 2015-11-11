package org.esa.snap.runtime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This main class offers to create a bundle from the existing SNAP installation, which can be used within 3rd-party
 * software, typically processing environments.
 *
 * @author Thomas Storm
 * @since SNAP 2.0
 */
public class BundleCreator {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage:");
            System.out.println("   BundleCreator <snap-installation-dir> <target-file> <arch> <os>");
            System.exit(-1);
        }

        EngineConfig config = EngineConfig.instance();

        String installationDir = args[0];
        config.installDir(Paths.get(installationDir));
        String targetFile = args[1];


        Path userDir = config.userDir();
        InstallationScanner.ScanResult scanResult = new InstallationScanner(config).scanInstallationDir();
        List<Path> classPathEntries = scanResult.classPathEntries;
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + targetFile);

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
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
                Files.copy(classPathEntry, zipfs.getPath("/" + targetFilename));
                config.logger().fine("Added '" + classPathEntry.toString() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        config.logger().info("Done.");
    }

    private static String getTargetFilename(String directory, Path classPathEntry) {
        return classPathEntry.toString().substring(classPathEntry.toString().indexOf(directory) + directory.length() + 1);
    }

}
