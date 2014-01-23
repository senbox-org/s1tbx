/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.modis;

import org.junit.Test;

import static org.junit.Assert.*;

public class ModisProductReaderTest {

    @Test
    public void testIsGlobalAttributeName() {
         assertTrue(ModisProductReader.isGlobalAttributeName("StructMetadata\\.0"));
         assertTrue(ModisProductReader.isGlobalAttributeName("CoreMetadata\\.0"));
         assertTrue(ModisProductReader.isGlobalAttributeName("ArchiveMetadata\\.0"));

        assertFalse(ModisProductReader.isGlobalAttributeName("EV_250_Aggr1km_RefSB"));
        assertFalse(ModisProductReader.isGlobalAttributeName("property_thingy"));
        assertFalse(ModisProductReader.isGlobalAttributeName(""));
        assertFalse(ModisProductReader.isGlobalAttributeName(null));
    }
}
