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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.TestCase;

public class BeamFormatterTest extends TestCase {

    private String _head = "The header String";

    public void testConstructor() {
        try {
            new BeamFormatter(null);
            fail("Exception expected here");
        } catch (IllegalArgumentException e) {
        }

        try {
            new BeamFormatter(_head);
        } catch (IllegalArgumentException e) {
            fail("No exception expected here");
        }
    }

    public void testFormat() {
        String message = "the logging message";
        LogRecord record = new LogRecord(Level.INFO, message);

        BeamFormatter formatter = new BeamFormatter(_head);
        String formattedMessage = formatter.format(record);

        assertTrue(formattedMessage.contains(message));
        assertTrue(formattedMessage.contains("INFO"));
    }
}
