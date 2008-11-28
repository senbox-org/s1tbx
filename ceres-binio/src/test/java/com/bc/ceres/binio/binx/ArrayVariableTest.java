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

public class ArrayVariableTest extends TestCase {
    public void testBinX() throws IOException, BinXException, URISyntaxException {
        BinX binx = createBinX();

        Map<String, Type> definitions = binx.getDefinitions();
        assertTrue(definitions.get("Ernie") instanceof CompoundType);
        CompoundType ernieType = (CompoundType) definitions.get("Ernie");
        assertEquals("Ernie", ernieType.getName());
        assertEquals(2, ernieType.getMemberCount());
        assertEquals("af32_Counter", ernieType.getMember(0).getName());
        assertEquals("af32", ernieType.getMember(1).getName());
        assertEquals("float[$af32_Counter]", ernieType.getMember(1).getType().getName());

        assertTrue(definitions.get("Bert") instanceof CompoundType);
        CompoundType bertType = (CompoundType) definitions.get("Bert");
        assertEquals("Bert", bertType.getName());
        assertEquals(3, bertType.getMemberCount());
        assertEquals("ai32_count", bertType.getMember(0).getName());
        assertEquals("ai32", bertType.getMember(1).getName());
        assertEquals("int[$ai32_count]", bertType.getMember(1).getType().getName());
        assertEquals("flags", bertType.getMember(2).getName());

        assertNotNull(binx.getDataset());
        assertEquals("Dataset", binx.getDataset().getName());
        assertEquals(5, binx.getDataset().getMemberCount());
        assertEquals("magic1", binx.getDataset().getMember(0).getName());
        assertEquals("data1", binx.getDataset().getMember(1).getName());
        assertEquals("magic2", binx.getDataset().getMember(2).getName());
        assertEquals("data2", binx.getDataset().getMember(3).getName());
        assertEquals("magic3", binx.getDataset().getMember(4).getName());

        assertSame(ernieType, binx.getDataset().getMember(1).getType());
        assertSame(bertType, binx.getDataset().getMember(3).getType());
    }

    public void testFormat() throws URISyntaxException, IOException, BinXException {
        BinX binx = createBinX();

        DataFormat format = binx.getFormat("ArrayVariableTest");
        assertEquals("ArrayVariableTest", format.getName());
        assertNotNull(format.getType());
        assertEquals("Dataset", format.getType().getName());
        assertEquals(true, format.isTypeDef("Ernie"));
        assertTrue(format.getTypeDef("Ernie") instanceof CompoundType);
    }

    private BinX createBinX() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("ArrayVariable.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        return new BinX(uri);
    }
}
