/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.common.reproject;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public abstract class AbstractReprojectionOpTest {

    static final String AUTO_UTM = "AUTO:42001";
    static final String WGS84_CODE = "EPSG:4326";
    static final String UTM33N_CODE = "EPSG:32633";
    @SuppressWarnings({"StringConcatenation"})
    static final String UTM33N_WKT = "PROJCS[\"WGS 84 / UTM zone 33N\"," +
            "GEOGCS[\"WGS 84\"," +
            "  DATUM[\"World Geodetic System 1984\"," +
            "    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]]," +
            "    AUTHORITY[\"EPSG\",\"6326\"]]," +
            "  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]]," +
            "  UNIT[\"degree\", 0.017453292519943295]," +
            "  AXIS[\"Geodetic longitude\", EAST]," +
            "  AXIS[\"Geodetic latitude\", NORTH]," +
            "  AUTHORITY[\"EPSG\",\"4326\"]]," +
            "PROJECTION[\"Transverse Mercator\", AUTHORITY[\"EPSG\",\"9807\"]]," +
            "PARAMETER[\"central_meridian\", 15.0]," +
            "PARAMETER[\"latitude_of_origin\", 0.0]," +
            "PARAMETER[\"scale_factor\", 0.9996]," +
            "PARAMETER[\"false_easting\", 500000.0]," +
            "PARAMETER[\"false_northing\", 0.0]," +
            "UNIT[\"m\", 1.0]," +
            "AXIS[\"Easting\", EAST]," +
            "AXIS[\"Northing\", NORTH]," +
            "AUTHORITY[\"EPSG\",\"32633\"]]";
    protected static File wktFile;

    private static final float[] LATS = new float[]{
            50.0f, 50.0f,
            30.0f, 30.0f
    };
    private static final float[] LONS = new float[]{
            6.0f, 26.0f,
            6.0f, 26.0f
    };
    protected static final String FLOAT_BAND_NAME = "floatData";
    protected static final String INT_BAND_NAME = "intData";

    protected Product sourceProduct;
    protected Map<String, Object> parameterMap;
    protected static final double EPS = 1.0e-6;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        wktFile = new File(AbstractReprojectionOpTest.class.getResource("test.wkt").toURI());
    }

    private void createSourceProduct() throws Exception {
        sourceProduct = new Product("source", "t", 50, 50);
        final TiePointGrid latGrid = new TiePointGrid("latGrid", 2, 2, 0.5f, 0.5f, 49, 49, LATS);
        final TiePointGrid lonGrid = new TiePointGrid("lonGrid", 2, 2, 0.5f, 0.5f, 49, 49, LONS);
        sourceProduct.addTiePointGrid(latGrid);
        sourceProduct.addTiePointGrid(lonGrid);
        sourceProduct.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        sourceProduct.setStartTime(ProductData.UTC.parse("02-Jan-2008 10:15:10"));
        sourceProduct.setEndTime(ProductData.UTC.parse("02-Jan-2008 10:45:50"));
        Band floatDataBand = sourceProduct.addBand(FLOAT_BAND_NAME, ProductData.TYPE_FLOAT32);
        floatDataBand.setRasterData(createDataFor(floatDataBand));
        floatDataBand.setSynthetic(true);
        Band intDataBand = sourceProduct.addBand(INT_BAND_NAME, ProductData.TYPE_INT16);
        intDataBand.setRasterData(createDataFor(intDataBand));
        intDataBand.setSynthetic(true);
    }

    @Before
    public void setupTestMethod() throws Exception {
        parameterMap = new HashMap<>(5);
        createSourceProduct();
    }


    protected Product createReprojectedProduct(Map<String, Product> sourceMap) {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceMap);
    }

    protected Product createReprojectedProduct() {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceProduct);
    }

    protected void assertPixelValidState(Band targetBand, double sourceX, double sourceY,
                                         boolean expectedValid) throws IOException {
        final PixelPos targetPP = computeTargetPP(targetBand, sourceX, sourceY);
        assertTargetPixelValidState(targetBand, targetPP, expectedValid);
    }

    protected void assertTargetPixelValidState(Band targetBand, PixelPos targetPP, boolean expectedValid) {
        boolean pixelValid = targetBand.isPixelValid((int) Math.floor(targetPP.x), (int) Math.floor(targetPP.y));
        assertEquals(expectedValid, pixelValid);
    }

    protected void assertPixelValue(Band targetBand, double sourceX, double sourceY,
                                    double expectedPixelValue, double delta) throws IOException {
        final PixelPos targetPP = computeTargetPP(targetBand, sourceX, sourceY);
        assertTargetPixelValue(targetBand, targetPP, expectedPixelValue, delta);
    }

    protected void assertTargetPixelValue(Band targetBand, PixelPos targetPP, double expectedPixelValue, double delta) throws IOException {
        final double[] pixels = new double[1];
        targetBand.readPixels((int) Math.floor(targetPP.x), (int) Math.floor(targetPP.y), 1, 1, pixels);
        assertEquals(expectedPixelValue, pixels[0], delta);
    }

    protected PixelPos computeTargetPP(Band targetBand, double sourceX, double sourceY) {
        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final PixelPos sourcePP = new PixelPos(sourceX, sourceY);
        final GeoPos geoPos = sourceBand.getGeoCoding().getGeoPos(sourcePP, null);
        return targetBand.getGeoCoding().getPixelPos(geoPos, null);
    }

    private static ProductData createDataFor(Band dataBand) {
        final int width = dataBand.getRasterWidth();
        final int height = dataBand.getRasterHeight();
        final ProductData data = ProductData.createInstance(dataBand.getDataType(), width * height);
        for (int y = 0; y < height; y++) {
            final int line = y * width;
            for (int x = 0; x < width; x++) {
                data.setElemIntAt(line + x, x * y);
            }
        }
        return data;
    }
}
