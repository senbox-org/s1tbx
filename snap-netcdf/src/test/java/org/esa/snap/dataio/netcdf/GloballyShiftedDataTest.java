package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class GloballyShiftedDataTest {
    // This generates data which mimics the LCCCI data for the Climate Data Store, just with a lower resolution
    // This test relates to https://senbox.atlassian.net/browse/SNAP-950 and https://senbox.atlassian.net/browse/SNAP-951

    private static File tempFile;
    private static final int WIDTH = 8100;
    private static final int HEIGHT = 4050;
    private static final int STEP_HEIGHT = 50;
    private static final int HALF_WIDTH = WIDTH / 2;
    private Product product;

    @BeforeClass
    public static void createTestDataFile() throws IOException {
        // Fails on Linux: "Unable to load library 'netcdf': libnetcdf.so: cannot open shared object file: No such file or directory"
        // why is it failing writing Netcdf files in test mode but not when SNAP is installed?
        // Probably because we use the Nujan lib for writing usually.
        Assume.assumeTrue(isWindows());
        
        tempFile = File.createTempFile(GloballyShiftedDataTest.class.getSimpleName(), ".nc");
//        tempFile = new File(String.format("%s\\%s.nc", System.getProperty("user.home"), GloballyShiftedDataTest.class.getSimpleName()));
        NetcdfFileWriter ncFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, tempFile.getAbsolutePath());
        ncFile.addDimension(null, "lat", HEIGHT);
        ncFile.addDimension(null, "lon", WIDTH);
        Variable lat = ncFile.addVariable(null, "lat", DataType.DOUBLE, "lat");

        Variable lon = ncFile.addVariable(null, "lon", DataType.DOUBLE, "lon");

        Variable data = ncFile.addVariable(null, "data", DataType.INT, "lat lon");

        final double[] latValues = new double[HEIGHT];
        double latStep = 180.0 / HEIGHT;
        for (int i = 0; i < latValues.length; i++) {
            latValues[i] = 90 - (i + 0.5) * latStep;
        }
        final double[] lonValues = new double[WIDTH];
        double lonStep = 360.0 / WIDTH;
        for (int i = 0; i < lonValues.length; i++) {
            lonValues[i] = (i + 0.5) * lonStep;
        }

        ncFile.create();
        try {
            ncFile.write(lat, Array.factory(DataType.DOUBLE, new int[]{HEIGHT}, latValues));
            ncFile.write(lon, Array.factory(DataType.DOUBLE, new int[]{WIDTH},lonValues));
            ncFile.flush();
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }


        try {
            // rightValues are written to the left side of the image, but as the data is globally shifted
            // they will be on the right side when read in
            int[] rightDataValues = new int[HALF_WIDTH * STEP_HEIGHT];
            int[] leftDataValues = new int[HALF_WIDTH * STEP_HEIGHT];
            Arrays.setAll(rightDataValues, i -> i + HALF_WIDTH);
            Arrays.setAll(leftDataValues, i -> i);
            Array rightValues = Array.factory(DataType.INT, new int[]{STEP_HEIGHT, HALF_WIDTH}, rightDataValues);
            Array leftValues = Array.factory(DataType.INT, new int[]{STEP_HEIGHT, HALF_WIDTH}, leftDataValues);
            for (int i = 0; i < HEIGHT; i = i + STEP_HEIGHT) {
                ncFile.write(data, new int[]{i, 0}, rightValues);
                ncFile.write(data, new int[]{i, HALF_WIDTH}, leftValues);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }

        ncFile.close();
    }

    @Before
    public void readProduct() throws Exception {
        product = ProductIO.readProduct(tempFile);
    }

    @AfterClass
    public static void deleteTestDataFile() {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    @After
    public void tearDown() {
        if (product != null) {
            product.dispose();
        }
        product = null;
    }

    @Test
    public void readingData() throws IOException {
        Band band = product.getBandAt(0);
        int[] actualValues = new int[HALF_WIDTH * STEP_HEIGHT];

        int[] expectedLeftValues = new int[HALF_WIDTH * STEP_HEIGHT];
        Arrays.setAll(expectedLeftValues, i -> i);
        Arrays.fill(actualValues, -1);
        band.readPixels(0, 0, HALF_WIDTH, STEP_HEIGHT, actualValues);
        Assert.assertArrayEquals(expectedLeftValues, actualValues);

        int[] expectedRightValues = new int[HALF_WIDTH * STEP_HEIGHT];
        Arrays.setAll(expectedRightValues, i -> i + HALF_WIDTH);
        Arrays.fill(actualValues, -1);
        band.readPixels(WIDTH / 2, 0, WIDTH / 2, STEP_HEIGHT, actualValues);
        Assert.assertArrayEquals(expectedRightValues, actualValues);
    }

    @Test
    public void gettingDataLevel0() {
        Band band = product.getBandAt(0);

        int maxLevel = band.getMultiLevelModel().getLevelCount() - 1;
        RenderedImage maxLevelImage = band.getSourceImage().getImage(maxLevel);
        try {
            maxLevelImage.getData();
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("Issue SNAP-950 not fixed??");
        }

    }

    @Ignore("Not fixed yet: SNAP-951")
    @Test
    public void testLevelImagesNotScrambled() {
        Band band = product.getBandAt(0);

        int maxLevel = band.getMultiLevelModel().getLevelCount() - 1;
        for (int level = 0; level < maxLevel; level++) {
            RenderedImage levelImage = band.getSourceImage().getImage(level);
            Rectangle rect = new Rectangle(0, 0, levelImage.getWidth(), 1);
            DataBuffer buffer = levelImage.getData(rect).getDataBuffer();
            int expectedStep = buffer.getElem(1) - buffer.getElem(0);
            for (int i = 1; i < buffer.getSize(); i++) {
                int actualStep = buffer.getElem(i) - buffer.getElem(i - 1);
                String msg = String.format("Unexpected step size (%d != %d) at sample index %d level %d", expectedStep, actualStep, i, level);
                assertEquals(msg, expectedStep, actualStep);
            }

            // for visual inspection
//            String userHome = System.getProperty("user.home");
//            String path = String.format("%s\\%s_Level%d.png", userHome, GloballyShiftedDataTest.class.getSimpleName(), level);
//            File imageFile = new File(path);
              // needs to be tiff 32-bit int is not supported by png
//            ImageIO.write(levelImage, "TIFF", imageFile);
        }

    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

}
