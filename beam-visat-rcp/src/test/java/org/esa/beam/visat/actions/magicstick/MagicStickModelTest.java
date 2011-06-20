package org.esa.beam.visat.actions.magicstick;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class MagicStickModelTest {

    private Product product;
    private Band b1;
    private Band b2;
    private Band b3;

    @Before
    public void setUp() throws Exception {
        product = new Product("product", "t", 16, 16);
        product.addBand("a1", ProductData.TYPE_FLOAT32);
        b1 = product.addBand("b1", ProductData.TYPE_FLOAT32);
        product.addBand("a2", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("b2", ProductData.TYPE_FLOAT32);
        product.addBand("a3", ProductData.TYPE_FLOAT32);
        b3 = product.addBand("b3", ProductData.TYPE_FLOAT32);
        b1.setSpectralWavelength(100);
        b2.setSpectralWavelength(200);
        b3.setSpectralWavelength(300);
    }

    @Test
    public void testConstructorSetsDefaultValues() throws Exception {
        MagicStickModel model = new MagicStickModel();
        assertEquals(MagicStickModel.Mode.SINGLE, model.getMode());
        assertEquals(0.1, model.getTolerance(), 0.0);
        assertEquals("0", model.createExpression(new Band[0]));
    }

    @Test
    public void testGetSpectralBands() throws Exception {
        Band[] bands = MagicStickModel.getSpectralBands(product);
        assertEquals(3, bands.length);
        assertSame(bands[0], b1);
        assertSame(bands[1], b2);
        assertSame(bands[2], b3);
    }

    @Test
    public void testCreateExpressionWith3Bands() throws Exception {
        MagicStickModel model = new MagicStickModel();
        model.addSpectrum(new double[]{0.4, 0.3, 0.2});
        model.setTolerance(0.3);
        String expression = model.createExpression(new Band[]{b1, b2, b3});
        assertEquals("distance(b1,b2,b3,0.4,0.3,0.2)/3 < 0.3", expression);
    }

    @Test
    public void testCreateExpressionWith3Bands2PlusSpectra() throws Exception {
        MagicStickModel model = new MagicStickModel();
        model.setMode(MagicStickModel.Mode.PLUS);
        model.addSpectrum(new double[]{0.4, 0.3, 0.2});
        model.addSpectrum(new double[]{0.6, 0.9, 0.7});
        model.setTolerance(0.25);
        String expression = model.createExpression(new Band[]{b1, b2, b3});
        assertEquals("distance(b1,b2,b3,0.4,0.3,0.2)/3 < 0.25" +
                " || distance(b1,b2,b3,0.6,0.9,0.7)/3 < 0.25", expression);
    }

    @Test
    public void testCreateExpressionWith1Band() throws Exception {
        MagicStickModel model = new MagicStickModel();
        model.addSpectrum(new double[]{0.2});
        model.setTolerance(0.25);
        model.setTolerance(0.05);
        String expression = model.createExpression(new Band[]{product.getBand("a2")});
        assertEquals("distance(a2,0.2) < 0.05", expression);
    }
}
