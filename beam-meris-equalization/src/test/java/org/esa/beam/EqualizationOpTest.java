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

package org.esa.beam;

import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Test;

import static junit.framework.Assert.*;

public class EqualizationOpTest {

    @Test(expected = OperatorException.class)
    public void testParseReproVersion_MERIS_Fails() {
        EqualizationOp.parseReprocessingVersion("MERIS", 4.67f);
    }

    @Test(expected = OperatorException.class)
    public void testParseReproVersion_MEGS_Fails() {
        EqualizationOp.parseReprocessingVersion("MEGS-PC", 8.1f);
    }

    @Test
    public void testParseReproVersion() {
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.02f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.03f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.04f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.05f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MEGS-PC", 7.4f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MEGS-PC", 7.41f));

        assertEquals(3, EqualizationOp.parseReprocessingVersion("MEGS-PC", 8.0f));
    }

    @Test
    public void testToJulianDay() {
        assertEquals(2455414, (long) JulianDate.julianDate(2010, 7, 6));
        assertEquals(2452365, (long) JulianDate.julianDate(2002, 3, 1));

    }
}
