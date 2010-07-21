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
package org.esa.beam.util.logging;

import junit.framework.TestCase;

public class BeamLogManagerTest extends TestCase {

    private final String _name = "a name";
    private final String _version = "a version";
    private final String _copyright = "a copyright";

    public void testGetHeader() {
        final String logHeader = BeamLogManager.createLogHeader(_name, _version, _copyright);
        assertNotNull(logHeader);
        assertTrue(logHeader.indexOf(_name) > 0);
        assertTrue(logHeader.indexOf(_version) > 0);
        assertTrue(logHeader.indexOf(_copyright) > 0);
    }

    public void testCreateFormatter() {
        final BeamFormatter formatter = BeamLogManager.createFormatter(_name, _version, _copyright);
        assertNotNull(formatter);
        assertTrue(formatter instanceof BeamFormatter);
        assertEquals(formatter.getHead(null), BeamLogManager.createLogHeader(_name, _version, _copyright));
    }
}

