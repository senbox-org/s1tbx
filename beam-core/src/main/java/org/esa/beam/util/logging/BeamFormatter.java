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

import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

import org.esa.beam.util.Guardian;


public class BeamFormatter extends SimpleFormatter {

    private String _head;

    /**
     * Constructs the logging formatter for the BEAM framework.
     *
     * @param head      the head string for this formatter
     */
    public BeamFormatter(String head) {
        Guardian.assertNotNull("head", head);
        _head = head;
    }

    /**
     * Return the header string for a set of formatted records.
     *
     * @param h The target handler.
     *
     * @return header string
     */
    @Override
    public String getHead(Handler h) {
        return _head;
    }
}
