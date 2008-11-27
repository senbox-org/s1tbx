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
        BinX binx = new BinX(uri);

        assertSame(uri, binx.getURI());
        assertEquals("http://www.edikt.org/binx/2003/06/binx", binx.getNamespace());

        Map<String, Type> definitions = binx.getDefinitions();
        assertTrue(definitions.get("UTC_Type") instanceof CompoundType);
        assertTrue(definitions.get("Processing_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(definitions.get("Science_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(definitions.get("Confidence_Descriptors_Data_Type") instanceof CompoundType);
        assertTrue(definitions.get("Retrieval_Results_Data_Type") instanceof CompoundType);
        assertTrue(definitions.get("Grid_Point_Data_Type") instanceof CompoundType);
        assertTrue(definitions.get("Data_Block_Type") instanceof CompoundType);

        assertNotNull(binx.getDataset());
        assertEquals("Dataset", binx.getDataset().getName());
        assertEquals(2, binx.getDataset().getMemberCount());
        assertEquals("N_Grid_Points", binx.getDataset().getMember(0).getName());
        assertEquals("Data", binx.getDataset().getMember(1).getName());

        assertEquals("Grid_Point_Data_Type[$N_Grid_Points]", binx.getDataset().getMember(1).getType().getName());

        DataFormat format = binx.getFormat("DBL_SM_XXXX_MIR_SMUDP2_0100");
        assertNotNull(format);
        assertEquals("DBL_SM_XXXX_MIR_SMUDP2_0100", format.getName());
        assertSame(binx.getDataset(), format.getType());
    }
}
