package org.esa.snap.core.gpf.common.resample;

/**
 * Created by obarrile on 13/04/2019.
 */
public class BandResamplingPreset {

    public static String SEPARATOR = ":";
    private String bandName;
    private String downsamplingAlias;
    private String upsamplingAlias;

    public BandResamplingPreset (String bandName, String downsamplingAlias, String upsamplingAlias) {
        this.bandName = bandName;
        this.downsamplingAlias = downsamplingAlias;
        this.upsamplingAlias = upsamplingAlias;
    }

    public static BandResamplingPreset loadBandResamplingPreset(String bandResamplingPresetString) {
        if(bandResamplingPresetString.startsWith("#")) {
            return null;
        }
        String[] parts = bandResamplingPresetString.split(SEPARATOR);
        if( parts.length < 3) {
            return null;
        }
        return new BandResamplingPreset(parts[0],parts[1],parts[2]);
    }

    public String getBandName() {
        return bandName;
    }

    public String getDownsamplingAlias() {
        return downsamplingAlias;
    }

    public String getUpsamplingAlias() {
        return upsamplingAlias;
    }

    public void setDownsamplingAlias(String downsamplingAlias) {
        this.downsamplingAlias = downsamplingAlias;
    }

    public void setUpsamplingAlias(String upsamplingAlias) {
        this.upsamplingAlias = upsamplingAlias;
    }


    @Override
    public String toString() {
        return bandName + SEPARATOR + downsamplingAlias + SEPARATOR + upsamplingAlias;
    }
}
