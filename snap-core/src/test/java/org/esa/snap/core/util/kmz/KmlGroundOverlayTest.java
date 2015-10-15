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

package org.esa.snap.core.util.kmz;

import org.esa.snap.core.datamodel.ProductData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

public class KmlGroundOverlayTest {

    @Test
    public void testExportWithoutTime() {
        ReferencedEnvelope latLonBox = new ReferencedEnvelope(12, 13, -30, -31, DefaultGeographicCRS.WGS84);
        KmlGroundOverlay groundOverlay = new KmlGroundOverlay("scene", new DummyTestOpImage(3, 3), latLonBox);

        String expected = getExpectedWithoutTime();
        StringBuilder builder = new StringBuilder();
        groundOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    @Test
    public void testExportWithTime() throws ParseException {
        ReferencedEnvelope latLonBox = new ReferencedEnvelope(12, 13, -30, -31, DefaultGeographicCRS.WGS84);
        ProductData.UTC startTime = ProductData.UTC.parse("10-03-1999", "dd-MM-yyyy");
        ProductData.UTC endTime = ProductData.UTC.parse("11-03-1999", "dd-MM-yyyy");
        KmlGroundOverlay groundOverlay = new KmlGroundOverlay("scene", new DummyTestOpImage(3, 3),
                                                              latLonBox, startTime, endTime);
        String expected = getExpectedWithTime();
        StringBuilder builder = new StringBuilder();
        groundOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    private String getExpectedWithoutTime() {
        return "<GroundOverlay>" +
               "<name>scene</name>" +
               "<Icon>" +
               "<href>scene.png</href>" +
               "</Icon>" +
               "<LatLonBox>" +
               "<north>-30.0</north>" +
               "<south>-31.0</south>" +
               "<east>13.0</east>" +
               "<west>12.0</west>" +
               "</LatLonBox>" +
               "</GroundOverlay>";
    }

    private String getExpectedWithTime() {
        return "<GroundOverlay>" +
               "<name>scene</name>" +
               "<Icon>" +
               "<href>scene.png</href>" +
               "</Icon>" +
               "<TimeSpan>" +
               "<begin>1999-03-10</begin>" +
               "<end>1999-03-11</end>" +
               "</TimeSpan>" +
               "<LatLonBox>" +
               "<north>-30.0</north>" +
               "<south>-31.0</south>" +
               "<east>13.0</east>" +
               "<west>12.0</west>" +
               "</LatLonBox>" +
               "</GroundOverlay>";
    }
}
