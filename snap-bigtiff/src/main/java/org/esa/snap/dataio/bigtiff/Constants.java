package org.esa.snap.dataio.bigtiff;

// todo - remove this non-API class (nf - 20150211)
public class Constants {

    public static final int PRIVATE_BEAM_TIFF_TAG_NUMBER = 65000;

    static final String DESCRIPTION = "GeoTIFF / BigTIFF data product";

    static final String[] FORMAT_NAMES = new String[]{BigGeoTiffProductReaderPlugIn.FORMAT_NAME};
    static final String[] FILE_EXTENSIONS = new String[]{".tif", ".tiff"};
}
