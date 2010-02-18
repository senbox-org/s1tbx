/*
 * $Id: BeamLogManagerTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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

