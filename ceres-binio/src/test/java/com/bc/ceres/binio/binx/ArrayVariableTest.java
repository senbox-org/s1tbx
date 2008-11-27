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
        assertTrue(definitions.get("VarFloat32ArrayType") instanceof CompoundType);
        CompoundType arrayCompoundType = (CompoundType) definitions.get("VarFloat32ArrayType");
        assertEquals("VarFloat32ArrayType", arrayCompoundType.getName());
        assertEquals(2, arrayCompoundType.getMemberCount());
        assertEquals("Length", arrayCompoundType.getMember(0).getName());
        assertEquals("Data", arrayCompoundType.getMember(1).getName());
        assertEquals("float[$Length]", arrayCompoundType.getMember(1).getType().getName());

        assertNotNull(binx.getDataset());
        assertEquals("Dataset", binx.getDataset().getName());
        assertEquals(5, binx.getDataset().getMemberCount());
        assertEquals("magic1", binx.getDataset().getMember(0).getName());
        assertEquals("data1", binx.getDataset().getMember(1).getName());
        assertEquals("magic2", binx.getDataset().getMember(2).getName());
        assertEquals("data2", binx.getDataset().getMember(3).getName());
        assertEquals("magic3", binx.getDataset().getMember(4).getName());

        assertSame(arrayCompoundType, binx.getDataset().getMember(1).getType());
        assertSame(arrayCompoundType, binx.getDataset().getMember(3).getType());
    }

    public void testFormat() throws URISyntaxException, IOException, BinXException {
        BinX binx = createBinX();

        DataFormat format = binx.getFormat("ArrayVariableTest");
        assertEquals("ArrayVariableTest", format.getName());
        assertNotNull(format.getType());
        assertEquals("Dataset", format.getType().getName());
        assertEquals(true, format.isTypeDef("VarFloat32ArrayType"));
        assertTrue(format.getTypeDef("VarFloat32ArrayType") instanceof CompoundType);
    }

    private BinX createBinX() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("ArrayVariable.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        return new BinX(uri);
    }
}
