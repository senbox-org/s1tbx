package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.runtime.Config;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class PixelGeoCoding2Test {

    private static final int S = 4;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    @Test
    public void testGetPixelPos_PixelCenterAccuracy() throws Exception {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.fractionAccuracy", false);
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", false);
            Product product = createProduct();
            GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                             product.getBand("lonBand"), null, 5,
                                                                             ProgressMonitor.NULL);
            product.setSceneGeoCoding(pixelGeoCoding);

            PixelPos calculatedPixelPos = new PixelPos();
            GeoPos gp = new GeoPos();
            double expectedDistance = Math.sqrt(Math.pow(0.25, 2) + Math.pow(0.25, 2));
            double[] offsets = new double[]{0.25, 0.75};
            for (int i = 0; i < 100; i++) {
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        for (double xOffset : offsets)
                            for (double yOffset : offsets) {
                                final PixelPos originalPixelPos = new PixelPos(x + xOffset, y + yOffset);
                                pixelGeoCoding.getGeoPos(originalPixelPos, gp);
                                pixelGeoCoding.getPixelPos(gp, calculatedPixelPos);
                                assertEquals(expectedDistance, calculatedPixelPos.distance(originalPixelPos), 1e-8);
                            }
                    }
                }
            }
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.fractionAccuracy");
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    @Test
    public void testGetPixelPos_FractionAccuracy() throws Exception {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.fractionAccuracy", true);
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", true);
            Product product = createProduct();
            GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                             product.getBand("lonBand"), null, 5,
                                                                             ProgressMonitor.NULL);
            product.setSceneGeoCoding(pixelGeoCoding);


            PixelPos calculatedPixelPos = new PixelPos();
            GeoPos gp = new GeoPos();
            double[] offsets = new double[]{0.25, 0.75};
            for (int i = 0; i < 100; i++) {
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        for (double xOffset : offsets)
                            for (double yOffset : offsets) {
                                final PixelPos originalPixelPos = new PixelPos(x + xOffset, y + yOffset);
                                pixelGeoCoding.getGeoPos(originalPixelPos, gp);
                                pixelGeoCoding.getPixelPos(gp, calculatedPixelPos);
                                assertEquals(0, calculatedPixelPos.distance(originalPixelPos), 2.5e-7);
                            }
                    }
                }
            }
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.fractionAccuracy");
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    @Test
    @Ignore
    public void testGetPixelPos_PixelCenterAccuracy_withTimeStop() throws Exception {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.fractionAccuracy", false);
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", false);
            Product product = createProduct();
            GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                             product.getBand("lonBand"), null, 5,
                                                                             ProgressMonitor.NULL);
            product.setSceneGeoCoding(pixelGeoCoding);

            PixelPos calculatedPixelPos = new PixelPos();
            GeoPos gp = new GeoPos();
            double expectedDistance = Math.sqrt(Math.pow(0.25, 2) + Math.pow(0.25, 2));
            double[] offsets = new double[]{0.25, 0.75};
            StopWatch stopWatch = new StopWatch();
            long totalTime = 0;
            for (int i = 0; i < 100; i++) {
                stopWatch.start();
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        for (double xOffset : offsets)
                            for (double yOffset : offsets) {
                                final PixelPos originalPixelPos = new PixelPos(x + xOffset, y + yOffset);
                                pixelGeoCoding.getGeoPos(originalPixelPos, gp);
                                pixelGeoCoding.getPixelPos(gp, calculatedPixelPos);
                                assertEquals(expectedDistance, calculatedPixelPos.distance(originalPixelPos), 1e-8);
                            }
                    }
                }
                stopWatch.stop();
                totalTime += stopWatch.getTimeDiff();
            }
            System.out.println("Required time: " + StopWatch.getTimeString(totalTime));
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.fractionAccuracy");
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    @Test
    @Ignore
    public void testGetPixelPos_FractionAccuracy_withTimeStop() throws Exception {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.fractionAccuracy", true);
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", true);
            Product product = createProduct();
            GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                             product.getBand("lonBand"), null, 5,
                                                                             ProgressMonitor.NULL);
            product.setSceneGeoCoding(pixelGeoCoding);


            PixelPos calculatedPixelPos = new PixelPos();
            GeoPos gp = new GeoPos();
            double[] offsets = new double[]{0.25, 0.75};
            StopWatch stopWatch = new StopWatch();
            long totalTime = 0;
            for (int i = 0; i < 100; i++) {
                stopWatch.start();
                for (int y = 0; y < product.getSceneRasterHeight(); y++) {
                    for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                        for (double xOffset : offsets)
                            for (double yOffset : offsets) {
                                final PixelPos originalPixelPos = new PixelPos(x + xOffset, y + yOffset);
                                pixelGeoCoding.getGeoPos(originalPixelPos, gp);
                                pixelGeoCoding.getPixelPos(gp, calculatedPixelPos);
                                assertEquals(0, calculatedPixelPos.distance(originalPixelPos), 2.5e-7);
                            }
                    }
                }
                stopWatch.stop();
                totalTime += stopWatch.getTimeDiff();
            }
            System.out.println("Required time: " + StopWatch.getTimeString(totalTime));
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.fractionAccuracy");
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    private Product createProduct() {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, 0.5, 0.5, S, S, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, 0.5, 0.5, S, S, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lonBand", ProductData.TYPE_FLOAT32);

        latBand.setRasterData(ProductData.createInstance(createBandData(latGrid)));
        lonBand.setRasterData(ProductData.createInstance(createBandData(lonGrid)));
        final FlagCoding flagCoding = new FlagCoding("flags");
        flagCoding.addFlag("valid", 0x01, "valid pixel");

        product.getFlagCodingGroup().add(flagCoding);

        Band flagomatBand = product.addBand("flagomat", ProductData.TYPE_UINT8);
        byte[] flagomatData = new byte[PW * PH];
        Arrays.fill(flagomatData, (byte) 0x01);
        flagomatBand.setRasterData(ProductData.createInstance(ProductData.TYPE_UINT8, flagomatData));
        flagomatBand.setSampleCoding(flagCoding);

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        return product;
    }

    private float[] createLatGridData() {
        return createLatGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createLonGridData(LON_1, LON_2);
    }

    private static float[] createBandData(TiePointGrid grid) {
        float[] floats = new float[PW * PH];
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                floats[y * PW + x] = grid.getPixelFloat(x, y);
            }
        }
        return floats;
    }

    private static float[] createLatGridData(float lat0, float lat1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                floats[j * GW + i] = lat0 + (lat1 - lat0) * y * y + 0.1f * (lat1 - lat0) * x * x;
            }
        }

        return floats;
    }

    private static float[] createLonGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                final int index = j * GW + i;
                floats[(index)] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }

        return floats;
    }
}