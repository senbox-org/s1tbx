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
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;

import java.util.Arrays;

public class PixelGeoCoding_TransferGeoCodingToTest extends TestCase {

    private Product sourceP;
    private String bandNameLat = "latb";
    private String bandNameLon = "lonb";

    protected void setUp() throws Exception {
        sourceP = new Product("test", "test", 6, 7);
        final Band latBand = sourceP.addBand(bandNameLat, ProductData.TYPE_FLOAT32);
        fillWithData(latBand, 0.03f, 30f);
        final Band lonBand = sourceP.addBand(bandNameLon, ProductData.TYPE_FLOAT32);
        fillWithData(lonBand, 0.047f, 50f);
        sourceP.setGeoCoding(new PixelGeoCoding(latBand, lonBand, "lsmf", 5));
    }

    protected void tearDown() throws Exception {
    }

    public void testDestLatLonBandsExisting() {
        final ProductSubsetDef subsetDef = null;
        final Product destP = new Product("dest", "dest",
                                          sourceP.getSceneRasterWidth(),
                                          sourceP.getSceneRasterHeight());
        copyBandTo(destP, ((PixelGeoCoding) sourceP.getGeoCoding()).getLatBand());
        copyBandTo(destP, ((PixelGeoCoding) sourceP.getGeoCoding()).getLonBand());

        assertEquals(true, sourceP.transferGeoCodingTo(destP, subsetDef));
        assertNotNull(destP.getGeoCoding());
        assertEquals(true, destP.getGeoCoding() instanceof PixelGeoCoding);
    }

    public void testDestWithoutLatLonBands() {
        final ProductSubsetDef subsetDef = null;
        final Product destP = new Product("dest", "dest",
                                          sourceP.getSceneRasterWidth(),
                                          sourceP.getSceneRasterHeight());

        assertEquals(true, sourceP.transferGeoCodingTo(destP, subsetDef));
        final GeoCoding destGeoCoding = destP.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertEquals(true, destGeoCoding instanceof PixelGeoCoding);
    }

    private void copyBandTo(Product destP, Band sourceBand) {
        final Band destBand = new Band(sourceBand.getName(), sourceBand.getDataType(),
                                       sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        destBand.setRasterData(sourceBand.getData().createDeepClone());
        destP.addBand(destBand);
    }

    private void fillWithData(Band band, float multiplicator, float offset) {
        band.ensureRasterData();
        final ProductData data = band.getRasterData();
        for (int i = 0; i < data.getNumElems(); i++) {
            data.setElemFloatAt(i, i * multiplicator + offset);
        }
    }
}
