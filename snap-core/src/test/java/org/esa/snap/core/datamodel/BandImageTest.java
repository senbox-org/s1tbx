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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import junit.framework.TestCase;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class BandImageTest extends TestCase {
    private static final int SIZE = 1000;
    private static final AffineTransform I2M = new AffineTransform(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);

    public void testThatMultiLevelImagesAreConsistent() throws IOException {
        testThat(new ProductFactory(false));
        testThat(new ProductFactory(true));
    }

    private void testThat(ProductReader reader) throws IOException {
        Product product = reader.readProductNodes(null, null);
        testThat(product.getBand("B1").getSourceImage(), product.getBand("B2").getSourceImage());
        testThat(product.getBand("B1").getGeophysicalImage(), product.getBand("B2").getGeophysicalImage());
    }

    private void testThat(MultiLevelImage image1, MultiLevelImage image2) {
        assertNotNull(image1);
        assertNotNull(image2);
        MultiLevelModel model1 = image1.getModel();
        MultiLevelModel model2 = image2.getModel();
        assertEquals(model1.getLevelCount(), model2.getLevelCount());
        assertEquals(model1.getModelBounds(), model2.getModelBounds());
        assertEquals(model1.getImageToModelTransform(0), model2.getImageToModelTransform(0));
        if (model1.getLevelCount() > 1) {
            assertEquals(model1.getImageToModelTransform(1), model2.getImageToModelTransform(1));
        }
        if (model1.getLevelCount() > 2) {
            assertEquals(model1.getImageToModelTransform(2), model2.getImageToModelTransform(2));
        }
    }

    private static class ProductFactory extends AbstractProductReader {
        private final boolean projected;

        public ProductFactory(boolean projected) {
            super(null);
            this.projected = projected;
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            Product product = new Product("A", "B", SIZE, SIZE, this);
            if (projected) {
                setMapGeoCoding(product);
            }

            // (1) A "normal" band, whose source image is implicitly created by framework
            product.addBand("B1", ProductData.TYPE_FLOAT32);

            // (2) A band whose source image is set explicitly
            Band b2 = product.addBand("B2", ProductData.TYPE_UINT8);
            b2.setSourceImage(new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY));
            b2.setScalingFactor(0.05);

            return product;
        }

        private void setMapGeoCoding(Product product) {
            try {
                GeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                       new Rectangle(0, 0, SIZE, SIZE),
                                                       new AffineTransform(I2M));
                product.setSceneGeoCoding(geoCoding);
            } catch (FactoryException e) {
                fail(e.getMessage());
            } catch (TransformException e) {
                fail(e.getMessage());
            }
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
            // ignored
        }
    }
}
