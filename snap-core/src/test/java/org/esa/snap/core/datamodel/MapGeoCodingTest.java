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
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;

import java.awt.Point;
import java.awt.geom.AffineTransform;

public class MapGeoCodingTest extends TestCase {

    public void testFail() {
        MapGeoCoding mapGeoCoding = createIdentityMapGeoCoding();

        final GeoPos geoPos = new GeoPos();

        mapGeoCoding.getGeoPos(new PixelPos(-180, 10), geoPos);
        assertEquals(-10, geoPos.getLat(), 1e-5);
        assertEquals(-180, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(-180.00001f, 15), geoPos);
        assertEquals(-15, geoPos.getLat(), 1e-5);
        assertEquals(179.99999, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(180, 20), geoPos);
        assertEquals(-20, geoPos.getLat(), 1e-5);
        assertEquals(180, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(180.00001f, 25), geoPos);
        assertEquals(-25, geoPos.getLat(), 1e-5);
        assertEquals(-179.99999, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(-360 - 180, 10), geoPos);
        assertEquals(-10, geoPos.getLat(), 1e-5);
        assertEquals(-180, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(-360 - 180.0001f, 15), geoPos);
        assertEquals(-15, geoPos.getLat(), 1e-5);
        assertEquals(179.9999, geoPos.getLon(), 1e-4);

        mapGeoCoding.getGeoPos(new PixelPos(360 + 180, 20), geoPos);
        assertEquals(-20, geoPos.getLat(), 1e-5);
        assertEquals(180, geoPos.getLon(), 1e-5);

        mapGeoCoding.getGeoPos(new PixelPos(360 + 180.0001f, 25), geoPos);
        assertEquals(-25, geoPos.getLat(), 1e-5);
        assertEquals(-179.9999, geoPos.getLon(), 1e-4);
    }

    public void testTransferGeoCodingWithoutSubset() {
        MapGeoCoding mapGeoCoding = createIdentityMapGeoCoding();

        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 10, 20);
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(mapGeoCoding);
        final Scene destScene = SceneFactory.createScene(destNode);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        srcScene.transferGeoCodingTo(destScene, null);

        assertNotSame(mapGeoCoding, destScene.getGeoCoding());
        final MapInfo origMapInfo = mapGeoCoding.getMapInfo();
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
        MapGeoCoding mapGeoCoding = createIdentityMapGeoCoding();

        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(mapGeoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 10, 20);
        final Scene destScene = SceneFactory.createScene(destNode);

        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setSubSampling(2, 3);
        subsetDef.setRegion(10, 10, 50, 50);
        srcScene.transferGeoCodingTo(destScene, subsetDef);

        assertNotSame(mapGeoCoding, destNode.getGeoCoding());
        final MapInfo origMapInfo = mapGeoCoding.getMapInfo();
        final MapInfo copyMapInfo = ((MapGeoCoding) destNode.getGeoCoding()).getMapInfo();

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

    public void testAT() {
        AffineTransform transform = new AffineTransform();

        transform.translate(1, 2);
        transform.scale(3, 4);

        assertEquals(new Point(1 + 3 * 10, 2 + 4 * 11), transform.transform(new Point(10, 11), null));
    }

    public void testThatAffineTransformCanBeUsedInstead() {
        MapGeoCoding mapGeoCoding = createNonRotatedMapGeoCoding();

        float x, y, lat, lon, cosa, sina;

        x = 0.0f;
        y = 0.0f;
        lon = +5.6f + (x - -1.2f) * +0.9f;
        lat = -7.8f - (y - +3.4f) * -1.0f;
        testPixelToMapTransform(mapGeoCoding, x, y, lat, lon);

        x = 1.0f;
        y = -2.0f;
        lon = +5.6f + (x - -1.2f) * +0.9f;
        lat = -7.8f - (y - +3.4f) * -1.0f;
        testPixelToMapTransform(mapGeoCoding, x, y, lat, lon);

        cosa = (float) Math.cos(Math.toRadians(42));
        sina = (float) Math.sin(Math.toRadians(42));

        mapGeoCoding = createRotatedMapGeoCoding();
        x = 0.0f;
        y = 0.0f;
        lon = +5.6f + ((x - -1.2f) * cosa + (y - +3.4f) * sina) * +0.9f;
        lat = -7.8f - ((x - -1.2f) * -sina + (y - +3.4f) * cosa) * -1.0f;
        testPixelToMapTransform(mapGeoCoding, x, y, lat, lon);

        x = 1.0f;
        y = -2.0f;
        lon = +5.6f + ((x - -1.2f) * cosa + (y - +3.4f) * sina) * +0.9f;
        lat = -7.8f - ((x - -1.2f) * -sina + (y - +3.4f) * cosa) * -1.0f;
        testPixelToMapTransform(mapGeoCoding, x, y, lat, lon);
    }

    private static void testPixelToMapTransform(MapGeoCoding mapGeoCoding, float x, float y, float lat, float lon) {
        GeoPos expectedGP = new GeoPos(lat, lon);
        PixelPos expectedPP = new PixelPos(x, y);

        GeoPos someGP = mapGeoCoding.getGeoPos(expectedPP, null);
        assertEquals(expectedGP.lon, someGP.lon, 1e-4f);
        assertEquals(expectedGP.lat, someGP.lat, 1e-4f);
        PixelPos somePP = mapGeoCoding.getPixelPos(expectedGP, null);
        assertEquals(expectedPP.x, somePP.x, 1e-4f);
        assertEquals(expectedPP.y, somePP.y, 1e-4f);
    }

    private MapGeoCoding createIdentityMapGeoCoding() {
        final IdentityTransformDescriptor td = new IdentityTransformDescriptor();
        final MapTransform transform = td.createTransform(null);
        final MapProjection projection = new MapProjection("wullidutsch", transform);
        final MapInfo mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, Datum.WGS_84);
        return new MapGeoCoding(mapInfo);
    }

    private MapGeoCoding createNonRotatedMapGeoCoding() {
        return new MapGeoCoding(createMapInfo(Datum.WGS_84));
    }

    private MapGeoCoding createRotatedMapGeoCoding() {
        final MapInfo mapInfo = createMapInfo(Datum.WGS_84);
        mapInfo.setOrientation(42.0f);
        return new MapGeoCoding(mapInfo);
    }

    private MapInfo createMapInfo(Datum datum) {
        final IdentityTransformDescriptor td = new IdentityTransformDescriptor();
        final MapTransform transform = td.createTransform(null);
        return createMapInfo(datum, new MapProjection("wullidutsch", transform));
    }

    private MapInfo createMapInfo(Datum datum, MapProjection projection) {
        return new MapInfo(projection, -1.2f, 3.4f, 5.6f, -7.8f, 0.9f, -1.0f, datum);
    }

}
