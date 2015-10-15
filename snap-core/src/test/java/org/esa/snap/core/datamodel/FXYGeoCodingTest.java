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

package org.esa.snap.core.datamodel;


import junit.framework.TestCase;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.math.FXYSum;

import java.util.Arrays;

public class FXYGeoCodingTest extends TestCase {

    private FXYGeoCoding _geoCoding;

    @Override
    public void setUp() {
        final FXYSum.Linear xFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear yFunc = new FXYSum.Linear(new double[]{0, 1, 0});
        final FXYSum.Linear latFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear lonFunc = new FXYSum.Linear(new double[]{0, 1, 0});

        _geoCoding = new FXYGeoCoding(0, 0, 1, 1,
                                      xFunc, yFunc,
                                      latFunc, lonFunc,
                                      Datum.WGS_84);

    }

    public void testThatReverseIsInvOfForward() {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos(12.5f, 349.1f);
        _geoCoding.getGeoPos(pixelPos, geoPos);

        final PixelPos pixelPosRev = new PixelPos();
        _geoCoding.getPixelPos(geoPos, pixelPosRev);

        assertEquals(pixelPos.x, pixelPosRev.x, 1e-4);
        assertEquals(pixelPos.y, pixelPosRev.y, 1e-4);

    }

    public void testFXYSumsRevAndForwAreEqual() {
        // values are taken from a AVNIR-2 product
        final FXYSum.Cubic funcLat = new FXYSum.Cubic(new double[]{
            38.500063158199914,
            -1.5864380827666764E-5,
            -8.87076135137345E-5,
            -5.4715650494309404E-11,
            1.7664837083366042E-11,
            -2.117514813932369E-12,
            3.064408168899423E-17,
            1.585962164097323E-16,
            -6.098694274571135E-17,
            5.84863124300545E-18
        });
        final FXYSum.Cubic funcLon = new FXYSum.Cubic(new double[]{
            140.13684646307198,
            1.128828545988631E-4,
            -2.018789097584942E-5,
            -2.3783153647830587E-11,
            -1.3475060752719392E-10,
            2.3779980524035797E-11,
            -8.964042454477968E-17,
            1.603005243179082E-16,
            2.689055849423117E-16,
            -5.342672795186944E-17
        });
        final FXYSum.Cubic funcX = new FXYSum.Cubic(new double[]{
            -1004274.164152284,
            -8438.725369567894,
            1931.015292199648,
            480.2628768865113,
            -51.80353463858226,
            79.50343943972683,
            -1.170751563256245,
            -2.461732799965884,
            0.43455217130283397,
            -0.24909748244716065
        });
        final FXYSum.Cubic funcY = new FXYSum.Cubic(new double[]{
            129893.51986530663,
            -10247.4132114523,
            3492.9557841930427,
            -35.089200336275994,
            -12.449311003384066,
            8.925552458488387,
            -0.13026608164787684,
            0.34836844379127946,
            0.03006361443561656,
            -0.13394457012591415
        });

        final double x = 10.5;
        final double y = 15.5;
        final double lat = funcLat.computeZ(x, y);
        final double lon = funcLon.computeZ(x, y);

        final double x2 = funcX.computeZ(lat, lon);
        final double y2 = funcY.computeZ(lat, lon);

        assertEquals(x, x2, 1e-1);
        assertEquals(y, y2, 1e-2);
    }

    public void testTransferGeoCodingWithSubset() {
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10,20);
        srcNode.setGeoCoding(_geoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final ProductSubsetDef subset = new ProductSubsetDef("subset");
        subset.setRegion(10,10,50,50);
        subset.setSubSampling(2, 3);
        final Band destNode = new Band("destDummy",ProductData.TYPE_INT8, 10,20);
        final Scene destScene = SceneFactory.createScene(destNode);

        srcScene.transferGeoCodingTo(destScene, subset);

        assertFXYGeoCodingIsCopied((FXYGeoCoding)destNode.getGeoCoding(), subset);

    }


    public void testTransferGeoCodingWithoutSubset() {
        final Band srcNode = new Band("srcDummy",ProductData.TYPE_INT8, 10,20);
        srcNode.setGeoCoding(_geoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final Band destNode = new Band("destDummy",ProductData.TYPE_INT8, 10,20);
        final Scene destScene = SceneFactory.createScene(destNode);

        srcScene.transferGeoCodingTo(destScene, null);

        assertFXYGeoCodingIsCopied((FXYGeoCoding)destNode.getGeoCoding(), null);
    }

    private void assertFXYGeoCodingIsCopied(final FXYGeoCoding subsetGeoCoding, ProductSubsetDef subset) {
        assertTrue(_geoCoding != subsetGeoCoding);

        if(subset == null) {
            subset = new ProductSubsetDef("s");
            subset.setRegion(0,0,100, 100);
            subset.setSubSampling(1,1);
        }
        assertEquals(_geoCoding.getPixelOffsetX() + subset.getRegion().getX(), subsetGeoCoding.getPixelOffsetX(), 1.e-6);
        assertEquals(_geoCoding.getPixelOffsetY() + subset.getRegion().getY(), subsetGeoCoding.getPixelOffsetY(), 1.e-6);
        assertEquals(_geoCoding.getPixelSizeX() * subset.getSubSamplingX(), subsetGeoCoding.getPixelSizeX(), 1.e-6);
        assertEquals(_geoCoding.getPixelSizeY() * subset.getSubSamplingY(), subsetGeoCoding.getPixelSizeY(), 1.e-6);

        assertTrue(_geoCoding.getPixelXFunction() != subsetGeoCoding.getPixelXFunction());
        assertTrue(_geoCoding.getPixelYFunction() != subsetGeoCoding.getPixelYFunction());
        assertTrue(_geoCoding.getLatFunction() != subsetGeoCoding.getLatFunction());
        assertTrue(_geoCoding.getLonFunction() != subsetGeoCoding.getLonFunction());

        assertTrue(_geoCoding.getPixelXFunction().getCoefficients() !=
                   subsetGeoCoding.getPixelXFunction().getCoefficients());
        assertTrue(_geoCoding.getPixelYFunction().getCoefficients() !=
                   subsetGeoCoding.getPixelYFunction().getCoefficients());
        assertTrue(_geoCoding.getLatFunction().getCoefficients() !=
                   subsetGeoCoding.getLatFunction().getCoefficients());
        assertTrue(_geoCoding.getLonFunction().getCoefficients() !=
                   subsetGeoCoding.getLonFunction().getCoefficients());

        assertTrue(Arrays.equals(_geoCoding.getPixelXFunction().getCoefficients(),
                                 subsetGeoCoding.getPixelXFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getPixelYFunction().getCoefficients(),
                                 subsetGeoCoding.getPixelYFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getLatFunction().getCoefficients(),
                                 subsetGeoCoding.getLatFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getLonFunction().getCoefficients(),
                                 subsetGeoCoding.getLonFunction().getCoefficients()));
    }

}
