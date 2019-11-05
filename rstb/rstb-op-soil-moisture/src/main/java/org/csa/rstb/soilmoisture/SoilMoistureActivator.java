package org.csa.rstb.soilmoisture;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Activator class for deploying soil moisture resources to the aux data dir
 *
 */
public class SoilMoistureActivator implements Activator {

    @Override
    public void start() {
        Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata/sm_luts");
        Path auxdataDirectory = SystemUtils.getApplicationDataDir().toPath().resolve("auxdata").resolve("sm_luts");
        if (auxdataDirectory == null) {
            SystemUtils.LOG.severe("Failed to retrieve auxdata path");
            return;
        }
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            resourceInstaller.install(".*", ProgressMonitor.NULL);
            fixUpPermissions(auxdataDirectory);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Failed to create " + auxdataDirectory);
            return;
        }
    }

    @Override
    public void stop() {
        // Purposely no-op
    }

    private static void fixUpPermissions(Path destPath) throws IOException {
        final Stream<Path> files = Files.list(destPath);
        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                try {
                    fixUpPermissions(path);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Failed to fix permissions on " + path);
                }
            }
        });
    }
}
