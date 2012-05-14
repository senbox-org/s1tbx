package org.esa.beam.dataio.modis.attribute;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;

public class DaacAttributesTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void testInheritance() {
        final DaacAttributes daacAttributes = new DaacAttributes(new NetCDFVariables());

        assertTrue(daacAttributes instanceof ModisGlobalAttributes);
    }

    public void testIsImappFormat() {
        final DaacAttributes daacAttributes = new DaacAttributes(new NetCDFVariables());

        assertFalse(daacAttributes.isImappFormat());
    }
}
