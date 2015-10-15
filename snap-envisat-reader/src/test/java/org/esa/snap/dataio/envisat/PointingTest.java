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

package org.esa.snap.dataio.envisat;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.AngularDirection;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Pointing;
import org.esa.snap.core.datamodel.PointingFactory;
import org.esa.snap.core.datamodel.PointingFactoryRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;


public class PointingTest extends TestCase {


    public PointingTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(PointingTest.class);
    }

    public void testPointingForMerisLikeProduct() {
        Product product = createMerisLikeProduct();
        testMerisPointingAt00(product, product.getBand("radiance_7").getPointing());
        testMerisPointingAt00(product, product.getBand("radiance_13").getPointing());
        testMerisPointingAt00(product, product.getBand("l1_flags").getPointing());
    }

    public void testPointingForAatsrLikeProduct() {
        Product product = createAatsrLikeProduct();
        testAatsrPointingAt00Fward(product, product.getBand("reflec_fward_0670").getPointing());
        testAatsrPointingAt00Nadir(product, product.getBand("reflec_nadir_0670").getPointing());
        testAatsrPointingAt00Fward(product, product.getBand("confid_flags_fward").getPointing());
        testAatsrPointingAt00Nadir(product, product.getBand("confid_flags_nadir").getPointing());
    }


    private Product createMerisLikeProduct() {
        Product product = new Product("Tatutata", EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME, 16, 16);
        final TiePointGrid latGrid = new TiePointGrid(EnvisatConstants.LAT_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{1, 2, 3, 4});
        final TiePointGrid lonGrid = new TiePointGrid(EnvisatConstants.LON_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{1, 2, 3, 4});
        final TiePointGrid saGrid = new TiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{2, 3, 4, 5});
        final TiePointGrid szGrid = new TiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{1, 2, 3, 4});
        final TiePointGrid vaGrid = new TiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{10, 20, 30, 40});
        final TiePointGrid vzGrid = new TiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{0, 1, 2, 3});
        final TiePointGrid elGrid = new TiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{20, 30, 40, 50});
        final Band band7 = new Band(EnvisatConstants.MERIS_L1B_RADIANCE_7_BAND_NAME, ProductData.TYPE_UINT16, 16, 16);
        final Band band13 = new Band(EnvisatConstants.MERIS_L1B_RADIANCE_13_BAND_NAME, ProductData.TYPE_UINT16, 16, 16);
        final Band flags = new Band(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME, ProductData.TYPE_UINT8, 16, 16);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.addTiePointGrid(szGrid);
        product.addTiePointGrid(saGrid);
        product.addTiePointGrid(vzGrid);
        product.addTiePointGrid(vaGrid);
        product.addTiePointGrid(elGrid);
        product.addBand(band7);
        product.addBand(band13);
        product.addBand(flags);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
        PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
        product.setPointingFactory(pointingFactory);
        return product;
    }

    private void testMerisPointingAt00(Product product, final Pointing pointing) {
        assertNotNull(pointing);
        assertSame(product.getSceneGeoCoding(), pointing.getGeoCoding());
        final PixelPos pixel00 = new PixelPos(0, 0);
        Assert.assertEquals(new AngularDirection(2, 1), pointing.getSunDir(pixel00, null));
        assertEquals(new AngularDirection(10, 0), pointing.getViewDir(pixel00, null));
        assertEquals(20.0f, pointing.getElevation(pixel00), 1.0e-10f);
    }

    private Product createAatsrLikeProduct() {
        Product product = new Product("FischersFritz", EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME, 16, 16);
        final TiePointGrid latGrid = new TiePointGrid(EnvisatConstants.LAT_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{1, 2, 3, 4});
        final TiePointGrid lonGrid = new TiePointGrid(EnvisatConstants.LON_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{1, 2, 3, 4});
        final TiePointGrid saGridF = new TiePointGrid(EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{22, 32, 42, 52});
        final TiePointGrid seGridF = new TiePointGrid(EnvisatConstants.AATSR_SUN_ELEV_FWARD_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{15, 25, 35, 45});
        final TiePointGrid vaGridF = new TiePointGrid(EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{72, 73, 74, 75});
        final TiePointGrid veGridF = new TiePointGrid(EnvisatConstants.AATSR_VIEW_ELEV_FWARD_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{61, 62, 63, 64});
        final TiePointGrid saGridN = new TiePointGrid(EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{10, 20, 30, 40});
        final TiePointGrid seGridN = new TiePointGrid(EnvisatConstants.AATSR_SUN_ELEV_NADIR_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                      new float[]{0, 1, 2, 3});
        final TiePointGrid vaGridN = new TiePointGrid(EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{-2, -3, -4, -5});
        final TiePointGrid veGridN = new TiePointGrid(EnvisatConstants.AATSR_VIEW_ELEV_NADIR_DS_NAME, 2, 2, 0, 0, 16,
                                                      16,
                                                      new float[]{17, 27, 37, 47});
        final TiePointGrid elGrid = new TiePointGrid(EnvisatConstants.AATSR_ALTITUDE_DS_NAME, 2, 2, 0, 0, 16, 16,
                                                     new float[]{207, 307, 407, 507});
        final Band band670F = new Band(EnvisatConstants.AATSR_L1B_REFLEC_FWARD_0670_BAND_NAME, ProductData.TYPE_UINT16,
                                       16, 16);
        final Band band670N = new Band(EnvisatConstants.AATSR_L1B_REFLEC_NADIR_0670_BAND_NAME, ProductData.TYPE_UINT16,
                                       16, 16);
        final Band flagsF = new Band(EnvisatConstants.AATSR_L1B_CONFID_FLAGS_FWARD_BAND_NAME, ProductData.TYPE_UINT8,
                                     16, 16);
        final Band flagsN = new Band(EnvisatConstants.AATSR_L1B_CONFID_FLAGS_NADIR_BAND_NAME, ProductData.TYPE_UINT8,
                                     16, 16);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.addTiePointGrid(saGridF);
        product.addTiePointGrid(seGridF);
        product.addTiePointGrid(vaGridF);
        product.addTiePointGrid(veGridF);
        product.addTiePointGrid(saGridN);
        product.addTiePointGrid(seGridN);
        product.addTiePointGrid(vaGridN);
        product.addTiePointGrid(veGridN);
        product.addTiePointGrid(elGrid);
        product.addBand(band670F);
        product.addBand(band670N);
        product.addBand(flagsF);
        product.addBand(flagsN);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
        PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
        product.setPointingFactory(pointingFactory);
        return product;
    }

    private void testAatsrPointingAt00Fward(Product product, final Pointing pointing) {
        assertNotNull(pointing);
        assertSame(product.getSceneGeoCoding(), pointing.getGeoCoding());
        final PixelPos pixel00 = new PixelPos(0, 0);
        assertEquals(new AngularDirection(22, 75), pointing.getSunDir(pixel00, null));
        assertEquals(new AngularDirection(72, 29), pointing.getViewDir(pixel00, null));
        assertEquals(207.0f, pointing.getElevation(pixel00), 1.0e-10f);
    }

    private void testAatsrPointingAt00Nadir(Product product, final Pointing pointing) {
        assertNotNull(pointing);
        assertSame(product.getSceneGeoCoding(), pointing.getGeoCoding());
        final PixelPos pixel00 = new PixelPos(0, 0);
        assertEquals(new AngularDirection(10, 90), pointing.getSunDir(pixel00, null));
        assertEquals(new AngularDirection(-2, 73), pointing.getViewDir(pixel00, null));
        assertEquals(207.0f, pointing.getElevation(pixel00), 1.0e-10f);
    }
}
