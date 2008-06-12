package org.esa.beam.framework.datamodel;

/*
 * $Id: CombinedFXYGeoCodingTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.math.FXYSum;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class CombinedFXYGeoCodingTest extends TestCase {

    private CombinedFXYGeoCoding _srcGeoCoding;

    public void setUp() throws Exception {
        final CombinedFXYGeoCoding.CodingWrapper[] codingWrappers = new CombinedFXYGeoCoding.CodingWrapper[2];
        final FXYGeoCoding geoCoding = new FXYGeoCoding(0, 0, 1, 1,
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        Datum.WGS_84);
        codingWrappers[0] = new CombinedFXYGeoCoding.CodingWrapper(geoCoding, 0, 0, 4, 5);
        codingWrappers[1] = new CombinedFXYGeoCoding.CodingWrapper(geoCoding, 4, 0, 6, 5);
        _srcGeoCoding = new CombinedFXYGeoCoding(codingWrappers);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        _srcGeoCoding = null;
    }

    public void testTransferGeoCodingWithSubSampling() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 4, 4));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setSubSampling(3, 3);


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(0, 0);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(3, 0);
        destPixelPos.setLocation(1, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(6, 0);
        destPixelPos.setLocation(2, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(9, 0);
        destPixelPos.setLocation(3, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

    }

    public void testTransferGeoCodingWithRegion() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 4, 3));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setRegion(3, 1, 4, 3);


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(3, 1);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 1);
        destPixelPos.setLocation(2, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(6, 2);
        destPixelPos.setLocation(3, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(4, 3);
        destPixelPos.setLocation(1, 2);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

    }

    public void testTransferGeoCodingWithRegionAndSubsampling() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 2, 2));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setRegion(3, 1, 4, 3);
        subsetDef.setSubSampling(2, 2);


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(3, 1);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 1);
        destPixelPos.setLocation(1, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(3, 3);
        destPixelPos.setLocation(0, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 3);
        destPixelPos.setLocation(1, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

    }

}