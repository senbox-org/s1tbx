package org.esa.snap.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class allows to scan an existing SNAP installation for its modules and libraries.
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 * @see Engine
 * @since SNAP 2.0
 */
class InstallationScanner {

    private static final String JAR_EXT = ".jar";

    private final EngineConfig config;

    public InstallationScanner(EngineConfig config) {
        this.config = config;
    }

    ScanResult scanInstallationDir() {
        try {
            return scanInstallationDir0();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ScanResult scanInstallationDir0() throws IOException {
        ScanResult scanResult = new ScanResult();
        Path installationDir = config.installDir();
        Path clustersFile = installationDir.resolve(Paths.get("etc", "snap.clusters"));
        if (Files.exists(clustersFile)) {
            // SNAP-Desktop NetBeans installation (the default)
            return scanNetBeansInstallationStructure(installationDir, clustersFile, scanResult);
        } else {
            // SNAP-Engine stand-alone packaging
            return scanEngineInstallationStructure(installationDir, scanResult);
        }
    }

    private ScanResult scanEngineInstallationStructure(Path installationDir, ScanResult scanResult) throws IOException {
        Path modulesDir = installationDir.resolve("modules");
        if (Files.isDirectory(modulesDir)) {
            scanDir(modulesDir, scanResult);
        }
        Path libDir = installationDir.resolve("lib");
        if (Files.isDirectory(libDir)) {
            scanDir(libDir, scanResult);
        }
        return scanResult;
    }

    private ScanResult scanNetBeansInstallationStructure(Path installationDir, Path clustersFile, ScanResult scanResult) throws IOException {

        Set<String> excludedClusterNames = new HashSet<>();
        Collections.addAll(excludedClusterNames, config.excludedClusterNames());

        ArrayList<Path> clusterPaths = new ArrayList<>();

        // first put the modules on the classpath which are found in system resp. AppData\Roaming\SNAP
        // in those directories module updates can be stored an should have precedence over those initially
        // installed in the installation directory
        Path unixNbUserDir = config.userDir().resolve("system");
        if (Files.isDirectory(unixNbUserDir)) {
            clusterPaths.add(unixNbUserDir);
        }
        Path windowsNbUserDir = Paths.get(System.getProperty("user.home")).resolve("AppData").resolve("Roaming").resolve("SNAP");
        if (Files.isDirectory(windowsNbUserDir)) {
            clusterPaths.add(windowsNbUserDir);
        }

        try {
            List<String> clusterNames = Files.readAllLines(clustersFile);
            clusterNames.stream().filter(clusterName -> !excludedClusterNames.contains(clusterName)).forEach(clusterName -> {
                Path clusterPath = installationDir.resolve(clusterName);
                if (Files.isDirectory(clusterPath)) {
                    clusterPaths.add(clusterPath);
                }
            });
        } catch (IOException e) {
            fail(e);
        }
        for (String pathName : config.preferences().get("snap.extraClusters", "").split(File.pathSeparator)) {
            if (!pathName.isEmpty()) {
                Path clusterPath = Paths.get(pathName);
                if (Files.isDirectory(clusterPath)) {
                    clusterPaths.add(clusterPath);
                }
            }
        }

        Set<String> excludedModuleNames = new HashSet<>();
        String[] moduleNames = config.excludedModuleNames();
        for (String mavenName : moduleNames) {
            if (mavenName.indexOf(':') == -1) {
                mavenName = "org.esa.snap:" + mavenName;
            }
            String netBeansName = mavenName.replace(':', '-').replace('.', '-');
            excludedModuleNames.add(netBeansName + ".jar");
        }

        if (!clusterPaths.isEmpty()) {
            for (Path clusterPath : clusterPaths) {
                scanNetBeansCluster(clusterPath, excludedModuleNames, scanResult);
            }
        } else {
            fail("No classpath entries found");
        }

        return scanResult;
    }

    private void scanNetBeansCluster(Path clusterDir, Set<String> excludedModuleNames, ScanResult scanResult) throws IOException {
        Path modulesDir = clusterDir.resolve(Paths.get("modules"));

        if (!Files.isDirectory(modulesDir)) {
            return;
        }

        // Collect module JARs
        List<Path> moduleJarFiles = Files.list(modulesDir)
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> {
                    Path fileName = path.getFileName();
                    String name = fileName.toString();
                    return name.endsWith(JAR_EXT) && !excludedModuleNames.contains(name) &&
                           // if already present don't consider this module
                           !scanResult.classPathEntries.stream().filter(path2 -> path2.endsWith(fileName)).findAny().isPresent();
                })
                .collect(Collectors.toList());
        for (Path moduleJarFile : moduleJarFiles) {
            scanResult.classPathEntries.add(moduleJarFile);
        }

        // Retrieve list of included module names
        Set<String> includedModuleNames = new HashSet<>();
        for (Path moduleJarFile : moduleJarFiles) {
            String moduleJarName = moduleJarFile.getFileName().toString();
            String moduleName = moduleJarName.substring(0, moduleJarName.length() - JAR_EXT.length()).replace('.', '-');
            includedModuleNames.add(moduleName);
        }

        // Collect external JAR dependencies for each included module
        Path extDir = modulesDir.resolve(Paths.get("ext"));
        if (Files.isDirectory(extDir)) {
            List<Path> subDirs = Files.list(extDir).filter(p -> Files.isDirectory(p)).collect(Collectors.toList());
            for (Path subDir : subDirs) {
                String moduleName = subDir.getFileName().toString().replace('.', '-');
                if (includedModuleNames.contains(moduleName)) {
                    scanDir(subDir, scanResult);
                }
            }
        }

        // Collect native library dependencies
        scanNativeLibraryPaths(modulesDir, scanResult);
    }

    private void scanNativeLibraryPaths(Path modulesDir, ScanResult scanResult) {
        Path libDir = modulesDir.resolve(Paths.get("lib"));
        if (Files.isDirectory(libDir)) {
            scanResult.libraryPathEntries.add(libDir);
            Path libArchDir = libDir.resolve(System.getProperty("os.arch"));
            if (Files.isDirectory(libArchDir)) {
                scanResult.libraryPathEntries.add(libArchDir);
                Path libArchOsDir = libArchDir.resolve(System.getProperty("os.name"));
                if (Files.isDirectory(libArchOsDir)) {
                    scanResult.libraryPathEntries.add(libArchOsDir);
                }
            }
        }
    }

    private void scanDir(Path dir, ScanResult scanResult) throws IOException {
        List<Path> entries = Files.list(dir).collect(Collectors.toList());

        scanResult.classPathEntries.addAll(entries.stream()
                                                   .filter(path -> Files.isRegularFile(path))
                                                   .filter(path -> path.getFileName().toString().endsWith(JAR_EXT))
                                                   .collect(Collectors.toList()));

        for (Path entry : entries) {
            if (Files.isDirectory(entry)) {
                scanDir(entry, scanResult);
            }
        }
    }

    private void fail(String s) {
        throw new RuntimeException(s);
    }

    private void fail(Exception e) {
        throw new RuntimeException(e);
    }

    static class ScanResult {

        List<Path> classPathEntries;
        List<Path> libraryPathEntries;

        ScanResult() {
            classPathEntries = new ArrayList<>();
            libraryPathEntries = new ArrayList<>();
        }
    }

}
