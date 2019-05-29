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
package org.esa.snap.core.gpf.experimental;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Dimension;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Requirements:
 * 1. Receive raster data for all bands for a requested tile size.
 * 2. Deterministic vs. Non-deterministic Op/Graph Execution
 * 3. Efficient computation of single pixels (may not be implemented here, but in MultiLevelImage)
 * 4. Immediate Execution
 * 5. Configurable with respect to e.g. parallelism, execution order
 */
@Ignore
public class OperatorExecutor2Test {

    @Test
    public void testWithFrameSizeEqualsTileSize() throws Exception {
        SimpleOp op = new SimpleOp();

        Product s = new Product("S", "S", 10, 10);
        s.setPreferredTileSize(10, 1);

        op.setSourceProduct(s);
        Product t = op.getTargetProduct();

        // Prerequisites
        assertEquals(s.getPreferredTileSize(), t.getPreferredTileSize());
        assertEquals(3 + 3 + 3, t.getNumBands());

        OperatorExecutor2 ex = new OperatorExecutor2(t);
        // Configure
        // ex.setXXX()

        assertEquals(t.getPreferredTileSize(), ex.getFrameSize());

        TracingHandler handler = new TracingHandler();
        Object metrics = ex.execute(handler);

        assertEquals("" +
                             "#bands=9, x=0, y=0, w=10, h=2\n" +
                             "#bands=9, x=0, y=2, w=10, h=2\n" +
                             "#bands=9, x=0, y=4, w=10, h=2\n" +
                             "#bands=9, x=0, y=6, w=10, h=2\n" +
                             "#bands=9, x=0, y=8, w=10, h=2\n",
                     handler.trace.toString());

        assertNotNull(metrics);
    }

    @Test
    public void testWithCustomFrameSize() throws Exception {
        SimpleOp op = new SimpleOp();

        Product s = new Product("S", "S", 10, 10);
        op.setSourceProduct(s);
        Product t = op.getTargetProduct();

        // Prerequisites
        assertEquals(3 + 3 + 3, t.getNumBands());

        OperatorExecutor2 ex = new OperatorExecutor2(t);
        ex.setFrameSize(2, 10);

        assertEquals(new Dimension(2, 10), ex.getFrameSize());

        TracingHandler handler = new TracingHandler();
        Object metrics = ex.execute(handler);

        assertEquals("" +
                             "#bands=9, x=0, y=0, w=2, h=10\n" +
                             "#bands=9, x=2, y=0, w=2, h=10\n" +
                             "#bands=9, x=4, y=0, w=2, h=10\n" +
                             "#bands=9, x=6, y=0, w=2, h=10\n" +
                             "#bands=9, x=8, y=0, w=2, h=10\n",
                     handler.trace.toString());

        assertNotNull(metrics);
    }

    @Test
    public void testWithCustomFrameSizeNotFittingSceneSize() throws Exception {
        SimpleOp op = new SimpleOp();

        Product s = new Product("S", "S", 10, 10);
        op.setSourceProduct(s);
        Product t = op.getTargetProduct();

        // Prerequisites
        assertEquals(3 + 3 + 3, t.getNumBands());

        OperatorExecutor2 ex = new OperatorExecutor2(t);
        ex.setFrameSize(7, 3);

        assertEquals(new Dimension(7, 3), ex.getFrameSize());

        TracingHandler handler = new TracingHandler();
        Object metrics = ex.execute(handler);

        assertEquals("" +
                             "#bands=9, x=0, y=0, w=7, h=3\n" +
                             "#bands=9, x=7, y=0, w=3, h=3\n" +
                             "#bands=9, x=0, y=3, w=7, h=3\n" +
                             "#bands=9, x=7, y=3, w=3, h=3\n" +
                             "#bands=9, x=0, y=6, w=7, h=3\n" +
                             "#bands=9, x=7, y=6, w=3, h=3\n" +
                             "#bands=9, x=0, y=9, w=7, h=1\n" +
                             "#bands=9, x=7, y=9, w=3, h=1\n",
                     handler.trace.toString());

        assertNotNull(metrics);
    }

    @Test
    public void testData() throws Exception {
        SimpleOp op = new SimpleOp();

        Product s = new Product("S", "S", 10, 10);
        s.setPreferredTileSize(10, 2);

        op.setSourceProduct(s);
        Product t = op.getTargetProduct();

        OperatorExecutor2 ex = new OperatorExecutor2(t);
        TracingHandler2 handler = new TracingHandler2();
        Object metrics = ex.execute(handler);
        assertEquals(5, handler.trace.size());

        OperatorExecutor2.Frame frame0 = handler.trace.get(0);

        assertNotNull(frame0.getData(0));

        assertEquals(0.1f, frame0.getData(0).getElemFloatAt(0), 0.0f);
        assertEquals(0.3f, frame0.getData(3).getElemFloatAt(0), 0.0f);
        assertEquals(0.1f - 0.3f, frame0.getData(6).getElemFloatAt(0), 0.0f);

        assertEquals(0.1f, frame0.getData(0).getElemFloatAt(7), 0.0f);
        assertEquals(0.3f, frame0.getData(3).getElemFloatAt(7), 0.0f);
        assertEquals(0.1f - 0.3f, frame0.getData(6).getElemFloatAt(7), 0.0f);

        OperatorExecutor2.Frame frame3 = handler.trace.get(3);

        assertEquals(0.1f, frame3.getData(0).getElemFloatAt(0), 0.0f);
        assertEquals(0.3f, frame3.getData(3).getElemFloatAt(0), 0.0f);
        assertEquals(0.1f - 0.3f, frame3.getData(6).getElemFloatAt(0), 0.0f);

        assertEquals(0.1f, frame3.getData(0).getElemFloatAt(7), 0.0f);
        assertEquals(0.3f, frame3.getData(3).getElemFloatAt(7), 0.0f);
        assertEquals(0.1f - 0.3f, frame3.getData(6).getElemFloatAt(7), 0.0f);

        assertNotNull(metrics);
    }

    private static class TracingHandler implements OperatorExecutor2.Handler {

        StringBuilder trace = new StringBuilder();

        @Override
        public void frameComputed(OperatorExecutor2.Frame frame) {

            trace.append(String.format("#bands=%d, x=%d, y=%d, w=%d, h=%d\n",
                                       frame.getNumBands(),
                                       frame.getRegion().x,
                                       frame.getRegion().y,
                                       frame.getRegion().width,
                                       frame.getRegion().height));
        }

    }

    private static class TracingHandler2 implements OperatorExecutor2.Handler {

        ArrayList<OperatorExecutor2.Frame> trace = new ArrayList<OperatorExecutor2.Frame>();

        @Override
        public void frameComputed(OperatorExecutor2.Frame frame) {
            assertFalse(trace.contains(frame));
            trace.add(frame);
        }

    }

    public static class SimpleOp extends PixelOperator {
        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);

            int w = productConfigurer.getTargetProduct().getSceneRasterWidth();
            int h = productConfigurer.getTargetProduct().getSceneRasterHeight();
            productConfigurer.addBand("a", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("b", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("c", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("x", ProductData.TYPE_FLOAT32).setSourceImage(
                    ConstantDescriptor.create(1f * w, 1f * h, new Float[]{0.3f}, null));
            productConfigurer.addBand("y", ProductData.TYPE_FLOAT32).setSourceImage(
                    ConstantDescriptor.create(1f * w, 1f * h, new Float[]{0.6f}, null));
            productConfigurer.addBand("z", ProductData.TYPE_FLOAT32).setSourceImage(
                    ConstantDescriptor.create(1f * w, 1f * h, new Float[]{0.9f}, null));
            productConfigurer.addBand("u", "a-x");
            productConfigurer.addBand("v", "b-y");
            productConfigurer.addBand("w", "c-z");
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(0, "a");
            sampleConfigurer.defineSample(1, "b");
            sampleConfigurer.defineSample(2, "c");
        }

        @Override
        protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

            final int n = 10000000;
            float u = 0, v = 0, w = 0;
            for (int i = 0; i < n; i++) {

                for (int j = 0; j < n; j++) {

                    for (int k = 0; k < n; k++) {

                        u = 0.1f * i;
                        v = 0.2f * j;
                        w = 0.4f * k;

                        u /= i;
                        v /= j;
                        w /= k;
                    }
                }
            }

            targetSamples[0].set(u);
            targetSamples[1].set(v);
            targetSamples[2].set(w);
        }

    }
}
