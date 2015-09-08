package org.esa.s1tbx;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.datamodel.RGBImageProfile;
import org.esa.snap.framework.datamodel.RGBImageProfileManager;
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

    static{
        registerRGBProfiles();
    }

    public static void installColorPalettes(final Class callingClass, final String path) {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(callingClass);
        final Path auxdataDir = getColorPalettesDir();
        Path sourcePath = moduleBasePath.resolve(path);
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

    private static void registerRGBProfiles() {
        final RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("Pauli",
                                               new String[]{
                                                       "((i_HH-i_VV)*(i_HH-i_VV)+(q_HH-q_VV)*(q_HH-q_VV))/2",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/2",
                                                       "((i_HH+i_VV)*(i_HH+i_VV)+(q_HH+q_VV)*(q_HH+q_VV))/2"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Sinclair",
                                               new String[]{
                                                       "i_VV*i_VV+q_VV*q_VV",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/4",
                                                       "i_HH*i_HH+q_HH*q_HH"
                                               }
        ));

        // Intensity
        manager.addProfile(createDPRatioProfile("Intensity", "HH", "HV", ""));
        manager.addProfile(createDPRatioProfile("Intensity", "VV", "VH", ""));
        manager.addProfile(createDPRatioProfile("Intensity", "HH", "VV", ""));
        // Intensity dB
        manager.addProfile(createDPRatioProfile("Intensity", "HH", "HV", "_db"));
        manager.addProfile(createDPRatioProfile("Intensity", "VV", "VH", "_db"));
        manager.addProfile(createDPRatioProfile("Intensity", "HH", "VV", "_db"));

        // Sigma0
        manager.addProfile(createDPRatioProfile("Sigma0", "HH", "HV", ""));
        manager.addProfile(createDPRatioProfile("Sigma0", "VV", "VH", ""));
        manager.addProfile(createDPRatioProfile("Sigma0", "HH", "VV", ""));
        // Sigma0 dB
        manager.addProfile(createDPRatioProfile("Sigma0", "HH", "HV", "_db"));
        manager.addProfile(createDPRatioProfile("Sigma0", "VV", "VH", "_db"));
        manager.addProfile(createDPRatioProfile("Sigma0", "HH", "VV", "_db"));

        // Intensity
        manager.addProfile(createDPDiffProfile("Intensity", "HH", "HV", ""));
        manager.addProfile(createDPDiffProfile("Intensity", "VV", "VH", ""));
        manager.addProfile(createDPDiffProfile("Intensity", "HH", "VV", ""));
        // Intensity dB
        manager.addProfile(createDPDiffProfile("Intensity", "HH", "HV", "_db"));
        manager.addProfile(createDPDiffProfile("Intensity", "VV", "VH", "_db"));
        manager.addProfile(createDPDiffProfile("Intensity", "HH", "VV", "_db"));

        // Sigma0
        manager.addProfile(createDPDiffProfile("Sigma0", "HH", "HV", ""));
        manager.addProfile(createDPDiffProfile("Sigma0", "VV", "VH", ""));
        manager.addProfile(createDPDiffProfile("Sigma0", "HH", "VV", ""));
        // Sigma0 dB
        manager.addProfile(createDPDiffProfile("Sigma0", "HH", "HV", "_db"));
        manager.addProfile(createDPDiffProfile("Sigma0", "VV", "VH", "_db"));
        manager.addProfile(createDPDiffProfile("Sigma0", "HH", "VV", "_db"));
    }

    private static RGBImageProfile createDPRatioProfile(final String name, final String pol1, final String pol2, final String suffix) {
        return new RGBImageProfile("Dual Pol Ratio "+name+suffix+" "+pol1+"+"+pol2,
                                               new String[]{
                                                       name+"_"+pol1+suffix,
                                                       name+"_"+pol2+suffix,
                                                       name+"_"+pol1+suffix+"/"+name+"_"+pol2+suffix
                                               }
        );
    }

    private static RGBImageProfile createDPDiffProfile(final String name, final String pol1, final String pol2, final String suffix) {
        return new RGBImageProfile("Dual Pol Difference "+name+suffix+" "+pol1+"+"+pol2,
                                   new String[]{
                                           name+"_"+pol2+suffix,
                                           name+"_"+pol1+suffix,
                                           name+"_"+pol1+suffix+"-"+name+"_"+pol2+suffix
                                   }
        );
    }
}
