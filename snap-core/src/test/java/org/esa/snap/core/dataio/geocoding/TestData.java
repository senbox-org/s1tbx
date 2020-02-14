package org.esa.snap.core.dataio.geocoding;

public class TestData {

    public static GeoRaster get_AMSR_2_anti_meridian() {
        return new GeoRaster(AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT, null, null, 32, 26,
                32, 26, 5.0, 0.5, 0.5, 1.0, 1.0);
    }

    public static GeoRaster get_AMSRE() {
        return new GeoRaster(AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT,null, null,
                25, 25,5.0);
    }

    public static GeoRaster get_AMSRE_subs_3() {
        return new GeoRaster(TestData.getSubSampled(3, AMSRE.AMSRE_HIGH_RES_LON, 25),
                TestData.getSubSampled(3, AMSRE.AMSRE_HIGH_RES_LAT, 25), null, null,
                9, 9, 25, 25,
                15.0, 0.5, 0.5, 3.0, 3.0);
    }

    public static GeoRaster get_AMSRE_subs_4() {
        return new GeoRaster(TestData.getSubSampled(4, AMSRE.AMSRE_HIGH_RES_LON, 25),
                TestData.getSubSampled(4, AMSRE.AMSRE_HIGH_RES_LAT, 25), null, null,
                7, 7,
                25, 25, 20.0, 0.5, 0.5, 4.0, 4.0);
    }

    public static GeoRaster get_AMSRE_subs_6() {
        return new GeoRaster(TestData.getSubSampled(6, AMSRE.AMSRE_HIGH_RES_LON, 25),
                TestData.getSubSampled(6, AMSRE.AMSRE_HIGH_RES_LAT, 25), null, null,
                5, 5, 25, 25,
                30.0, 0.5, 0.5, 6.0, 6.0);
    }

    public static GeoRaster get_AMSUB() {
        return new GeoRaster(AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT,null, null,
                31, 31, 48.0);
    }

    public static GeoRaster get_AMSUB_subs_3_anti_meridian() {
        return new GeoRaster(TestData.getSubSampled(3, AMSUB.AMSUB_ANTI_MERID_LON, 31),
                TestData.getSubSampled(3, AMSUB.AMSUB_ANTI_MERID_LAT, 31), null, null,
                11, 11, 31, 31,
                48.0, 0.5, 0.5, 3.0, 3.0);
    }

    public static GeoRaster get_AMSUB_subs_5_anti_meridian() {
        return new GeoRaster(TestData.getSubSampled(5, AMSUB.AMSUB_ANTI_MERID_LON, 31),
                TestData.getSubSampled(5, AMSUB.AMSUB_ANTI_MERID_LAT, 31),null, null,
                7, 7, 31, 31,
                80.0, 0.5, 0.5, 5.0, 5.0);
    }

    public static GeoRaster get_AMSUB_subs_6_anti_meridian() {
        return new GeoRaster(TestData.getSubSampled(6, AMSUB.AMSUB_ANTI_MERID_LON, 31),
                TestData.getSubSampled(6, AMSUB.AMSUB_ANTI_MERID_LAT, 31),null, null,
                6, 6, 31, 31,
                96.0, 0.5, 0.5, 6.0, 6.0);
    }

    public static GeoRaster get_MER_RR() {
        return new GeoRaster(MERIS.MER_RR_LON, MERIS.MER_RR_LAT, null, null,5, 5,
                65, 65, 17.6, 0.5, 0.5, 16.0, 16.0);
    }

    public static GeoRaster get_MER_FSG() {
        return new GeoRaster(MERIS.MER_FSG_LON, MERIS.MER_FSG_LAT, null, null,26, 35,
                26, 35, 0.3, 0.5, 0.5, 1.0, 1.0);
    }

    public static GeoRaster get_SLSTR_OL() {
        return new GeoRaster(S3_SYN.SLSTR_OL_LON, S3_SYN.SLSTR_OL_LAT, null, null,32, 26,
                32, 26, 0.3, 0.5, 0.5, 1.0, 1.0);
    }

    public static GeoRaster get_OLCI() {
        return new GeoRaster(OLCI.OLCI_L2_LON, OLCI.OLCI_L2_LAT, null, null,32, 36,
                32, 36, 0.3, 0.5, 0.5, 1.0, 1.0);
    }

    public static GeoRaster get_SYN_AOD() {
        return new GeoRaster(S3_SYN.SYN_AOD_LON, S3_SYN.SYN_AOD_LAT, null, null,25, 27,
                25, 27, 0.3, 0.5, 0.5, 1.0, 1.0);
    }


    public static double[] getSubSampled(int factor, double[] data, int dataWidth) {
        final int height = data.length / dataWidth;
        final int targetSize = (int) (Math.ceil(height / (double) factor) * Math.ceil(dataWidth / (double) factor));

        final double[] subsampledData = new double[targetSize];

        int writeIndex = 0;
        for (int line = 0; line < height; line += factor) {
            for (int col = 0; col < dataWidth; col += factor) {
                subsampledData[writeIndex] = data[line * dataWidth + col];
                ++writeIndex;
            }
        }

        return subsampledData;
    }
}
