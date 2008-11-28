package com.bc.ceres.binio.binx;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.Type;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;


public class DBL_SM_XXXX_MIR_SMUDP2_0100_Test extends TestCase {

    public void testBinXIO() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("DBL_SM_XXXX_MIR_SMUDP2_0100.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();
        binx.setSingleDatasetStructInlined(true);
        binx.setArrayVariableInlined(true);

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
        assertEquals("N_Grid_Points", dataFormat.getType().getMember(0).getName());
        assertEquals("SM_SWATH", dataFormat.getType().getMember(1).getName());

        assertEquals("Grid_Point_Data_Type[$N_Grid_Points]", dataFormat.getType().getMember(1).getType().getName());
    }
}
