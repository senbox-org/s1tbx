/*
 * $Id: BeamFormatterTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation. This program is distributed in the hope it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
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

        assertTrue(-1 != formattedMessage.indexOf(message));
        assertTrue(-1 != formattedMessage.indexOf("INFO"));
    }
}
