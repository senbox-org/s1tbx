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

import org.junit.Test;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class KmlPlacemarkTest {

    @Test
    public void testExport() throws Exception {
        KmlPlacemark kmlPlacemark = new KmlPlacemark("Pin", null, new Point2D.Double(12.5, 60.9));
        StringBuilder builder = new StringBuilder();
        kmlPlacemark.createKml(builder);

        assertEquals("<Placemark>" +
                     "<name>Pin</name>" +
                     "<Point>" +
                     "<coordinates>12.5,60.9,0</coordinates>" +
                     "</Point>" +
                     "</Placemark>", builder.toString());
    }

    @Test
    public void testExportWithExtendedData() throws Exception {
        KmlPlacemark kmlPlacemark = new KmlPlacemark("Pin", null, new Point2D.Double(1.01, 2.0));
        ExtendedData extendedData = new ExtendedData();
        extendedData.add("Clark Kent", "Superman", "Hero");
        kmlPlacemark.setExtendedData(extendedData);
        StringBuilder builder = new StringBuilder();
        kmlPlacemark.createKml(builder);

        assertEquals("<Placemark>" +
                     "<name>Pin</name>" +
                     "<Point>" +
                     "<coordinates>1.01,2.0,0</coordinates>" +
                     "</Point>" +
                     "<ExtendedData>" +
                     "<Data name=\"Clark Kent\">" +
                     "<displayName>Superman</displayName>" +
                     "<value>Hero</value>" +
                     "</Data>" +
                     "</ExtendedData>" +
                     "</Placemark>", builder.toString());
    }

}
