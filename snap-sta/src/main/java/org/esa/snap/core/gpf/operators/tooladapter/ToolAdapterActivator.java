package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by kraftek on 12/28/2015.
 *
 * TODO: 1. Search for uncompressed adapters
 * TODO: 2. Look for jar adapters that have not been uncompressed
 * TODO: 3. Look for nbm modules that have not been uncompressed
 */
public class ToolAdapterActivator implements Activator {
    @Override
    public void start() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        Path jarPaths = SystemUtils.getApplicationDataDir().toPath().resolve("modules");
        Map<String, File> jarAdapters = getJarAdapters(jarPaths.toFile());
        if (spiRegistry != null) {
            Collection<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
            if (operatorSpis != null) {
                if (operatorSpis.size() == 0) {
                    operatorSpis.addAll(ToolAdapterIO.searchAndRegisterAdapters());
                }
            }
        }
    }

    @Override
    public void stop() {

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
}
