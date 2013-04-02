package org.esa.beam.dataio.modis.hdf;

import junit.framework.TestCase;

public class HdfDataFieldTest extends TestCase {

    private HdfDataField dataField;

    public void testConstruction() {
        assertEquals("", dataField.getName());
        assertEquals(0, dataField.getHeight());
        assertEquals(1, dataField.getLayers());
        assertEquals(0, dataField.getWidth());
        assertNull(dataField.getDimensionNames());
    }

    public void testSetGetName() {
        final String name_1 = "Harry";
        final String name_2 = "Sally";

        dataField.setName(name_1);
        assertEquals(name_1, dataField.getName());

        dataField.setName(name_2);
        assertEquals(name_2, dataField.getName());
    }

    public void testSetGetWidth() {
        final int width_1 = 34;
        final int width_2 = 109;

        dataField.setWidth(width_1);
        assertEquals(width_1, dataField.getWidth());

        dataField.setWidth(width_2);
        assertEquals(width_2, dataField.getWidth());
    }

    public void testSetGetHeight() {
        final int height_1 = 889;
        final int height_2 = 57;

        dataField.setHeight(height_1);
        assertEquals(height_1, dataField.getHeight());

        dataField.setHeight(height_2);
        assertEquals(height_2, dataField.getHeight());
    }

    public void testSetGetLayers() {
        final int layers_1 = 3;
        final int layers_2 = 9;

        dataField.setLayers(layers_1);
        assertEquals(layers_1, dataField.getLayers());

        dataField.setLayers(layers_2);
        assertEquals(layers_2, dataField.getLayers());
    }

    public void testSetGetDimensionNames() {
        final String[] dimensionNames = new String[] {"dim_1", "dim_2"};

        dataField.setDimensionNames(dimensionNames);
        final String[] result = dataField.getDimensionNames();
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("dim_2", result[1]);
    }

    @Override
    protected void setUp() throws Exception {
        dataField = new HdfDataField();
    }
}
