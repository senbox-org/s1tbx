package org.esa.beam.visat.toolviews.spectrum;

import static org.junit.Assert.*;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

public class SpectraDiagramTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSomething() {
        // preparation
        final List<Band> bands = new ArrayList<Band>();

        final Product product1 = new Product("P1", "T1", 10, 10);
        product1.setAutoGrouping("_g_");
        bands.add(product1.addBand("Band_g_1", ProductData.TYPE_FLOAT32)); // grouped
        bands.add(product1.addBand("Band_g_2", ProductData.TYPE_FLOAT32)); // grouped
        bands.add(product1.addBand("Band_u_1", ProductData.TYPE_FLOAT32)); // not grouped
        bands.add(product1.addBand("Band_u_2", ProductData.TYPE_FLOAT32)); // not grouped

        final Product product2 = new Product("P2", "T2", 10, 10);
        product2.setAutoGrouping("_g1_:_g2_");
        bands.add(product2.addBand("Band_g1_1", ProductData.TYPE_FLOAT32)); // group 1
        bands.add(product2.addBand("Band_g1_2", ProductData.TYPE_FLOAT32)); // group 1
        bands.add(product2.addBand("Band_g2_1", ProductData.TYPE_FLOAT32)); // group 2
        bands.add(product2.addBand("Band_g2_2", ProductData.TYPE_FLOAT32)); // group 2

        // execution
        final Band[][] spectra = SpectraDiagram.extractSpectra(bands.toArray(new Band[bands.size()]));

        // verification
        assertEquals(4, spectra.length);
        assertEquals(2, spectra[0].length);
        assertEquals(2, spectra[1].length);
        assertEquals(2, spectra[2].length);
        assertEquals(2, spectra[3].length);

        assertEquals("Band_u_1",spectra[0][0].getName()); // not grouped
        assertEquals("Band_u_2",spectra[0][1].getName()); // not grouped
        assertEquals("Band_g1_1",spectra[1][0].getName()); // group 1
        assertEquals("Band_g1_2",spectra[1][1].getName()); // group 1
        assertEquals("Band_g2_1",spectra[2][0].getName()); // group 2
        assertEquals("Band_g2_2",spectra[2][1].getName()); // group 2
        assertEquals("Band_g_1",spectra[3][0].getName()); // grouped
        assertEquals("Band_g_2",spectra[3][1].getName()); // grouped
    }
}
