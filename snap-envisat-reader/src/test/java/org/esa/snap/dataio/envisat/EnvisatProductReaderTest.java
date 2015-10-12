/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Scanner;

import static org.junit.Assert.*;

public class EnvisatProductReaderTest {

    private EnvisatProductReaderPlugIn readerPlugIn;

    @Before
    public void setUp() {
        readerPlugIn = new EnvisatProductReaderPlugIn();
    }

    @Ignore
    @Test
    public void testAatsrGeoLocation_UpperRightCorner() throws IOException, URISyntaxException {
        final EnvisatProductReader reader = (EnvisatProductReader) readerPlugIn.createReaderInstance();

        try {
            final Product product = reader.readProductNodes(
                    new File(getClass().getResource(
                            "ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.N1").toURI()), null);
            assertEquals(512, product.getSceneRasterWidth());
            assertEquals(320, product.getSceneRasterHeight());

            final TiePointGrid latGrid = product.getTiePointGrid("latitude");
            final TiePointGrid lonGrid = product.getTiePointGrid("longitude");
            assertNotNull(latGrid);
            assertNotNull(lonGrid);

            final ProductFile productFile = reader.getProductFile();
            assertTrue(productFile.storesPixelsInChronologicalOrder());

            final int colCount = 512;
            final int rowCount = 320;
            final float[] lats = new float[colCount * rowCount];
            final float[] lons = new float[colCount * rowCount];
            readFloats("image_latgrid_ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.txt", lats);
            readFloats("image_longrid_ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.txt", lons);

            final GeoPos geoPos = new GeoPos();

            product.getSceneGeoCoding().getGeoPos(new PixelPos(0.0F + 1.0F, 0.0F), geoPos);
            assertEquals(44.550718F, geoPos.getLat(), 1.0E-5F);
            assertEquals(32.878792F, geoPos.getLon(), 1.0E-5F);

            product.getSceneGeoCoding().getGeoPos(new PixelPos(5.0F + 1.0F, 0.0F), geoPos);
            assertEquals(44.541008F, geoPos.getLat(), 1.0E-5F);
            assertEquals(32.940249F, geoPos.getLon(), 1.0E-5F);

            for (int i = 0; i < rowCount; i++) {
                for (int j = 0, k = colCount - 1; j < colCount; j++, k--) {
                    product.getSceneGeoCoding().getGeoPos(new PixelPos(j + 1.0F, i + 0.0F), geoPos);
                    assertEquals(lats[i * colCount + k], geoPos.getLat(), 1.2E-5F);
                    assertEquals(lons[i * colCount + k], geoPos.getLon(), 1.2E-5F);
                }
            }
        } finally {
            reader.close();
        }
    }

    @Ignore
    @Test
    public void testAatsrGeoLocation_Center() throws IOException, URISyntaxException {
        final EnvisatProductReader reader = (EnvisatProductReader) readerPlugIn.createReaderInstance();

        try {
            final Product product = reader.readProductNodes(
                    new File(getClass().getResource(
                            "ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.N1").toURI()), null);
            assertEquals(512, product.getSceneRasterWidth());
            assertEquals(320, product.getSceneRasterHeight());

            final TiePointGrid latGrid = product.getTiePointGrid("latitude");
            final TiePointGrid lonGrid = product.getTiePointGrid("longitude");
            assertNotNull(latGrid);
            assertNotNull(lonGrid);

            final ProductFile productFile = reader.getProductFile();
            assertTrue(productFile.storesPixelsInChronologicalOrder());

            final int colCount = 512;
            final int rowCount = 320;
            final float[] lats = new float[colCount * rowCount];
            final float[] lons = new float[colCount * rowCount];
            readFloats("image_latgrid_centre_ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.txt", lats);
            readFloats("image_longrid_centre_ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.txt", lons);

            final GeoPos geoPos = new GeoPos();

            for (int i = 0; i < rowCount; i++) {
                for (int j = 0, k = colCount - 1; j < colCount; j++, k--) {
                    product.getSceneGeoCoding().getGeoPos(new PixelPos(j + 0.5F, i + 0.5F), geoPos);
                    assertEquals(lats[i * colCount + k], geoPos.getLat(), 1.2E-5F);
                    assertEquals(lons[i * colCount + k], geoPos.getLon(), 1.2E-5F);
                }
            }
        } finally {
            reader.close();
        }
    }

    private void readFloats(String resourceName, float[] floats) {
        final Scanner scanner = new Scanner(getClass().getResourceAsStream(resourceName), "US-ASCII");
        scanner.useLocale(Locale.ENGLISH);

        try {
            for (int i = 0; i < floats.length; i++) {
                floats[i] = scanner.nextFloat();
            }
        } finally {
            scanner.close();
        }
    }
}
