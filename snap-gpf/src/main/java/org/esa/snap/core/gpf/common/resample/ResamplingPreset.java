package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by obarrile on 13/04/2019.
 */
public class ResamplingPreset {

    public static final String FILENAME_EXTENSION = ".res";

    private String resamplingPresetName;
    private ArrayList<BandResamplingPreset> bandResamplingPresets;

    public ResamplingPreset (String resamplingPresetName, ArrayList<BandResamplingPreset> bandResamplingPresets) {
        this.resamplingPresetName = resamplingPresetName;
        this.bandResamplingPresets = new ArrayList();
        for (BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
            this.bandResamplingPresets.add(bandResamplingPreset);
        }
    }
    /**
     * Loads a resampling preset from the given file
     *
     * @param file the file
     *
     * @return the resampling preset, never null
     *
     * @throws IOException if an I/O error occurs
     */
    public static ResamplingPreset loadResamplingPreset(final File file) throws IOException {

        ArrayList<BandResamplingPreset> bandResamplingPresets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {
            while(reader.ready()) {
                String line = reader.readLine();
                BandResamplingPreset bandResamplingPreset = BandResamplingPreset.loadBandResamplingPreset(line);
                if(bandResamplingPreset == null) {
                    continue;
                }
                bandResamplingPresets.add(bandResamplingPreset);
            }
        }
        FileUtils.getFilenameWithoutExtension(file);
        return new ResamplingPreset(FileUtils.getFilenameWithoutExtension(file),bandResamplingPresets);
    }

    public String getResamplingPresetName() {
        return resamplingPresetName;
    }

    public ArrayList<BandResamplingPreset> getBandResamplingPresets() {
        return bandResamplingPresets;
    }

    public BandResamplingPreset getBandResamplingPreset (String bandName) {
        for (BandResamplingPreset bandResamplingPreset : bandResamplingPresets) {
            if(bandResamplingPreset.getBandName().equals(bandName)){
                return bandResamplingPreset;
            }
        }
        return null;
    }
}
