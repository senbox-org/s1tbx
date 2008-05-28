package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.ProductIOException;

public class ModisDaacUtilsTest_extractProductType extends TestCase {

    public void testOk_MOD13A2_InTheMiddle() throws ProductIOException {
        final String typeString = ModisDaacUtils.extractProductType("MOD_SS.MOD13A2.somthing other");

        assertEquals("MOD13A2", typeString);
    }

    public void testOk_MYD13A2_AtStart() throws ProductIOException {
        final String typeString = ModisDaacUtils.extractProductType("MYD13A2.somthing other");

        assertEquals("MYD13A2", typeString);
    }

    public void testAllProductTypes_InTheMiddle() throws ProductIOException {
        final String[] supportetProductTypes = ModisProductDb.getInstance().getSupportetProductTypes();
        for (int i = 0; i < supportetProductTypes.length; i++) {
            final String type = supportetProductTypes[i];
            final String toTest = "anyPrefix." + type + ".anySuffix";
            assertEquals("Index = " + i, ModisDaacUtils.extractProductType(toTest), type);
        }
    }

    public void testAllProductTypes_AtStart() throws ProductIOException {
        final String[] supportetProductTypes = ModisProductDb.getInstance().getSupportetProductTypes();
        for (int i = 0; i < supportetProductTypes.length; i++) {
            final String type = supportetProductTypes[i];
            final String toTest = type + ".anySuffix";
            assertEquals("Index = " + i, ModisDaacUtils.extractProductType(toTest), type);
        }
    }
}