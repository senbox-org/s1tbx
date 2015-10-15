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
package org.esa.snap.core.dataop.maptransf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MapInfoTest extends TestCase {

    public MapInfoTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MapInfoTest.class);
    }

    public void testCreateDeepClone() {
        MapInfo original = createMapInfo();
        original.setNoDataValue(-1.23456);
        original.setOrthorectified(true);
        original.setSceneSizeFitted(true);
        original.setElevationModelName("G8");

        MapInfo deepClone = original.createDeepClone();

        assertEquals(deepClone.getMapProjection(), original.getMapProjection());
        assertEquals(deepClone.getDatum(), original.getDatum());
        assertEquals(deepClone.getPixelX(), original.getPixelX(), 1e-6f);
        assertEquals(deepClone.getPixelY(), original.getPixelY(), 1e-6f);
        assertEquals(deepClone.getPixelSizeX(), original.getPixelSizeX(), 1e-6f);
        assertEquals(deepClone.getPixelSizeY(), original.getPixelSizeY(), 1e-6f);
        assertEquals(deepClone.getNorthing(), original.getNorthing(), 1e-6f);
        assertEquals(deepClone.getEasting(), original.getEasting(), 1e-6f);
        assertEquals(deepClone.getOrientation(), original.getOrientation(), 1e-6f);
        assertEquals(deepClone.getElevationModelName(), original.getElevationModelName());
        assertEquals(deepClone.getSceneWidth(), original.getSceneWidth());
        assertEquals(deepClone.getSceneHeight(), original.getSceneHeight());
        assertEquals(deepClone.getNoDataValue(), original.getNoDataValue(), 1e-10);
        assertEquals(deepClone.isOrthorectified(), original.isOrthorectified());
        assertEquals(deepClone.isSceneSizeFitted(), original.isSceneSizeFitted());
    }

    public void testToString() {
        // Note: we need this test because BEAM-DIMAP writer uses toString in order to serialize a mapInfo
        // todo - remove this dependency from toString!
        assertEquals("pro_name, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, datumName, units=degree, 123, 234",
                     createMapInfo().toString());
    }

    private MapInfo createMapInfo() {
        MapTransform transform = MapTransformFactory.createTransform("Identity", null);
        MapProjection projection = new MapProjection("pro_name", transform);
        Datum datum = new Datum("datumName", new Ellipsoid("ellipsoidName", 7d, 8d), 0, 0, 0);
        MapInfo mapInfo = new MapInfo(projection, 1f, 2f, 3f, 4f, 5f, 6f, datum);
        mapInfo.setSceneWidth(123);
        mapInfo.setSceneHeight(234);
        return mapInfo;
    }
}
