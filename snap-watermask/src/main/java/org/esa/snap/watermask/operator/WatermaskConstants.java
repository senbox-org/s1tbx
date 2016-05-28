package org.esa.snap.watermask.operator;

import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * Holds static constants for retrieving auxdata from the remote host.
 * @author olafd
 */
public class WatermaskConstants {
    // the 'preliminary final' HTTP location (might get a versioning later, tbd):
    public static final String REMOTE_HTTP_HOST = "http://step.esa.int";

    public static final String REMOTE_HTTP_PATH = "/auxdata/watermask/images/";

    public static final Path LOCAL_AUXDATA_PATH = SystemUtils.getAuxDataPath().resolve("watermask").toAbsolutePath();

    // we have:
    // 50m.zip
    // 150m.zip
    // GC_water_mask.zip
    // MODIS_north_water_mask.zip
    // MODIS_south_water_mask.zip
    public static final String[] AUXDATA_FILENAMES = {
            "50m.zip",
            "150m.zip",
            "GC_water_mask.zip",
            "MODIS_north_water_mask.zip",
            "MODIS_south_water_mask.zip"};
}
