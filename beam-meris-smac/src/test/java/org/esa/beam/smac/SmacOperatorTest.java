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

package org.esa.beam.smac;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SmacOperatorTest {

    @Test
    public void testConvertAndRevert() throws Exception {
        HashMap<String, String> map = new HashMap<>();
        assertEquals("reflec_2", SmacOperator.convertMerisBandName("radiance_2", map));
        assertEquals("reflec_5", SmacOperator.convertMerisBandName("radiance_5", map));
        assertEquals("reflec", SmacOperator.convertMerisBandName("kaputtnick", map));
        assertEquals("radiance_2", map.get("reflec_2"));
        assertEquals("radiance_5", map.get("reflec_5"));
        assertEquals("kaputtnick", map.get("reflec"));


        map.clear();
        assertEquals("radiance_2", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("radiance_2", map), map));
        assertEquals("radiance_5", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("radiance_5", map), map));
        assertEquals("kaputtnick", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("kaputtnick", map), map));

        assertEquals("i dont exist", SmacOperator.revertMerisBandName("i dont exist", map));
    }
}
