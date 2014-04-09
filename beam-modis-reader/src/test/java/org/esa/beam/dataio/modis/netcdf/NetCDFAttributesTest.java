package org.esa.beam.dataio.modis.netcdf;

import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class NetCDFAttributesTest {

    private NetCDFAttributes netCDFAttributes;

    @Test
    public void testAddList_emptyList() {
        final List<Attribute> globalAttributes = new ArrayList<>();
        netCDFAttributes.add(globalAttributes);

        assertNull(netCDFAttributes.get("egal_was"));
    }

    @Test
    public void testAddList_nullList() {
        netCDFAttributes.add(null);

        assertNull(netCDFAttributes.get("egal_was"));
    }

    @Test
    public void testAddList() {
        final List<Attribute> globalAttributes = new ArrayList<>();
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

    @Test
    public void testGetAll() {
        final List<Attribute> globalAttributes = new ArrayList<>();
        globalAttributes.add(new Attribute("one_attrib", 1));
        globalAttributes.add(new Attribute("two_attrib", 2));
        netCDFAttributes.add(globalAttributes);

        final Attribute[] result = netCDFAttributes.getAll();
        assertNotNull(result);
        assertEquals(2, result.length);
        List<Attribute> resultList = Arrays.asList(result);
        resultList.contains(new Attribute("one_attrib", 1));
        resultList.contains(new Attribute("two_attrib", 2));
    }

    @Before
    public void setUp() throws Exception {
        netCDFAttributes = new NetCDFAttributes();
    }
}
