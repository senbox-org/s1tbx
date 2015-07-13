
package org.esa.snap.configurator;


import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Storage for performance parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class PerformanceParameters {


    /**
     * Preferences key for the default tile size in pixels
     */
    public static final String PROPERTY_DEFAULT_TILE_SIZE = "snap.jai.defaultTileSize";

    /**
     * Preferences key for the cache size in Mb
     */
    public static final String PROPERTY_JAI_CACHE_SIZE = "snap.jai.tileCacheSize";

    /**
     * Preferences key for the number of processors which may be employed for JAI image processing.
     */
    public static final String PROPERTY_JAI_PARALLELISM = "snap.jai.parallelism";


    private VMParameters vmParameters;
    private Path userDir;
    private int nbThreads;

    private int defaultTileSize;
    private int cacheSize;

    /**
     * Default constructor
     */
    public PerformanceParameters() {}

    /**
     * Cloning constructor
     *
     * @param clone the performance parameter to copy
     */
    public PerformanceParameters(PerformanceParameters clone) {
        this.setVMParameters(clone.vmParameters.toString());
        this.setUserDir(clone.getUserDir());

        this.setNbThreads(clone.getNbThreads());
        this.setDefaultTileSize(clone.getDefaultTileSize());
        this.setCacheSize(clone.getCacheSize());
    }

    public void setVmXMX(long vmXMX) {
        vmParameters.setVmXMX(vmXMX);
    }

    public void setVmXMS(long vmXMS) {
        vmParameters.setVmXMS(vmXMS);
    }


    public Path getUserDir() {
        return userDir;
    }

    public void setUserDir(Path largeTileCache) {
        this.userDir = largeTileCache;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    public void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }

    /**
     * Build a string from vm parameters
     *
     * @return VM parameters as a String
     */
    public String getVMParameters() {
        return vmParameters.toString();
    }


    /**
     * set the Xmx, Xms and other parameters from a string
     *
     * @param vmParametersLine the vm parameters string to parse
     */
    public void setVMParameters(String vmParametersLine) {
        vmParameters = new VMParameters(vmParametersLine);
    }



    public int getDefaultTileSize() {
        return defaultTileSize;
    }

    public void setDefaultTileSize(int defaultTileSize) {
        this.defaultTileSize = defaultTileSize;
    }
    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }


    /**
     *
     * Reads the parameters files and system settings to retreive the actual performance parameters.
     * It updates the "actualParameters" according to the configuration loaded.
     *
     * @return the actual performance parameters, loaded by this method
     */
    synchronized static PerformanceParameters loadConfiguration() {

        Config configuration = Config.instance().load();
        Preferences preferences = configuration.preferences();

        PerformanceParameters actualParameters = new PerformanceParameters();

        VMParameters netBeansVmParameters = VMParameters.load();
        actualParameters.setVMParameters(netBeansVmParameters.toString());
        actualParameters.setUserDir(configuration.userDir());

        final int defaultNbThreads = JavaSystemInfos.getInstance().getNbCPUs();
        actualParameters.setNbThreads(preferences.getInt(PROPERTY_JAI_PARALLELISM, defaultNbThreads));
        actualParameters.setDefaultTileSize(preferences.getInt(PROPERTY_DEFAULT_TILE_SIZE, 0));
        actualParameters.setCacheSize(preferences.getInt(PROPERTY_JAI_CACHE_SIZE, 0));

        return actualParameters;
    }

    /**
     * Save the actual configuration
     *
     * @param confToSave The configuration to save
     */
    synchronized static void saveConfiguration(PerformanceParameters confToSave) throws IOException, BackingStoreException {

        confToSave.vmParameters.save();

        Config configuration = EngineConfig.instance().load();
        Preferences preferences = configuration.preferences();

        String userDirString = confToSave.getUserDir().toString();

        String[] diskNames = JavaSystemInfos.getInstance().getDisksNames();
        for(String diskName : diskNames) {
            if (diskName.equalsIgnoreCase(userDirString)) {
                userDirString = userDirString + File.separator + "." + SystemUtils.getApplicationContextId();
                File contextFolderAsFile = new File(userDirString);
                if(!contextFolderAsFile.mkdir()) {
                    SystemUtils.LOG.severe("Could not create user dir " + userDirString);
                } else {
                    confToSave.setUserDir(FileUtils.getPathFromURI(contextFolderAsFile.toURI()));
                }
                break;
            }
        }
        preferences.put(EngineConfig.PROPERTY_USER_DIR, userDirString);

        preferences.putInt(PROPERTY_JAI_PARALLELISM, confToSave.getNbThreads());
        preferences.putInt(PROPERTY_DEFAULT_TILE_SIZE, confToSave.getDefaultTileSize());
        preferences.putInt(PROPERTY_JAI_CACHE_SIZE, confToSave.getCacheSize());
        preferences.flush();
    }


}
