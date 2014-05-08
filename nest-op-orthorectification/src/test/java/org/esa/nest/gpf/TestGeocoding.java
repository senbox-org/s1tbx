/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;

import java.util.Arrays;

/**
 * Unit test for MultilookOperator.
 */
public class TestGeocoding extends TestCase {

    private OperatorSpi spi;

    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX" };
    private String[] exceptionExemptions = { "not supported" };

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new MultilookOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessing() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathWSM);
        
        GeoCoding gc = sourceProduct.getGeoCoding();
        GeoPos geo = new GeoPos();
        PixelPos pix1 = new PixelPos();
        PixelPos pix2 = new PixelPos();
        double errorX = 0, errorY=0;

        int n=0;
        for(int i=0; i < 1000; i+=10) {
            pix1.setLocation(i+0.5, i+0.5);

            gc.getGeoPos(pix1, geo);
            gc.getPixelPos(geo, pix2);

            errorX += Math.abs(pix1.getX()-pix2.getX());
            errorY += Math.abs(pix1.getY()-pix2.getY());

            TestUtils.log.info(pix1.getX()+" == "+pix2.getX()+", "+pix1.getY()+" == "+pix2.getY());
            ++n;
        }
        System.out.println("\nerrorX="+errorX);
        System.out.println("errorY="+errorY);
    }


}
