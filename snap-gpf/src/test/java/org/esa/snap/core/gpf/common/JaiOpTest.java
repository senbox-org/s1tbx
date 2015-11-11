/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.common;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

public class JaiOpTest extends TestCase {
    public void testNoSourceGiven() {
        final JaiOp op = new JaiOp();

        try {
            op.getTargetProduct();
            fail("OperatorException expected");
        } catch (OperatorException e) {
            // ok
        }
    }

    public void testNoJaiOpGiven() {

        final Product sourceProduct = createSourceProduct();

        final JaiOp op = new JaiOp();
        op.setSourceProduct(sourceProduct);

        try {
            op.getTargetProduct();
            fail("OperatorException expected");
        } catch (OperatorException e) {
            // ok
        }
    }

    // uses JAI "scale" to create a higher resolution version of a product
    public void testGeometricOperation() {

        final Product sourceProduct = createSourceProduct();
        final Band sourceBand = sourceProduct.getBand("b1");
        setSourceImage(sourceBand);

        final JaiOp op = new JaiOp();

        op.setOperationName("scale");
        op.setSourceProduct(sourceProduct);

        final HashMap<String, Object> operationParameters = new HashMap<String, Object>(3);
        operationParameters.put("xScale", 2.0f);
        operationParameters.put("yScale", 2.0f);
        op.setOperationParameters(operationParameters);

        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(2, targetProduct.getNumBands());
        assertEquals(8, targetProduct.getSceneRasterWidth());
        assertEquals(8, targetProduct.getSceneRasterHeight());

        final Band targetBand = targetProduct.getBand("b1");

        assertNotNull(targetBand);
        assertEquals(targetBand.getDataType(), sourceBand.getDataType());
        assertEquals(8, targetBand.getRasterWidth());
        assertEquals(8, targetBand.getRasterHeight());

        final RenderedImage targetImage = targetBand.getSourceImage();
        assertNotNull(targetImage);
        assertEquals(8, targetImage.getWidth());
        assertEquals(8, targetImage.getHeight());

        final Tile tile = op.getSourceTile(targetBand, new Rectangle(0, 0, 8, 8));
        assertEquals(123, tile.getSampleInt(0, 0));
        assertEquals(123, tile.getSampleInt(1, 1));
        assertEquals(234, tile.getSampleInt(2, 2));
        assertEquals(234, tile.getSampleInt(3, 3));
        assertEquals(345, tile.getSampleInt(4, 4));
        assertEquals(345, tile.getSampleInt(5, 5));
        assertEquals(456, tile.getSampleInt(6, 6));
        assertEquals(456, tile.getSampleInt(7, 7));
    }

    // uses JAI "rescale" to apply a linear transformation to sample value
    public void testSampleOperation() {

        final Product sourceProduct = createSourceProduct();
        final Band sourceBand = sourceProduct.getBand("b1");
        setSourceImage(sourceBand);

        final JaiOp op = new JaiOp();
        op.setOperationName("rescale");
        op.setSourceProduct(sourceProduct);

        final HashMap<String, Object> operationParameters = new HashMap<String, Object>(3);
        operationParameters.put("constants", new double[]{2.0});
        operationParameters.put("offsets", new double[]{1.0});
        op.setOperationParameters(operationParameters);

        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(2, targetProduct.getNumBands());
        assertEquals(4, targetProduct.getSceneRasterWidth());
        assertEquals(4, targetProduct.getSceneRasterHeight());

        final Band targetBand = targetProduct.getBand("b1");

        assertNotNull(targetBand);
        assertEquals(targetBand.getDataType(), sourceBand.getDataType());
        assertEquals(4, targetBand.getRasterWidth());
        assertEquals(4, targetBand.getRasterHeight());

        final RenderedImage targetImage = targetBand.getSourceImage();
        assertNotNull(targetImage);
        assertEquals(4, targetImage.getWidth());
        assertEquals(4, targetImage.getHeight());

        final Tile tile = op.getSourceTile(targetBand, new Rectangle(0, 0, 4, 4));
        assertEquals(1 + 2 * 123, tile.getSampleInt(0, 0));
        assertEquals(1 + 2 * 234, tile.getSampleInt(1, 1));
        assertEquals(1 + 2 * 345, tile.getSampleInt(2, 2));
        assertEquals(1 + 2 * 456, tile.getSampleInt(3, 3));
    }

    private void setSourceImage(Band sourceBand) {
        final TiledImage sourceImage = new TiledImage(0, 0,
                                                      sourceBand.getRasterWidth(),
                                                      sourceBand.getRasterHeight(),
                                                      0, 0,
                                                      new SingleBandedSampleModel(DataBuffer.TYPE_INT, sourceBand.getRasterWidth(), sourceBand.getRasterHeight()), null);
        sourceImage.setSample(0, 0, 0, 123);
        sourceImage.setSample(1, 1, 0, 234);
        sourceImage.setSample(2, 2, 0, 345);
        sourceImage.setSample(3, 3, 0, 456);
        sourceBand.setSourceImage(sourceImage);
    }

    private Product createSourceProduct() {
        final Product sourceProduct = new Product("sp", "spt", 4, 4);
        sourceProduct.addBand("b1", ProductData.TYPE_INT32);
        sourceProduct.addTiePointGrid(new TiePointGrid("tpg1", 3, 3, 0, 0, 2, 2, new float[]{
                0.1f, 0.2f, 0.3f,
                0.2f, 0.3f, 0.4f,
                0.3f, 0.4f, 0.5f,
        }));
        return sourceProduct;
    }


    /**
     * Not actually a unit-test, its just a code snippet allowing
     * to explore API specification and object state of a JAI RenderedOp.
     */
    public void testJaiOperationIntrospection() {
        final BufferedImage sourceImage = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);

        final ParameterBlockJAI params = new ParameterBlockJAI("scale");
        params.setParameter("xScale", 2.0f);
        params.setParameter("yScale", 3.0f);
        params.addSource(sourceImage);
        final RenderedOp op = JAI.create("scale", params);
        assertEquals("scale", op.getOperationName());

        final ParameterBlock parameterBlock = op.getParameterBlock();
        assertNotNull(parameterBlock);
        assertNotSame(params, parameterBlock);
        assertTrue(parameterBlock instanceof ParameterBlockJAI);
        ParameterBlockJAI parameterBlockJAI = (ParameterBlockJAI) op.getParameterBlock();

        final OperationDescriptor operationDescriptor = parameterBlockJAI.getOperationDescriptor();
        assertNotNull(operationDescriptor);
        assertEquals("Scale", operationDescriptor.getName());

        final ParameterListDescriptor parameterListDescriptor = operationDescriptor.getParameterListDescriptor("rendered");
        assertNotNull(parameterListDescriptor);
        assertEquals(5, parameterListDescriptor.getNumParameters());

        final String[] paramNames = parameterListDescriptor.getParamNames();
        assertNotNull(paramNames);
        assertEquals(5, paramNames.length);
        assertEquals("xScale", paramNames[0]);
        assertEquals("yScale", paramNames[1]);
        assertEquals("xTrans", paramNames[2]);
        assertEquals("yTrans", paramNames[3]);
        assertEquals("interpolation", paramNames[4]);

        final Class[] paramClasses = parameterListDescriptor.getParamClasses();
        assertNotNull(paramClasses);
        assertEquals(5, paramClasses.length);
        assertEquals(Float.class, paramClasses[0]);
        assertEquals(Float.class, paramClasses[1]);
        assertEquals(Float.class, paramClasses[2]);
        assertEquals(Float.class, paramClasses[3]);
        assertEquals(Interpolation.class, paramClasses[4]);

        final Object[] paramDefaults = parameterListDescriptor.getParamDefaults();
        assertNotNull(paramDefaults);
        assertEquals(5, paramDefaults.length);
        assertEquals(1.0f, paramDefaults[0]);
        assertEquals(1.0f, paramDefaults[1]);
        assertEquals(0.0f, paramDefaults[2]);
        assertEquals(0.0f, paramDefaults[3]);
        assertEquals(Interpolation.getInstance(Interpolation.INTERP_NEAREST), paramDefaults[4]);

    }


    /**
     * Not actually a unit-test, its just a code snippet allowing
     * to explore API specification and object state of a JAI RenderedOp.
     */
    public void testJaiOperationParameterChange() {
        final BufferedImage sourceImage = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);

        ParameterBlockJAI params = new ParameterBlockJAI("scale");
        params.setParameter("xScale", 2.0f);
        params.setParameter("yScale", 3.0f);
        params.addSource(sourceImage);
        final RenderedOp renderedOp = JAI.create("scale", params);

        final MockPCL mockPCL = new MockPCL();
        renderedOp.addPropertyChangeListener(mockPCL);

        final PlanarImage rendering = renderedOp.getRendering();
        assertSame(rendering, renderedOp.getRendering());
        assertTrue(rendering instanceof OpImage);

        final PlanarImage instance = renderedOp.createInstance();
        assertNotSame(instance, renderedOp);
        assertNotSame(instance, rendering);
        assertNotSame(instance, renderedOp.createInstance());

        assertEquals("", mockPCL.trace);

        params = new ParameterBlockJAI("scale");
        params.setParameter("xScale", 0.5f);
        params.setParameter("yScale", 3.3f);
        params.addSource(sourceImage);
        renderedOp.setParameterBlock(params);

        assertEquals("parameters;rendering;", mockPCL.trace);

        assertNotSame(rendering, renderedOp.getRendering());
        assertSame(renderedOp.getRendering(), renderedOp.getRendering());


    }

    /**
     * Not actually a unit-test, its just a code snippet allowing
     * to explore API specification and object state of a JAI RenderedOp.
     */
    public void testJaiOperationParameterChangeInDAG() {
        final BufferedImage sourceImage = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);

        ParameterBlockJAI params = new ParameterBlockJAI("scale");
        params.setParameter("xScale", 2.0f);
        params.setParameter("yScale", 3.0f);
        params.addSource(sourceImage);
        final RenderedOp renderedOp1 = JAI.create("scale", params);

        params = new ParameterBlockJAI("rescale");
        params.setParameter("offsets", new double[]{0.5, 0.5, 0.5, 0.5});
        params.setParameter("constants", new double[]{1.5, 1.5, 1.5, 1.5});
        params.addSource(renderedOp1);
        final RenderedOp renderedOp2 = JAI.create("rescale", params);

        final MockPCL mockPCL = new MockPCL();
        renderedOp2.addPropertyChangeListener(mockPCL);

        final PlanarImage rendering = renderedOp2.getRendering();
        assertSame(rendering, renderedOp2.getRendering());
        assertTrue(rendering instanceof OpImage);

        params = new ParameterBlockJAI("scale");
        params.setParameter("xScale", 0.5f);
        params.setParameter("yScale", 3.3f);
        params.addSource(sourceImage);
        renderedOp1.setParameterBlock(params);

        assertEquals("rendering;", mockPCL.trace);
        assertNotSame(mockPCL.lastNewValue, mockPCL.lastOldValue);
        assertEquals(rendering, mockPCL.lastOldValue);
        assertEquals(renderedOp2.getRendering(), mockPCL.lastNewValue);
    }

    private static class MockPCL implements PropertyChangeListener {
        String trace = "";
        Object lastOldValue;
        Object lastNewValue;

        public void propertyChange(PropertyChangeEvent evt) {
            trace += evt.getPropertyName() + ";";
            lastOldValue = evt.getOldValue();
            lastNewValue = evt.getNewValue();
        }
    }
}
