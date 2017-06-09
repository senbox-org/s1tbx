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

package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import junit.framework.TestCase;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;

import java.io.IOException;
import java.util.Arrays;


public class FeatureUtilsTest extends TestCase {

    public void testCollectionClipping() throws IOException {

        GeometryType gt1 = new GeometryTypeImpl(new NameImpl("geometry"), Polygon.class,DefaultGeographicCRS.WGS84,
                                                false, false, null, null, null);

        AttributeType at2 = new AttributeTypeImpl(new NameImpl("label"), String.class,
                                                  false, false, null, null, null);

        GeometryDescriptor gd1 = new GeometryDescriptorImpl(gt1, new NameImpl("geometry"),
                                                            0, 1,false, null);

        AttributeDescriptor ad2 = new AttributeDescriptorImpl(at2,new NameImpl("LABEL"),
                                                              0, 1,false,null);

        SimpleFeatureType marcoType = new SimpleFeatureTypeImpl(new NameImpl("MarcoType"), Arrays.asList(gd1, ad2), gd1,
                                                                false, null, null, null);


        GeometryFactory gf = new GeometryFactory();
        Object[] data1 = {gf.toGeometry(new Envelope(0, 10, 0, 10)), "G1"};
        Object[] data2 = {gf.toGeometry(new Envelope(20, 30, 0, 10)), "G2"};
        Object[] data3 = {gf.toGeometry(new Envelope(40, 50, 0, 10)), "G3"};
        SimpleFeatureImpl f1 = new SimpleFeatureImpl(data1, marcoType, new FeatureIdImpl("MarcoF1"), true);
        SimpleFeatureImpl f2 = new SimpleFeatureImpl(data2, marcoType, new FeatureIdImpl("MarcoF2"), true);
        SimpleFeatureImpl f3 = new SimpleFeatureImpl(data3, marcoType, new FeatureIdImpl("MarcoF3"), true);

        MemoryDataStore dataStore = new MemoryDataStore(new SimpleFeature[]{f1, f2, f3});

        // F1 - no intersection
        // F2 - vertically clipped in the half
        // F3 - fully inside
        Geometry clipGeometry = gf.toGeometry(new Envelope(25, 55, -5, 15));

        FeatureSource<SimpleFeatureType, SimpleFeature> marcoSource = dataStore.getFeatureSource("MarcoType");
        assertNotNull(marcoSource);
        assertNull(marcoSource.getFeatures().getID());
        assertSame(dataStore, marcoSource.getDataStore());
        assertSame(marcoType, marcoSource.getSchema());
        assertEquals(3, marcoSource.getCount(Query.ALL));
        assertEquals("MarcoType", marcoSource.getName().toString());
        final ReferencedEnvelope expectedEnvelope = new ReferencedEnvelope(0, 50, 0, 10,
                                                                           DefaultGeographicCRS.WGS84);
        assertTrue(expectedEnvelope.boundsEquals2D(marcoSource.getBounds(), 1.0e-6));
        assertTrue(expectedEnvelope.boundsEquals2D(marcoSource.getBounds(Query.ALL), 1.0e-6));

        FeatureCollection<SimpleFeatureType, SimpleFeature> normanSource = FeatureUtils.clipCollection(marcoSource.getFeatures(),
                                                                                                       null,
                                                                                                       clipGeometry,
                                                                                                       DefaultGeographicCRS.WGS84,
                                                                                                       "normansClippedCollection",
                                                                                                       DefaultGeographicCRS.WGS84,
                                                                                                       ProgressMonitor.NULL);

        assertNotNull(normanSource);
        assertEquals("normansClippedCollection", normanSource.getID());
        assertEquals(marcoType, normanSource.getSchema());
        assertEquals(2, normanSource.size());
        assertEquals(25, normanSource.getBounds().getMinX(), 1.0e-10);
        assertEquals(50, normanSource.getBounds().getMaxX(), 1.0e-10);
        assertEquals(0, normanSource.getBounds().getMinY(), 1.0e-10);
        assertEquals(10, normanSource.getBounds().getMaxY(), 1.0e-10);
    }

}
