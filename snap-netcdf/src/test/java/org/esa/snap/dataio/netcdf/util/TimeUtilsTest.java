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

package org.esa.snap.dataio.netcdf.util;

import org.junit.Test;
import ucar.nc2.Attribute;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class TimeUtilsTest {

    @Test
    public void testGetDateTimeString() throws Exception {
        String dateTimeString = TimeUtils.getDateTimeString(new Attribute("start_date", "2010-01-31"), new Attribute("start_time", "10:00:22"));
        assertEquals("2010-01-31 10:00:22", dateTimeString);
    }
}
