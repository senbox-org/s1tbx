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
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.util.TestUtils;

/**
 * Unit test for MapProjectionOp
 */
public class TestMapProjectionOp extends TestCase {

    private OperatorSpi spi;

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX", "GeoTIFF" };
    private String[] exceptionExemptions = { "not supported", "already map projected" };

    @Override
    protected void setUp() throws Exception {
        spi = new MapReProjectionOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllTerraSARX() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllCosmo() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllNestBox() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, exceptionExemptions);
    }
}