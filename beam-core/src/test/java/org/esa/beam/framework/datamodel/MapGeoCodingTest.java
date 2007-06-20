/*
 * $Id: MapGeoCodingTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;

public class MapGeoCodingTest extends TestCase {

    private MapGeoCoding _mapGeoCoding;

    public void setUp() {
        final IdentityTransformDescriptor td = new IdentityTransformDescriptor();
        final MapTransform transform = td.createTransform(null);
        final MapProjection projection = new MapProjection("wullidutsch", transform);
        final MapInfo mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, Datum.WGS_84);
        _mapGeoCoding = new MapGeoCoding(mapInfo);
    }

    public void testFail() {

        final GeoPos geoPos = new GeoPos();

        _mapGeoCoding.getGeoPos(new PixelPos(-180, 10), geoPos);
        assertEquals(-10, geoPos.getLat(), 1e-5);
        assertEquals(-180, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(-180.00001f, 15), geoPos);
        assertEquals(-15, geoPos.getLat(), 1e-5);
        assertEquals(179.99999, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(180, 20), geoPos);
        assertEquals(-20, geoPos.getLat(), 1e-5);
        assertEquals(180, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(180.00001f, 25), geoPos);
        assertEquals(-25, geoPos.getLat(), 1e-5);
        assertEquals(-179.99999, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(-360 - 180, 10), geoPos);
        assertEquals(-10, geoPos.getLat(), 1e-5);
        assertEquals(-180, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(-360 - 180.0001f, 15), geoPos);
        assertEquals(-15, geoPos.getLat(), 1e-5);
        assertEquals(179.9999, geoPos.getLon(), 1e-4);

        _mapGeoCoding.getGeoPos(new PixelPos(360 + 180, 20), geoPos);
        assertEquals(-20, geoPos.getLat(), 1e-5);
        assertEquals(180, geoPos.getLon(), 1e-5);

        _mapGeoCoding.getGeoPos(new PixelPos(360 + 180.0001f, 25), geoPos);
        assertEquals(-25, geoPos.getLat(), 1e-5);
        assertEquals(-179.9999, geoPos.getLon(), 1e-4);
    }

    public void testTransferGeoCodingWithoutSubset() {
        final Band destNode = new Band("destDummy",ProductData.TYPE_INT8, 10,20);
        final Band srcNode = new Band("srcDummy",ProductData.TYPE_INT8, 10,20);
        srcNode.setGeoCoding(_mapGeoCoding);
        final Scene destScene = SceneFactory.createScene(destNode);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        srcScene.transferGeoCodingTo(destScene, null);

        assertNotSame(_mapGeoCoding, destScene.getGeoCoding());
        final MapInfo origMapInfo = _mapGeoCoding.getMapInfo();
        final MapInfo copyMapInfo = ((MapGeoCoding) destScene.getGeoCoding()).getMapInfo();


        assertNotSame(origMapInfo, copyMapInfo);
        assertEquals(origMapInfo.getEasting(), copyMapInfo.getEasting(), 1e-6);
        assertEquals(origMapInfo.getNorthing(), copyMapInfo.getNorthing(), 1e-6);
        assertSame(origMapInfo.getDatum(), copyMapInfo.getDatum());
        assertEquals(origMapInfo.getElevationModelName(), copyMapInfo.getElevationModelName());
        assertEquals(origMapInfo.getMapProjection().getName(), copyMapInfo.getMapProjection().getName());
        assertEquals(origMapInfo.getOrientation(), copyMapInfo.getOrientation(), 1e-6);
        assertEquals(origMapInfo.getPixelSizeX(), copyMapInfo.getPixelSizeX(), 1e-6);
        assertEquals(origMapInfo.getPixelSizeY(), copyMapInfo.getPixelSizeY(), 1e-6);
        assertEquals(origMapInfo.getPixelX(), copyMapInfo.getPixelY(), 1e-6);
        assertEquals(destScene.getRasterHeight(), copyMapInfo.getSceneHeight(), 1e-6);
        assertEquals(destScene.getRasterWidth(), copyMapInfo.getSceneWidth(), 1e-6);

    }

    public void testTransferGeoCodingWithSubset() {
        final Band srcNode = new Band("srcDummy",ProductData.TYPE_INT8, 10,20);
        srcNode.setGeoCoding(_mapGeoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final Band destNode = new Band("destDummy",ProductData.TYPE_INT8, 10,20);
        final Scene destScene = SceneFactory.createScene(destNode);

        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setSubSampling(2,3);
        subsetDef.setRegion(10,10,50,50);
        srcScene.transferGeoCodingTo(destScene, subsetDef);

        assertNotSame(_mapGeoCoding, destNode.getGeoCoding());
        final MapInfo origMapInfo = _mapGeoCoding.getMapInfo();
        final MapInfo copyMapInfo = ((MapGeoCoding)destNode.getGeoCoding()).getMapInfo();

        assertNotSame(origMapInfo, copyMapInfo);
        assertEquals(origMapInfo.getEasting() + subsetDef.getRegion().getX(),
                     copyMapInfo.getEasting(), 1e-6);
        assertEquals(origMapInfo.getNorthing() - subsetDef.getRegion().getY(),
                     copyMapInfo.getNorthing(), 1e-6);
        assertSame(origMapInfo.getDatum(), copyMapInfo.getDatum());
        assertEquals(origMapInfo.getElevationModelName(), copyMapInfo.getElevationModelName());
        assertEquals(origMapInfo.getMapProjection().getName(), copyMapInfo.getMapProjection().getName());
        assertEquals(origMapInfo.getOrientation(), copyMapInfo.getOrientation(), 1e-6);
        assertEquals(origMapInfo.getPixelSizeX() * subsetDef.getSubSamplingX(),
                     copyMapInfo.getPixelSizeX(), 1e-6);
        assertEquals(origMapInfo.getPixelSizeY() * subsetDef.getSubSamplingY(),
                     copyMapInfo.getPixelSizeY(), 1e-6);
        assertEquals(origMapInfo.getPixelX(),
                     copyMapInfo.getPixelX(), 1e-6);
        assertEquals(origMapInfo.getPixelY(),
                     copyMapInfo.getPixelY(), 1e-6);
        assertEquals(destScene.getRasterHeight(), copyMapInfo.getSceneHeight(), 1e-6);
        assertEquals(destScene.getRasterWidth(), copyMapInfo.getSceneWidth(), 1e-6);


    }
}
