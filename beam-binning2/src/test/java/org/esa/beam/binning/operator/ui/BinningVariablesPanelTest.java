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

package org.esa.beam.binning.operator.ui;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinningVariablesPanelTest {

    @Test
    public void testGetResolutionString() throws Exception {
        assertEquals("9.28 km/pixel", BinningVariablesPanel.getResolutionString(2160));
        assertEquals("5.86 km/pixel", BinningVariablesPanel.getResolutionString(3420));
        assertEquals("2.1 km/pixel", BinningVariablesPanel.getResolutionString(9544));
        assertEquals("15.81 km/pixel", BinningVariablesPanel.getResolutionString(1268));
    }
}
