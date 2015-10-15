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

import static org.junit.Assert.*;

public class KmlScreenOverlayTest {

    @Test
    public void testExport() {
        KmlScreenOverlay screenOverlay = new KmlScreenOverlay("Legend", new DummyTestOpImage(2, 2));

        String expected = getExpected();
        StringBuilder builder = new StringBuilder();
        screenOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    private String getExpected() {
        return "<ScreenOverlay>" +
               "<name>Legend</name>" +
               "<Icon>" +
               "<href>Legend.png</href>" +
               "</Icon>" +
               "<overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />" +
               "<screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />" +
               "</ScreenOverlay>";
    }

}
