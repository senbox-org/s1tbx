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

import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class MosaicOpTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;


    private static Product product1;
    private static Product product2;
    private static Product product3;

    @BeforeClass
    public static void setup() throws FactoryException, TransformException {
        product1 = createProduct("P1", 0, 0, 2.0f);
        product2 = createProduct("P2", 4, -4, 3.0f);
        product3 = createProduct("P3", -5, 5, 5.0f);
    }

    @AfterClass
    public static void teardown() {
        product1.dispose();
        product2.dispose();
        product3.dispose();
    }

    @Test
    public void testMosaickingSimple() throws IOException {
        final MosaicOp op = new MosaicOp();
        op.setParameterDefaultValues();

        op.setSourceProducts(product1, product2, product3);
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),

        };
        op.westBound = -10.0;
        op.northBound = 10.0;
        op.eastBound = 10.0;
        op.southBound = -10.0;
        op.pixelSizeX = 1.0;
        op.pixelSizeY = 1.0;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.333333f, 2.5f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 3, 2});
    }

    @Test
    public void testMosaicking_Mask() throws IOException {
        final MosaicOp op = new MosaicOp();
        op.setParameterDefaultValues();

        op.setSourceProducts(product1, product2, product3);
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),
                new MosaicOp.Variable("myMask", "mask1"),

        };
        op.westBound = -10.0;
        op.northBound = 10.0;
        op.eastBound = 10.0;
        op.southBound = -10.0;
        op.pixelSizeX = 1.0;
        op.pixelSizeY = 1.0;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.333333f, 2.5f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 3, 2});
    }

    @Test
    public void testMosaickingWithConditions() {
        final MosaicOp op = new MosaicOp();
        op.setParameterDefaultValues();
        op.setSourceProducts(product1, product2, product3);
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1")
        };
        op.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };
        op.westBound = -10.0;
        op.northBound = 10.0;
        op.eastBound = 10.0;
        op.southBound = -10.0;
        op.pixelSizeX = 1.0;
        op.pixelSizeY = 1.0;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.5f, 2.0f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 2, 1});

        Band condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 2, 2, 1});
    }

    @Test
    public void testMosaickingWithInvalidSourceSamples() throws IOException {
        final Product product1Copy = ProductSubsetBuilder.createProductSubset(product1, null, "P1", "Descr");
        final Band flagBand = product1Copy.addBand("flag", ProductData.TYPE_INT32);
        final BufferedImage flagImage = new BufferedImage(WIDTH, HEIGHT, DataBuffer.TYPE_INT);
        int[] flagData = new int[WIDTH * HEIGHT];
        Arrays.fill(flagData, 1);
        Arrays.fill(flagData, 0, 3 * WIDTH, 0);
        flagImage.getRaster().setDataElements(0, 0, WIDTH, HEIGHT, flagData);
        flagBand.setSourceImage(flagImage);
        product1Copy.getBand("b1").setValidPixelExpression("flag == 1");

        final MosaicOp op = new MosaicOp();
        op.setParameterDefaultValues();
        op.setSourceProducts(product1Copy, product2, product3);
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1")
        };
        op.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };
        op.westBound = -10.0;
        op.northBound = 10.0;
        op.eastBound = 10.0;
        op.southBound = -10.0;
        op.pixelSizeX = 1.0;
        op.pixelSizeY = 1.0;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 5.0f, 3.5f, 2.0f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 1, 2, 1});

        Band condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 1, 2, 1});
    }

    @Test
    public void testMosaickingUpdate() throws IOException {
        final MosaicOp mosaicOp = new MosaicOp();
        mosaicOp.setParameterDefaultValues();
        mosaicOp.setSourceProducts(product1, product2);
        mosaicOp.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),
        };
        mosaicOp.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };

        mosaicOp.westBound = -10.0;
        mosaicOp.northBound = 10.0;
        mosaicOp.eastBound = 10.0;
        mosaicOp.southBound = -10.0;
        mosaicOp.pixelSizeX = 1.0;
        mosaicOp.pixelSizeY = 1.0;

        final Product mosaicProduct = mosaicOp.getTargetProduct();

        final MetadataElement mosaicMetadata = mosaicProduct.getMetadataRoot().getElement("Processing_Graph");
        assertNotNull(mosaicMetadata);
        final MetadataElement mosaicSourcesElement = getSourcesElement(mosaicOp, mosaicMetadata);
        assertEquals(2, mosaicSourcesElement.getNumAttributes());
        for (int i = 0; i < mosaicSourcesElement.getAttributes().length; i++) {
            MetadataAttribute attribute = mosaicSourcesElement.getAttributes()[i];
            assertEquals("sourceProduct." + (1 + i), attribute.getName());
        }

        Band b1Band;
        Band countBand;
        Band condBand;

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        b1Band = mosaicProduct.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 0.0f, 2.0f, 2.0f, 2.0f});

        countBand = mosaicProduct.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 0, 1, 1, 1});

        condBand = mosaicProduct.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 0, 1, 1, 1});


        final MosaicOp mosaicUpdateOp = new MosaicOp();
        mosaicUpdateOp.setParameterDefaultValues();
        mosaicUpdateOp.setSourceProducts(product3);
        mosaicUpdateOp.updateProduct = mosaicOp.getTargetProduct();

        final Product product = mosaicUpdateOp.getTargetProduct();
        final MetadataElement updateMetadata = product.getMetadataRoot().getElement("Processing_Graph");
        assertNotNull(updateMetadata);
        final MetadataElement updateSourcesElement = getSourcesElement(mosaicUpdateOp, updateMetadata);
        assertEquals(3, updateSourcesElement.getNumAttributes());
        for (int i = 0; i < updateSourcesElement.getAttributes().length; i++) {
            MetadataAttribute attribute = updateSourcesElement.getAttributes()[i];
            assertEquals("sourceProduct." + (1 + i), attribute.getName());
        }

        b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.5f, 2.0f});

        countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 2, 1});

        condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 2, 2, 1});

    }

    private MetadataElement getSourcesElement(MosaicOp mosaicUpdateOp, MetadataElement mosaicMetadata) {
        for (MetadataElement element : mosaicMetadata.getElements()) {
            if (mosaicUpdateOp.getSpi().getOperatorAlias().equals(element.getAttributeString("operator"))) {
                return element.getElement("sources");
            }
        }
        return null;
    }

    private void assertSampleValuesFloat(Band b1Band, GeoPos[] geoPositions, float[] expectedValues) {
        GeoCoding geoCoding = b1Band.getGeoCoding();
        final Raster b1Raster = b1Band.getSourceImage().getData();
        for (int i = 0; i < geoPositions.length; i++) {
            PixelPos pp = geoCoding.getPixelPos(geoPositions[i], null);
            final float expectedValue = expectedValues[i];
            final float actualValue = b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0);
            final String message = String.format("At <%d>:", i);
            assertEquals(message, expectedValue, actualValue, 1.0e-6);
        }
    }

    private void assertSampleValuesInt(Band b1Band, GeoPos[] geoPositions, int[] expectedValues) {
        GeoCoding geoCoding = b1Band.getGeoCoding();
        final Raster b1Raster = b1Band.getSourceImage().getData();
        for (int i = 0; i < geoPositions.length; i++) {
            PixelPos pp = geoCoding.getPixelPos(geoPositions[i], null);
            final int expectedValue = expectedValues[i];
            final int actualValue = b1Raster.getSample((int) pp.x, (int) pp.y, 0);
            final String message = String.format("At <%d>:", i);
            assertEquals(message, expectedValue, actualValue);
        }
    }

    private static Product createProduct(final String name, final int easting, final int northing,
                                         final float bandFillValue) throws FactoryException, TransformException {
        final Product product = new Product(name, "T", WIDTH, HEIGHT);
        product.addBand(createBand(bandFillValue));
        product.addMask("mask1", "X % 2 == 0", "description", Color.RED, 0.5);
        final AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(1, -1);
        transform.translate(-0.5, -0.5);
        product.setSceneGeoCoding(
                new CrsGeoCoding(CRS.decode("EPSG:4326", true), new Rectangle(0, 0, WIDTH, HEIGHT), transform));
        return product;
    }

    private static Band createBand(float fillValue) {
        final Band band = new Band("b1", ProductData.TYPE_FLOAT32, WIDTH, HEIGHT);
        band.setSourceImage(ConstantDescriptor.create((float) WIDTH, (float) HEIGHT, new Float[]{fillValue}, null));
        return band;
    }


}
