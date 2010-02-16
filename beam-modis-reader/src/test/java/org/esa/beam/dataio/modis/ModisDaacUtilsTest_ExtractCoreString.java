package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.hdf.HdfAttributeContainer;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;

import java.util.ArrayList;

public class ModisDaacUtilsTest_ExtractCoreString extends TestCase {

    private HdfAttributes hdfGlobalAttributes;

    @Override
    protected void setUp() throws Exception {
        final ArrayList<HdfAttributeContainer> attribList = new ArrayList<HdfAttributeContainer>();
        attribList.add(new HdfAttributeContainer("CoreMetadata.0.somewhatelse", 4, "otherPart", 9));
        attribList.add(new HdfAttributeContainer("CoreMetadata.1", 4, "secondPart", 10));
        attribList.add(new HdfAttributeContainer("OtherMetadata.0", 4, "anyString", 9));
        attribList.add(new HdfAttributeContainer("CoreMetadata.0", 4, "firstPart", 9));
        hdfGlobalAttributes = new HdfAttributes(attribList);
    }

    public void testOk() {
        final String coreString = ModisDaacUtils.extractCoreString(hdfGlobalAttributes);

        assertEquals("firstPartsecondPart", coreString);
    }
}
