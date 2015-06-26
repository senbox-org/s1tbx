package org.esa.s1tbx;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.util.ResourceInstaller;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.TreeCopier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class to install resources
 * Usually called within OnStartUp of a module
 */
public class S1TBXSetup {

    public static void installColorPalettes(final Class callingClass) {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(callingClass);
        final Path auxdataDir = getColorPalettesDir();
        Path sourcePath = moduleBasePath.resolve("org/esa/s1tbx/auxdata/color_palettes");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourcePath, auxdataDir);

        try {
            resourceInstaller.install(".*.cpd", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to install colour palettes "+moduleBasePath+" to "+auxdataDir+" "+e.getMessage());
        }
    }

    private static Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }

    private static Path getGraphsDir() {
        return SystemUtils.getApplicationDataDir().toPath().resolve("graphs");
    }

    public static void installGraphs(final Class callingClass, final String path) {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(callingClass);
        final Path srcGraphPath = moduleBasePath.resolve(path);
        final Path dstGraphPath = getGraphsDir();
        //final ResourceInstaller resourceInstaller = new ResourceInstaller(moduleBasePath, "org/esa/s1tbx/graphs/",
        //                                                                  dstGraphPath);

        try {
            if (!Files.exists(dstGraphPath)) {
                Files.createDirectories(dstGraphPath);
            }
            TreeCopier.copy(srcGraphPath, dstGraphPath);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to install graphs "+srcGraphPath+" to "+dstGraphPath+" "+e.getMessage());
        }
    }
}
