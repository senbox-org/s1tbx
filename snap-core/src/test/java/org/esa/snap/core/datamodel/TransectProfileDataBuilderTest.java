/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.snap.core.dataio.AbstractProductReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class TransectProfileDataBuilderTest {

    private TransectProfileDataBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new TransectProfileDataBuilder();
    }

    @Test
    public void testBuildDefault() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Shape dummyPath = createDummyPath();
        builder.raster(dummyBand);
        builder.path(dummyPath);

        TransectProfileData data = builder.build();

        assertEquals(1, data.config.boxSize);
        assertSame(dummyBand, data.config.raster);
        assertSame(dummyPath, data.config.path);
        assertEquals(true, data.config.connectVertices);
        assertEquals(null, data.config.roiMask);
        assertEquals(false, data.config.useRoiMask);
    }

    @Test
    public void testBuildNonDefaultWithVDN() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Mask dummyMask = createDummyMask();

        builder.raster(dummyBand);
        builder.pointData(createDummyVDN());
        builder.boxSize(5);
        builder.roiMask(dummyMask);
        builder.useRoiMask(true);
        builder.connectVertices(false);

        TransectProfileData data = builder.build();

        assertSame(dummyBand, data.config.raster);
        assertNotNull(data.config.path);
        assertEquals(5, data.config.boxSize);
        assertSame(dummyMask, data.config.roiMask);
        assertEquals(true, data.config.useRoiMask);
        assertEquals(false, data.config.connectVertices);
    }

    @Test
    public void testBuildNonDefaultWithPath() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Mask dummyMask = createDummyMask();

        builder.raster(dummyBand);
        builder.path(createDummyPath());
        builder.boxSize(13);
        builder.roiMask(dummyMask);
        builder.useRoiMask(true);
        builder.connectVertices(false);

        TransectProfileData data = builder.build();

        assertSame(dummyBand, data.config.raster);
        assertNotNull(data.config.path);
        assertEquals(13, data.config.boxSize);
        assertSame(dummyMask, data.config.roiMask);
        assertEquals(true, data.config.useRoiMask);
        assertEquals(false, data.config.connectVertices);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailForMissingRaster() throws Exception {
        builder.path(createDummyPath());
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testFailForMissingPath() throws Exception {
        builder.raster(createDummyBandWithProduct());
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailForMissingProduct() throws Exception {
        builder.raster(createDummyBandWithoutProduct());
        builder.build();
    }

    private Mask createDummyMask() {
        Mask mask = new Product("dummy", "type", 10, 10).addMask("maskName", Mask.BandMathsType.INSTANCE);
        mask.getImageConfig().setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, "Y <= 1.5");
        return mask;
    }

    private VectorDataNode createDummyVDN() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("typeName");
        return new VectorDataNode("name", typeBuilder.buildFeatureType());
    }

    private RasterDataNode createDummyBandWithoutProduct() {
        return new Band("name", ProductData.TYPE_FLOAT32, 10, 10);
    }

    private RasterDataNode createDummyBandWithProduct() {
        final Product product = new Product("pname", "type", 10, 10);
        product.setProductReader(new AbstractProductReader(null) {
            @Override
            protected Product readProductNodesImpl() throws IOException {
                return product;
            }

            @Override
            protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
            }
        });
        return product.addBand("name", ProductData.TYPE_FLOAT32);
    }

    private Shape createDummyPath() {
        return new Rectangle();
    }
}
