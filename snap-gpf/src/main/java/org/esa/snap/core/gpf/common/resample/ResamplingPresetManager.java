package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by obarrile on 13/04/2019.
 */
public class ResamplingPresetManager {
    private final List<ResamplingPreset> _resamplingPresets;
    private final static File resamplingPresetsDir;

    static {
        resamplingPresetsDir = SystemUtils.getAuxDataPath().resolve("resampling_presets").toFile();
        if (!resamplingPresetsDir.exists()) {
            resamplingPresetsDir.mkdirs();
        }
    }

    private ResamplingPresetManager() {
        _resamplingPresets = new ArrayList<>();
        loadDefaultResamplingPresets();
    }

    public static File getResamplingPresetsDir() {
        return resamplingPresetsDir;
    }

    public static ResamplingPresetManager getInstance() {
        return Holder.instance;
    }

    public void addResamplingPreset(ResamplingPreset preset) {
        if (preset != null && !_resamplingPresets.contains(preset)) {
            _resamplingPresets.add(preset);
        }
    }

    public void removeResamplingPreset(ResamplingPreset preset) {
        if (preset != null) {
            _resamplingPresets.remove(preset);
        }
    }

    public int getResamplingPresetCount() {
        return _resamplingPresets.size();
    }

    public ResamplingPreset getResamplingPreset(int i) {
        return _resamplingPresets.get(i);
    }

    public ResamplingPreset getResamplingPreset(String resamplingName) {
        for(ResamplingPreset resamplingPreset : _resamplingPresets) {
            if(resamplingPreset.getResamplingPresetName().equals(resamplingName)){
                return resamplingPreset;
            }
        }
        return null;
    }

    public ResamplingPreset[] getAllResamplingPresets() {
        return _resamplingPresets.toArray(new ResamplingPreset[_resamplingPresets.size()]);
    }

    /**
     * Loads all profiles from the BEAM default RGB profile location.
     */
    private void loadDefaultResamplingPresets() {
        if (!getResamplingPresetsDir().exists()) {
            resamplingPresetsDir.mkdirs();
            SystemUtils.LOG.log(Level.INFO, "Directory for Resampling presets not found: " + getResamplingPresetsDir());
        }
        final File[] files = getResamplingPresetsDir().listFiles(
                (dir, name) -> name.endsWith(ResamplingPreset.FILENAME_EXTENSION) || name.endsWith(
                        ResamplingPreset.FILENAME_EXTENSION.toUpperCase()));
        if (files != null) {
            for (File file : files) {
                try {
                    addResamplingPreset(ResamplingPreset.loadResamplingPreset(file));
                } catch (IOException e) {
                    SystemUtils.LOG.log(Level.SEVERE, "Failed to load Resampling preset from " + file, e);
                }
            }
        } else {
            SystemUtils.LOG.log(Level.INFO, "No Resampling presets found in " + getResamplingPresetsDir());
        }
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final ResamplingPresetManager instance = new ResamplingPresetManager();
    }
}
