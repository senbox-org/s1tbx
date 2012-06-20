package org.esa.beam.dataio.modis;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModisProductReaderTest {

    @Test
    public void testIsGlobalAttributeName() {
         assertTrue(ModisProductReader.isGlobalAttributeName("StructMetadata.0"));
         assertTrue(ModisProductReader.isGlobalAttributeName("CoreMetadata.0"));
         assertTrue(ModisProductReader.isGlobalAttributeName("ArchiveMetadata.0"));

         assertFalse(ModisProductReader.isGlobalAttributeName("EV_250_Aggr1km_RefSB"));
         assertFalse(ModisProductReader.isGlobalAttributeName("property_thingy"));
         assertFalse(ModisProductReader.isGlobalAttributeName(""));
         assertFalse(ModisProductReader.isGlobalAttributeName(null));
    }
}
