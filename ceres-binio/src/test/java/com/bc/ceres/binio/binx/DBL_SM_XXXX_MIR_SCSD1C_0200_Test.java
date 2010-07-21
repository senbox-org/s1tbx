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
package com.bc.ceres.binio.binx;

import junit.framework.TestCase;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.io.IOException;
import java.util.Map;

import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;

public class DBL_SM_XXXX_MIR_SCSD1C_0200_Test extends TestCase {

    public void testBinXIO() throws URISyntaxException, IOException, BinXException {
        final URL resource = getClass().getResource("DBL_SM_XXXX_MIR_SCSD1C_0200.binXschema.xml");
        assertNotNull(resource);

        final URI uri = resource.toURI();
        BinX binx = new BinX();
        binx.setSingleDatasetStructInlined(true);
        binx.setArrayVariableInlined(true);
        binx.setVarNameMapping("Temp_Swath_Dual", "Grid_Point_Data");
        binx.setVarNameMapping("Swath_Snapshot_List", "Snapshot_Information");

        final DataFormat dataFormat = binx.readDataFormat(uri, "DBL_SM_XXXX_MIR_SCSD1C_0200");
        assertNotNull(dataFormat);
        assertNotNull(dataFormat.getType());
        assertEquals("DBL_SM_XXXX_MIR_SCSD1C_0200", dataFormat.getName());

        assertTrue(binx.getDefinition("Quality_Information_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("UTC_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("BT_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Snapshot_Information_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Grid_Point_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Data_Block_Type") instanceof CompoundType);

        assertEquals("Data_Block", dataFormat.getType().getName());
        assertEquals(4, dataFormat.getType().getMemberCount());
        assertEquals("Snapshot_Counter", dataFormat.getType().getMember(0).getName());
        assertEquals("Snapshot_Information", dataFormat.getType().getMember(1).getName());
        assertEquals("Grid_Point_Counter", dataFormat.getType().getMember(2).getName());
        assertEquals("Grid_Point_Data", dataFormat.getType().getMember(3).getName());

        assertEquals("Snapshot_Information_Type[$Snapshot_Counter]", dataFormat.getType().getMember(1).getType().getName());
        assertEquals("Grid_Point_Data_Type[$Grid_Point_Counter]", dataFormat.getType().getMember(3).getType().getName());
    }
}
