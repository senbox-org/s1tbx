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

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceType;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class DBL_SM_XXXX_MIR_SMUDP2_0100_Test extends TestCase {

    public void testBinXIO() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("DBL_SM_XXXX_MIR_SMUDP2_0100.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();
        binx.setSingleDatasetStructInlined(true);
        binx.setArrayVariableInlined(true);
        binx.setVarNameMapping("N_Grid_Points", "Grid_Point_Counter");
        binx.setVarNameMapping("SM_SWATH", "Grid_Point_Data");
        binx.setTypeMembersInlined("Retrieval_Results_Data_Type", true);
        binx.setTypeMembersInlined("Confidence_Descriptors_Data_Type", true);
        binx.setTypeMembersInlined("Science_Descriptors_Data_Type", true);
        binx.setTypeMembersInlined("Processing_Descriptors_Data_Type", true);

        DataFormat dataFormat = binx.readDataFormat(uri, "DBL_SM_XXXX_MIR_SMUDP2_0100");
        assertNotNull(dataFormat);
        assertEquals("DBL_SM_XXXX_MIR_SMUDP2_0100", dataFormat.getName());

        assertTrue(binx.getDefinition("UTC_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Processing_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Science_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Confidence_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Retrieval_Results_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Grid_Point_Data_Type") instanceof CompoundType);
        assertTrue(binx.getDefinition("Data_Block_Type") instanceof CompoundType);

        assertNotNull(dataFormat.getType());
        assertEquals("Data_Block", dataFormat.getType().getName());
        assertEquals(2, dataFormat.getType().getMemberCount());
        assertEquals("Grid_Point_Counter", dataFormat.getType().getMember(0).getName());
        assertEquals("Grid_Point_Data", dataFormat.getType().getMember(1).getName());

        assertEquals("Grid_Point_Data_Type[$Grid_Point_Counter]", dataFormat.getType().getMember(1).getType().getName());

        final SequenceType sequenceType = (SequenceType) dataFormat.getType().getMember(1).getType();
        final CompoundType elementType = (CompoundType) sequenceType.getElementType();
        assertEquals(57, elementType.getMemberCount());
    }
}
