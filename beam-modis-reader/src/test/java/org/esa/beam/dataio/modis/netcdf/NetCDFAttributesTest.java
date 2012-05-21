package org.esa.beam.dataio.modis.netcdf;

import junit.framework.TestCase;
import ucar.nc2.Attribute;

import java.util.ArrayList;
import java.util.List;

public class NetCDFAttributesTest extends TestCase {

    private NetCDFAttributes netCDFAttributes;

    public void testAddList_emptyList() {
        final List<Attribute> globalAttributes = new ArrayList<Attribute>();
        netCDFAttributes.add(globalAttributes);

        assertNull(netCDFAttributes.get("egal_was"));
    }

    public void testAddList_nullList() {
        netCDFAttributes.add(null);

        assertNull(netCDFAttributes.get("egal_was"));
    }

    public void testAddList() {
        final List<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute("a_attrib", 12));
        globalAttributes.add(new Attribute("b_attrib", 13));
        globalAttributes.add(new Attribute("c_attrib", 14));
        netCDFAttributes.add(globalAttributes);


        Attribute attrib = netCDFAttributes.get("a_attrib");
        assertNotNull(attrib);
        assertEquals(12, attrib.getNumericValue().intValue());

        attrib = netCDFAttributes.get("c_attrib");
        assertNotNull(attrib);
        assertEquals(14, attrib.getNumericValue().intValue());

        attrib = netCDFAttributes.get("egal_was");
        assertNull(attrib);
    }

    public void testGetAll() {
        final List<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute("one_attrib", 1));
        globalAttributes.add(new Attribute("two_attrib", 2));
        netCDFAttributes.add(globalAttributes);

        final Attribute[] result = netCDFAttributes.getAll();
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(1, result[0].getNumericValue());
    }

    protected void setUp() throws Exception {
        netCDFAttributes = new NetCDFAttributes();
    }
}
