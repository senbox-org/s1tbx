/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.Before;
import org.junit.Test;
import thredds.catalog.InvDataset;
import thredds.catalog.ThreddsMetadata;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.swing.JCheckBox;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class RegionFilterTest {

    private RegionFilter regionFilter;

    /**
     * Creates the following filter bounding box:
     *
     *   50.0N/-10.0E ---------- 50.0N/20.0E
     *      |                       |
     *      |                       |
     *      |                       |
     *  -70.0N/-10.0E ----------- -70.0N/20.0E
     *
     */
    @Before
    public void setUp() throws ValidationException {
        regionFilter = new RegionFilter(new JCheckBox());

        regionFilter.eastBoundProperty = new Property(new PropertyDescriptor("east", Double.class), new DefaultPropertyAccessor());
        regionFilter.westBoundProperty = new Property(new PropertyDescriptor("west", Double.class), new DefaultPropertyAccessor());
        regionFilter.northBoundProperty = new Property(new PropertyDescriptor("north", Double.class), new DefaultPropertyAccessor());
        regionFilter.southBoundProperty = new Property(new PropertyDescriptor("south", Double.class), new DefaultPropertyAccessor());

        regionFilter.eastBoundProperty.setValue(20.0);
        regionFilter.westBoundProperty.setValue(-10.0);
        regionFilter.northBoundProperty.setValue(50.0);
        regionFilter.southBoundProperty.setValue(-70.0);
    }

    @Test
    public void testAccept() throws Exception {
        OpendapLeaf opendapLeafFullyInside = createLeaf(45.0, -8.0, -68.0, 19.0);
        OpendapLeaf opendapLeafGoingHorizontallyThroughBox = createLeaf(89.0, -1.0, -89.0, 1.0);
        OpendapLeaf opendapLeafGoingVerticallyThroughBox = createLeaf(45.0, -179.0, -68.0, 179.0);
        OpendapLeaf opendapLeafContainingOneEdge = createLeaf(55.0, -12.0, 45.0, -8.0);
        OpendapLeaf opendapLeafWholeWorld = createLeaf(90.0, -180.0, -90.0, 180.0);

        OpendapLeaf opendapLeafAbove = createLeaf(90.0, -180.0, 80.0, 180.0);
        OpendapLeaf opendapLeafRight = createLeaf(90, 30.0, -90.0, 180.0);
        OpendapLeaf opendapLeafBelow = createLeaf(-80.0, -180, -90.0, 180.0);
        OpendapLeaf opendapLeafLeft = createLeaf(90.0, -180, -90.0, -11.0);

        assertTrue(regionFilter.accept(opendapLeafFullyInside));
        assertTrue(regionFilter.accept(opendapLeafGoingHorizontallyThroughBox));
        assertTrue(regionFilter.accept(opendapLeafGoingVerticallyThroughBox));
        assertTrue(regionFilter.accept(opendapLeafContainingOneEdge));
        assertTrue(regionFilter.accept(opendapLeafWholeWorld));

        assertFalse(regionFilter.accept(opendapLeafAbove));
        assertFalse(regionFilter.accept(opendapLeafRight));
        assertFalse(regionFilter.accept(opendapLeafBelow));
        assertFalse(regionFilter.accept(opendapLeafLeft));
    }

    private OpendapLeaf createLeaf(final double upperLeftLat, final double upperLeftLon, final double bottomRightLat, final double bottomRightLon) {
        return new OpendapLeaf("", new InvDataset(null, "") {
                @Override
                public ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
                    ThreddsMetadata.GeospatialCoverage geospatialCoverage = new ThreddsMetadata.GeospatialCoverage();
                    geospatialCoverage.setBoundingBox(new LatLonRect(
                            new LatLonPointImpl(upperLeftLat, upperLeftLon),
                            new LatLonPointImpl(bottomRightLat, bottomRightLon)));

                    return geospatialCoverage;
                }
            });
    }

}
