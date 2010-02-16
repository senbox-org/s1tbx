package com.bc.ceres.binio.binx;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ArrayVariableTest extends TestCase {
    public void testBinX() throws IOException, BinXException, URISyntaxException {
        URL resource = getClass().getResource("ArrayVariable.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();
        binx.setSingleDatasetStructInlined(true);
        binx.setArrayVariableInlined(true);
        DataFormat dataFormat = binx.readDataFormat(uri);

        assertTrue(binx.getDefinition("Ernie") instanceof CompoundType);
        CompoundType ernieType = (CompoundType) binx.getDefinition("Ernie");
        assertEquals("Ernie", ernieType.getName());
        assertEquals(2, ernieType.getMemberCount());
        assertEquals("af32_Counter", ernieType.getMember(0).getName());
        assertEquals("af32", ernieType.getMember(1).getName());
        assertEquals("float[$af32_Counter]", ernieType.getMember(1).getType().getName());

        assertTrue(binx.getDefinition("Bert") instanceof CompoundType);
        CompoundType bertType = (CompoundType) binx.getDefinition("Bert");
        assertEquals("Bert", bertType.getName());
        assertEquals(3, bertType.getMemberCount());
        assertEquals("ai32_count", bertType.getMember(0).getName());
        assertEquals("ai32", bertType.getMember(1).getName());
        assertEquals("int[$ai32_count]", bertType.getMember(1).getType().getName());
        assertEquals("flags", bertType.getMember(2).getName());

        CompoundType datasetType = dataFormat.getType();
        assertNotNull(datasetType);
        assertEquals("Dataset", datasetType.getName());
        assertEquals(5,datasetType.getMemberCount());
        assertEquals("magic1", datasetType.getMember(0).getName());
        assertEquals("data1", datasetType.getMember(1).getName());
        assertEquals("magic2", datasetType.getMember(2).getName());
        assertEquals("data2", datasetType.getMember(3).getName());
        assertEquals("magic3", datasetType.getMember(4).getName());

        assertSame(ernieType, datasetType.getMember(1).getType());
        assertSame(bertType,datasetType.getMember(3).getType());
    }

    public void testFormat() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("ArrayVariable.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();

        DataFormat format = binx.readDataFormat(uri, "ArrayVariableTest");
        assertEquals("ArrayVariableTest", format.getName());
        assertNotNull(format.getType());
        assertEquals("Dataset", format.getType().getName());
        assertEquals(true, format.isTypeDef("Ernie"));
        assertTrue(format.getTypeDef("Ernie") instanceof CompoundType);
    }
}
