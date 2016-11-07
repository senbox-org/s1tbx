package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Registers the installed tool adapters and, for those that have bundles to be installed, installs the bundles.
 *
 * @author Cosmin Cara
 */
public class ToolAdapterActivator implements Activator {

    private Map<String, Path> bundleMap = new HashMap<>();
    private static final Map<Path, Set<ToolAdapterOperatorDescriptor>> dependentInstallations = Collections.synchronizedMap(new HashMap<>());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        Path jarPaths = SystemUtils.getApplicationDataDir().toPath().resolve("modules");
        Map<String, File> jarAdapters = getJarAdapters(jarPaths.toFile());
        if (spiRegistry != null) {
            Collection<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
            if (operatorSpis != null) {
                final Collection<ToolAdapterOpSpi> toolAdapterOpSpis = ToolAdapterIO.searchAndRegisterAdapters();
                operatorSpis.addAll(toolAdapterOpSpis);
                final List<ToolAdapterOpSpi> opWithBundles = toolAdapterOpSpis.stream()
                        .filter(spi ->
                                spi.getOperatorDescriptor() instanceof ToolAdapterOperatorDescriptor &&
                                        ((ToolAdapterOperatorDescriptor) spi.getOperatorDescriptor()).getBundle() != null)
                        .collect(Collectors.toList());
                readMap();
                for (ToolAdapterOpSpi opWithBundle : opWithBundles) {
                    final ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) opWithBundle.getOperatorDescriptor();
                    Bundle bundle = (operatorDescriptor).getBundle();
                    File targetLocation = bundle.getTargetLocation();
                    String entryPoint = bundle.getEntryPoint();
                    if (targetLocation != null && entryPoint != null) {
                        Path target = targetLocation.toPath().resolve(entryPoint);
                        String alias = opWithBundle.getOperatorAlias();
                        if (!Files.exists(target) || dependentInstallations.containsKey(target)) {
                            if (!dependentInstallations.containsKey(target)) {
                                dependentInstallations.put(target, new HashSet<>());
                            }
                            bundleMap.remove(alias);
                            dependentInstallations.get(target).add(operatorDescriptor);
                            SystemUtils.LOG.info(String.format("Start installing bundle for %s", operatorDescriptor.getAlias()));
                            installBundle(operatorDescriptor);
                        } else if (!bundleMap.containsKey(alias)) {
                            bundleMap.put(alias, target);
                        }
                    }
                }
                saveMap();
            }
        }
    }

    @Override
    public void stop() {

    }

    private void installBundle(ToolAdapterOperatorDescriptor descriptor) {
        Bundle descriptorBundle = descriptor.getBundle();
        Path sourcePath = SystemUtils.getApplicationDataDir().toPath()
                                    .resolve("modules")
                                    .resolve("lib")
                                    .resolve(descriptorBundle.getEntryPoint());
        switch (descriptorBundle.getBundleType()) {
            case ARCHIVE:
                executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            uncompress(sourcePath, descriptorBundle);
                        } finally {
                            installFinished(descriptor);
                        }
                        return null;
                    }
                });
                break;
            case INSTALLER:
                executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            install(sourcePath, descriptorBundle);
                        } finally {
                            installFinished(descriptor);
                        }
                        return null;
                    }
                });
                break;
            default:
                executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            copy(sourcePath, descriptorBundle);
                        } finally {
                            installFinished(descriptor);
                        }
                        return null;
                    }
                });
                break;
        }
    }

    private void copy(Path source, Bundle bundle) throws IOException {
        File targetLocation = bundle.getTargetLocation();
        if (targetLocation == null) {
            throw new IOException("No target defined");
        }
        Path targetPath = targetLocation.toPath();
        if (!Files.exists(targetPath)) {
            Files.createDirectory(targetPath);
        }
        ToolAdapterIO.copy(source, targetPath);
    }

    private void uncompress(Path source, Bundle bundle) throws IOException {
        File targetLocation = bundle.getTargetLocation();
        if (targetLocation == null) {
            throw new IOException("No target defined");
        }
        ToolAdapterIO.unzip(source, targetLocation.toPath());
    }

    private void install(Path source, Bundle bundle) throws Exception {
        throw new NoSuchMethodException("Installers are not yet supported");
    }

    private Map<String, File> getJarAdapters(File fromPath) {
        Map<String, File> output = new HashMap<>();
        if (fromPath != null && fromPath.exists()) {
            String descriptionKeyName = "OpenIDE-Module-Short-Description";
            Attributes.Name typeKey = new Attributes.Name("OpenIDE-Module-Type");
            File[] files = fromPath.listFiles((dir, name) -> name.endsWith("jar"));
            if (files != null) {
                try {
                    for (File file : files) {
                        JarFile jarFile = new JarFile(file);
                        Manifest manifest = jarFile.getManifest();
                        Attributes manifestEntries = manifest.getMainAttributes();
                        if (manifestEntries.containsKey(typeKey) &&
                                "STA".equals(manifestEntries.getValue(typeKey.toString()))) {
                            output.put(manifestEntries.getValue(descriptionKeyName), file);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return output;
    }

    private void saveMap() {
        Path path = ToolAdapterIO.getAdaptersPath().resolve("bundles.dat");
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Path> entry : bundleMap.entrySet()) {
            builder.append(entry.getKey())
                    .append(",")
                    .append(entry.getValue().toString())
                    .append("\n");
        }
        try {
            Files.write(path, builder.toString().getBytes());
        } catch (IOException e) {
            SystemUtils.LOG.severe(String.format("ToolAdapterIO: %s", e.getMessage()));
        }
    }

    private void readMap() {
        Path path = ToolAdapterIO.getAdaptersPath().resolve("bundles.dat");
        bundleMap.clear();
        try {
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] tokens = line.split(",");
                    bundleMap.put(tokens[0], Paths.get(tokens[1]));
                }
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(String.format("ToolAdapterIO: %s", e.getMessage()));
        }
    }

    private void installFinished(ToolAdapterOperatorDescriptor descriptor) {
        Bundle bundle = descriptor.getBundle();
        Path target = bundle.getTargetLocation().toPath().resolve(bundle.getEntryPoint());
        String alias = descriptor.getAlias();
        if (Files.exists(target)) {
            Set<ToolAdapterOperatorDescriptor> dependants = dependentInstallations.get(target);
            for (ToolAdapterOperatorDescriptor dependant : dependants) {
                bundleMap.put(dependant.getAlias(), target);
            }
            saveMap();
            dependentInstallations.remove(target);
            SystemUtils.LOG.info(String.format("Installation of bundle for %s completed", alias));
        } else {
            SystemUtils.LOG.severe(String.format("Installation of bundle for %s failed", alias));
        }
    }
}
