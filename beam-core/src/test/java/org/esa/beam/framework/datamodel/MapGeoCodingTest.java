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
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.Point;
import java.awt.geom.AffineTransform;

public class MapGeoCodingTest extends TestCase {

    // TODO: complete this test
    public void testIdentityMapGeocidingCRS() throws FactoryException {
/*
        final MapProjection[] projections = MapProjectionRegistry.getProjections();

        for (MapProjection projection : projections) {
            System.out.println("projection.getName() = " + projection.getName());
        }

        projection.getName() = UTM Automatic
        projection.getName() = UTM Zone 1
        projection.getName() = UTM Zone 2
        projection.getName() = UTM Zone 3
        projection.getName() = UTM Zone 4
        projection.getName() = UTM Zone 5
        projection.getName() = UTM Zone 6
        projection.getName() = UTM Zone 7
        projection.getName() = UTM Zone 8
        projection.getName() = UTM Zone 9
        projection.getName() = UTM Zone 10
        projection.getName() = UTM Zone 11
        projection.getName() = UTM Zone 12
        projection.getName() = UTM Zone 13
        projection.getName() = UTM Zone 14
        projection.getName() = UTM Zone 15
        projection.getName() = UTM Zone 16
        projection.getName() = UTM Zone 17
        projection.getName() = UTM Zone 18
        projection.getName() = UTM Zone 19
        projection.getName() = UTM Zone 20
        projection.getName() = UTM Zone 21
        projection.getName() = UTM Zone 22
        projection.getName() = UTM Zone 23
        projection.getName() = UTM Zone 24
        projection.getName() = UTM Zone 25
        projection.getName() = UTM Zone 26
        projection.getName() = UTM Zone 27
        projection.getName() = UTM Zone 28
        projection.getName() = UTM Zone 29
        projection.getName() = UTM Zone 30
        projection.getName() = UTM Zone 31
        projection.getName() = UTM Zone 32
        projection.getName() = UTM Zone 33
        projection.getName() = UTM Zone 34
        projection.getName() = UTM Zone 35
        projection.getName() = UTM Zone 36
        projection.getName() = UTM Zone 37
        projection.getName() = UTM Zone 38
        projection.getName() = UTM Zone 39
        projection.getName() = UTM Zone 40
        projection.getName() = UTM Zone 41
        projection.getName() = UTM Zone 42
        projection.getName() = UTM Zone 43
        projection.getName() = UTM Zone 44
        projection.getName() = UTM Zone 45
        projection.getName() = UTM Zone 46
        projection.getName() = UTM Zone 47
        projection.getName() = UTM Zone 48
        projection.getName() = UTM Zone 49
        projection.getName() = UTM Zone 50
        projection.getName() = UTM Zone 51
        projection.getName() = UTM Zone 52
        projection.getName() = UTM Zone 53
        projection.getName() = UTM Zone 54
        projection.getName() = UTM Zone 55
        projection.getName() = UTM Zone 56
        projection.getName() = UTM Zone 57
        projection.getName() = UTM Zone 58
        projection.getName() = UTM Zone 59
        projection.getName() = UTM Zone 60
        projection.getName() = UTM Zone 1, South
        projection.getName() = UTM Zone 2, South
        projection.getName() = UTM Zone 3, South
        projection.getName() = UTM Zone 4, South
        projection.getName() = UTM Zone 5, South
        projection.getName() = UTM Zone 6, South
        projection.getName() = UTM Zone 7, South
        projection.getName() = UTM Zone 8, South
        projection.getName() = UTM Zone 9, South
        projection.getName() = UTM Zone 10, South
        projection.getName() = UTM Zone 11, South
        projection.getName() = UTM Zone 12, South
        projection.getName() = UTM Zone 13, South
        projection.getName() = UTM Zone 14, South
        projection.getName() = UTM Zone 15, South
        projection.getName() = UTM Zone 16, South
        projection.getName() = UTM Zone 17, South
        projection.getName() = UTM Zone 18, South
        projection.getName() = UTM Zone 19, South
        projection.getName() = UTM Zone 20, South
        projection.getName() = UTM Zone 21, South
        projection.getName() = UTM Zone 22, South
        projection.getName() = UTM Zone 23, South
        projection.getName() = UTM Zone 24, South
        projection.getName() = UTM Zone 25, South
        projection.getName() = UTM Zone 26, South
        projection.getName() = UTM Zone 27, South
        projection.getName() = UTM Zone 28, South
        projection.getName() = UTM Zone 29, South
        projection.getName() = UTM Zone 30, South
        projection.getName() = UTM Zone 31, South
        projection.getName() = UTM Zone 32, South
        projection.getName() = UTM Zone 33, South
        projection.getName() = UTM Zone 34, South
        projection.getName() = UTM Zone 35, South
        projection.getName() = UTM Zone 36, South
        projection.getName() = UTM Zone 37, South
        projection.getName() = UTM Zone 38, South
        projection.getName() = UTM Zone 39, South
        projection.getName() = UTM Zone 40, South
        projection.getName() = UTM Zone 41, South
        projection.getName() = UTM Zone 42, South
        projection.getName() = UTM Zone 43, South
        projection.getName() = UTM Zone 44, South
        projection.getName() = UTM Zone 45, South
        projection.getName() = UTM Zone 46, South
        projection.getName() = UTM Zone 47, South
        projection.getName() = UTM Zone 48, South
        projection.getName() = UTM Zone 49, South
        projection.getName() = UTM Zone 50, South
        projection.getName() = UTM Zone 51, South
        projection.getName() = UTM Zone 52, South
        projection.getName() = UTM Zone 53, South
        projection.getName() = UTM Zone 54, South
        projection.getName() = UTM Zone 55, South
        projection.getName() = UTM Zone 56, South
        projection.getName() = UTM Zone 57, South
        projection.getName() = UTM Zone 58, South
        projection.getName() = UTM Zone 59, South
        projection.getName() = UTM Zone 60, South
        projection.getName() = Transverse Mercator
        projection.getName() = Albers Equal Area Conic
        projection.getName() = Lambert Conformal Conic
        projection.getName() = Geographic Lat/Lon
        projection.getName() = Stereographic
        projection.getName() = Universal Polar Stereographic North
        projection.getName() = Universal Polar Stereographic South
*/

        MapInfo mapInfo = createMapInfo(Datum.WGS_84, MapProjectionRegistry.getProjection("Geographic Lat/Lon"));

        // assertEquals(getCRS(mapInfo), DefaultGeographicCRS.WGS84);
    }

    private static CoordinateReferenceSystem getCRS(MapInfo mapInfo) throws FactoryException {
        final DefaultMathTransformFactory mtf = new DefaultMathTransformFactory();
/*
        method.getName() = ESRI:Stereographic_North_Pole
        method.getName() = OGC:Mercator_1SP
        method.getName() = OGC:Oblique_Mercator
        method.getName() = OGC:Hotine_Oblique_Mercator
        method.getName() = OGC:Mercator_2SP
        method.getName() = OGC:Lambert_Conformal_Conic_2SP_Belgium
        method.getName() = OGC:Lambert_Conformal_Conic_2SP
        method.getName() = OGC:Albers_Conic_Equal_Area
        method.getName() = ESRI:Stereographic_South_Pole
        method.getName() = OGC:New_Zealand_Map_Grid
        method.getName() = OGC:Lambert_Azimuthal_Equal_Area
        method.getName() = ESRI:Hotine_Oblique_Mercator_Two_Point_Center
        method.getName() = ESRI:Hotine_Oblique_Mercator_Two_Point_Natural_Origin
        method.getName() = ESRI:Lambert_Conformal_Conic
        method.getName() = OGC:Krovak
        method.getName() = OGC:Polar_Stereographic
        method.getName() = OGC:Oblique_Stereographic
        method.getName() = ESRI:Stereographic
        method.getName() = OGC:Orthographic
        method.getName() = OGC:Transverse_Mercator
        method.getName() = EPSG:Transverse Mercator (South Orientated)
        method.getName() = OGC:Lambert_Conformal_Conic_1SP
        method.getName() = ESRI:Plate_Carree
        method.getName() = OGC:Equidistant_Cylindrical
        method.getName() = EPSG:Polar Stereographic (variant B)

        Set<OperationMethod> methods = mtf.getAvailableMethods(Projection.class);
        for (OperationMethod method : methods) {
            System.out.println("method.getName() = " + method.getName());
        }
*/

//      TODO: create CRS from map info
//      TODO: also see http://docs.codehaus.org/display/GEOTDOC/Coordinate+Transformation+Parameters
        final ParameterValueGroup p = mtf.getDefaultParameters("Transverse_Mercator");
        final Datum datum = mapInfo.getDatum();
        p.parameter("semi_major").setValue(datum.getEllipsoid().getSemiMajor());
        p.parameter("semi_minor").setValue(datum.getEllipsoid().getSemiMinor());

        final MathTransform mt = mtf.createParameterizedTransform(p);

        return new DefaultProjectedCRS("tm", DefaultGeographicCRS.WGS84, mt, DefaultCartesianCS.PROJECTED);
    }

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
