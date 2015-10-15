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


public class KmlDocumentTest {

    @Test
    public void testExportSimple() throws Exception {
        KmlDocument kmlDocument = new KmlDocument("Pluto", "Dog of Mickey");
        StringBuilder builder = new StringBuilder();

        kmlDocument.createKml(builder);
        String actual = builder.toString();
        assertEquals(getExpectedExportSimple(), actual);
    }

    @Test
    public void testExportWithChildren() throws Exception {
        KmlDocument kmlDocument = new KmlDocument("Pluto", "Dog of Mickey");
        kmlDocument.addChild(new DummyTestFeature("Dummy"));

        StringBuilder builder = new StringBuilder();
        kmlDocument.createKml(builder);
        String actual = builder.toString();
        assertEquals(getExpectedExportWithChildren(), actual);
    }

    private String getExpectedExportSimple() {
        return "<Document>" +
               "<name>Pluto</name>" +
               "<description>Dog of Mickey</description>" +
               "</Document>";
    }

    private String getExpectedExportWithChildren() {
        return "<Document>" +
               "<name>Pluto</name>" +
               "<description>Dog of Mickey</description>" +
               "<Dummy>" +
               "<name>Dummy</name>" +
               "<innerElement>some valuable information</innerElement" +
               "</Dummy>" +
               "</Document>";
    }

}
