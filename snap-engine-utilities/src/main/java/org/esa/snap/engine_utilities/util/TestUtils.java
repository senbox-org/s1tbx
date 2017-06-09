/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.CommonReaders;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Utilities for Operator unit tests
 */
public class TestUtils {

    private static final boolean FailOnSkip = false;
    private static final boolean FailOnLargeTestProducts = false;
    private static final boolean FailOnAllNoData = false;
    private static final int LARGE_DIMENSION = 100;

    private static boolean testEnvironmentInitialized = false;
    public static final String SKIPTEST = "skipTest";

    public static final Logger log = SystemUtils.LOG;

    public static void initTestEnvironment() {
        if (testEnvironmentInitialized)
            return;

        try {
            SystemUtils.init3rdPartyLibs(GPT.class);
            testEnvironmentInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Product readSourceProduct(final File inputFile) throws IOException {
        if (!inputFile.exists()) {
            throw new IOException(inputFile.getAbsolutePath() + " not found");
        }
        final Product product = CommonReaders.readProduct(inputFile);
        if (product == null) {
            throw new IOException("Unable to read " + inputFile.toString());
        }
        return product;
    }

    public static void verifyProduct(final Product product, final boolean verifyTimes, final boolean verifyGeoCoding) throws Exception {
        verifyProduct(product, verifyTimes, verifyGeoCoding, false);
    }

    public static void verifyProduct(final Product product, final boolean verifyTimes, final boolean verifyGeoCoding,
                                     final boolean verifyBandData) throws Exception {
        if (product == null)
            throw new Exception("product is null");
        if (verifyGeoCoding && product.getSceneGeoCoding() == null) {
            SystemUtils.LOG.warning("Geocoding is null for " + product.getFileLocation().getAbsolutePath());
            //throw new Exception("geocoding is null");
        }
        if (product.getMetadataRoot() == null)
            throw new Exception("metadataroot is null");
        if (product.getNumBands() == 0)
            throw new Exception("numbands is zero");
        if (product.getProductType() == null || product.getProductType().isEmpty())
            throw new Exception("productType is null");
        if (verifyTimes) {
            if (product.getStartTime() == null)
                throw new Exception("startTime is null");
            if (product.getEndTime() == null)
                throw new Exception("endTime is null");
        }
        if (verifyBandData && FailOnAllNoData) {
            for (Band b : product.getBands()) {
                if (b.getUnit() == null || b.getUnit().isEmpty())
                    throw new Exception("band " + b.getName() + " has null unit");

                // readPixels gets computeTiles to be executed
                final int w = b.getRasterWidth() / 2;
                final int h = b.getRasterHeight() / 2;
                if (FailOnLargeTestProducts && (w > LARGE_DIMENSION * 2 || h > LARGE_DIMENSION * 2)) {
                    throw new IOException("Test product too large " + w + "," + h);
                }
                final int x0 = w / 2;
                final int y0 = h / 2;

                boolean allNoData = true;
                for (int y = y0; y < y0 + h; ++y) {
                    final float[] floatValues = new float[w];
                    b.readPixels(x0, y, w, 1, floatValues, ProgressMonitor.NULL);
                    for (float f : floatValues) {
                        if (!(f == b.getNoDataValue() || f == 0 || f == Float.NaN))
                            allNoData = false;
                    }
                }
                if (allNoData) {
                    throw new Exception("band " + b.getName() + " is all no data value");
                }
            }
        }
    }

    public static Product createProduct(final String type, final int w, final int h) {
        final Product product = new Product("name", type, w, h);

        product.setStartTime(AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683"));
        product.setEndTime(AbstractMetadata.parseUTC("10-MAY-2008 20:35:46.890683"));
        product.setDescription("description");

        addGeoCoding(product);

        AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());

        return product;
    }

    public static Band createBand(final Product testProduct, final String bandName, final int w, final int h) {
        final Band band = testProduct.addBand(bandName, ProductData.TYPE_INT32);
        band.setUnit(Unit.AMPLITUDE);
        final int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band.setData(ProductData.createInstance(intValues));
        return band;
    }

    private static void addGeoCoding(final Product product) {

        final TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0.5f, 0.5f,
                                                      product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                      new float[]{10.0f, 10.0f, 5.0f, 5.0f});
        final TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0.5f, 0.5f,
                                                      product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                      new float[]{10.0f, 10.0f, 5.0f, 5.0f},
                                                      TiePointGrid.DISCONT_AT_360);
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
    }

    public static void attributeEquals(final MetadataElement elem, final String name,
                                       final double trueValue) throws Exception {
        final double val = elem.getAttributeDouble(name, 0);
        if (Double.compare(val, trueValue) != 0) {
            if (Float.compare((float) val, (float) trueValue) != 0)
                throwErr(name + " is " + val + ", expecting " + trueValue);
        }
    }

    public static void attributeEquals(final MetadataElement elem, String name,
                                       final String trueValue) throws Exception {
        final String val = elem.getAttributeString(name, "");
        if (!val.equals(trueValue))
            throwErr(name + " is " + val + ", expecting " + trueValue);
    }

    private static void compareMetadata(final Product testProduct, final Product expectedProduct,
                                        final String[] exemptionList) throws Exception {
        final MetadataElement testAbsRoot = AbstractMetadata.getAbstractedMetadata(testProduct);
        if (testAbsRoot == null)
            throwErr("Metadata is null");
        final MetadataElement expectedAbsRoot = AbstractMetadata.getAbstractedMetadata(expectedProduct);
        if (expectedAbsRoot == null)
            throwErr("Metadata is null");

        if (exemptionList != null) {
            Arrays.sort(exemptionList);
        }

        final MetadataAttribute[] attribList = expectedAbsRoot.getAttributes();
        for (MetadataAttribute expectedAttrib : attribList) {
            if (exemptionList != null && Arrays.binarySearch(exemptionList, expectedAttrib.getName()) >= 0)
                continue;

            final MetadataAttribute result = testAbsRoot.getAttribute(expectedAttrib.getName());
            if (result == null) {
                throwErr("Metadata attribute " + expectedAttrib.getName() + " is missing");
            }
            final ProductData expectedData = result.getData();
            if (!expectedData.equalElems(expectedAttrib.getData())) {
                if ((expectedData.getType() == ProductData.TYPE_FLOAT32 ||
                        expectedData.getType() == ProductData.TYPE_FLOAT64) &&
                        Double.compare(expectedData.getElemDouble(), result.getData().getElemDouble()) == 0) {

                } else if (expectedData.toString().trim().equalsIgnoreCase(result.getData().toString().trim())) {

                } else {
                    throwErr("Metadata attribute " + expectedAttrib.getName() + " expecting " + expectedAttrib.getData().toString()
                                     + " got " + result.getData().toString());
                }
            }
        }
    }

    public static void compareProducts(final Product targetProduct,
                                       final Product expectedProduct) throws Exception {
        // compare updated metadata
        compareMetadata(targetProduct, expectedProduct, null);

        if (targetProduct.getNumBands() != expectedProduct.getNumBands())
            throwErr("Different number of bands");

        if (!targetProduct.isCompatibleProduct(expectedProduct, 0))
            throwErr("Geocoding is different");

        for (TiePointGrid expectedTPG : expectedProduct.getTiePointGrids()) {
            final TiePointGrid trgTPG = targetProduct.getTiePointGrid(expectedTPG.getName());
            if (trgTPG == null)
                throwErr("TPG " + expectedTPG.getName() + " not found");

            final float[] expectedTiePoints = expectedTPG.getTiePoints();
            final float[] trgTiePoints = trgTPG.getTiePoints();

            if (!Arrays.equals(trgTiePoints, expectedTiePoints)) {
                throwErr("TPGs are different in file " + expectedProduct.getFileLocation().getAbsolutePath());
            }
        }

        for (Band expectedBand : expectedProduct.getBands()) {

            final Band trgBand = targetProduct.getBand(expectedBand.getName());
            if (trgBand == null)
                throwErr("Band " + expectedBand.getName() + " not found");

            final float[] floatValues = new float[2500];
            trgBand.readPixels(40, 40, 50, 50, floatValues, ProgressMonitor.NULL);

            final float[] expectedValues = new float[2500];
            expectedBand.readPixels(40, 40, 50, 50, expectedValues, ProgressMonitor.NULL);

            if (!Arrays.equals(floatValues, expectedValues)) {
                throwErr("Pixels are different in file " + expectedProduct.getFileLocation().getAbsolutePath());
            }
        }
    }

    public static void comparePixels(final Product targetProduct, final String bandName, final float[] expected) throws IOException {
        comparePixels(targetProduct, bandName, 0, 0, expected);
    }

    public static void comparePixels(final Product targetProduct, final String bandName,
                                     final int x, final int y, final float[] expected) throws IOException {
        final Band band = targetProduct.getBand(bandName);
        if (band == null) {
            throw new IOException(bandName + " not found");
        }

        final float[] actual = new float[expected.length];
        band.readPixels(x, y, expected.length, 1, actual, ProgressMonitor.NULL);

        for (int i = 0; i < expected.length; ++i) {
            if ((Math.abs(expected[i] - actual[i]) > 0.0001)) {
                String msg = "actual:";
                for (float anActual : actual) {
                    msg += anActual + ", ";
                }
                TestUtils.log.info(msg);
                msg = "expected:";
                for (float anExpected : expected) {
                    msg += anExpected + ", ";
                }
                TestUtils.log.info(msg);
                throw new IOException("Mismatch [" + i + "] " + actual[i] + " is not " + expected[i]);
            }
        }
    }

    public static void compareProducts(final Product targetProduct,
                                       final String expectedPath, final String[] exemptionList) throws Exception {

        final Band targetBand = targetProduct.getBandAt(0);
        if (targetBand == null)
            throwErr("targetBand at 0 is null");

        // readPixels: execute computeTiles()
        final float[] floatValues = new float[2500];
        targetBand.readPixels(40, 40, 50, 50, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        final File expectedFile = new File(expectedPath);
        if (!expectedFile.exists()) {
            throwErr("Expected file not found " + expectedFile.toString());
        }

        final ProductReader reader2 = ProductIO.getProductReaderForInput(expectedFile);

        final Product expectedProduct = reader2.readProductNodes(expectedFile, null);
        final Band expectedBand = expectedProduct.getBandAt(0);

        final float[] expectedValues = new float[2500];
        expectedBand.readPixels(40, 40, 50, 50, expectedValues, ProgressMonitor.NULL);
        if (!Arrays.equals(floatValues, expectedValues)) {
            throwErr("Pixels are different in file " + expectedPath);
        }

        // compare updated metadata
        compareMetadata(targetProduct, expectedProduct, exemptionList);
    }

    public static void compareArrays(final float[] actual, final float[] expected, final float threshold)
            throws IOException {

        if (actual.length != expected.length) {
            throw new IOException("The actual array and expected array have different lengths");
        }

        for (int i = 0; i < actual.length; ++i) {
            if ((Math.abs(expected[i] - actual[i]) > threshold)) {
                String msg = "actual:";
                for (float anActual : actual) {
                    msg += anActual + ", ";
                }
                TestUtils.log.info(msg);
                msg = "expected:";
                for (float anExpected : expected) {
                    msg += anExpected + ", ";
                }
                TestUtils.log.info(msg);
                throw new IOException("Mismatch [" + i + "] " + actual[i] + " is not " + expected[i]);
            }
        }
    }

    public static boolean containsProductType(final String[] productTypeExemptions, final String productType) {
        if (productTypeExemptions != null) {
            for (String str : productTypeExemptions) {
                if (productType.contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean skipTest(final Object obj, final String msg) throws Exception {
        SystemUtils.LOG.severe(obj.getClass().getName() + " skipped " + msg);
        if (FailOnSkip) {
            throw new Exception(obj.getClass().getName() + " skipped " + msg);
        }
        return true;
    }

    private static void throwErr(final String description) throws Exception {
        throw new Exception(description);
    }
}
