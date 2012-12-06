/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.radarsat2;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.util.TestUtils;

import javax.imageio.ImageReader;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRadarsat2ProductReader extends TestCase {

    private final static File tiffFile1 = new File(TestUtils.rootPathExpectedProducts+"largeFiles\\Tiff\\imagery_HV.tif");
    private final static File tiffFile2 = new File(TestUtils.rootPathExpectedProducts+"largeFiles\\Tiff\\srtm_39_04.tif");

    private Radarsat2ProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    private ImageIOFile imageIOFile1, imageIOFile2;
    private Product product1, product2;
    private ProductReader geoTiffReader;

    public TestRadarsat2ProductReader(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.initTestEnvironment();
        readerPlugin = new Radarsat2ProductReaderPlugIn();
        reader = readerPlugin.createReaderInstance();

        final ImageReader ioreader = ImageIOFile.getTiffIIOReader(tiffFile1);
        imageIOFile1 = new ImageIOFile(tiffFile1, ioreader);
        imageIOFile2 = new ImageIOFile(tiffFile2, ioreader);

        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns("GeoTIFF");
        ProductReaderPlugIn readerPlugin = (ProductReaderPlugIn)readerPlugIns.next();

        geoTiffReader = readerPlugin.createReaderInstance();
        product1 = geoTiffReader.readProductNodes(tiffFile1, null);
        product2 = geoTiffReader.readProductNodes(tiffFile2, null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        reader = null;
        readerPlugin = null;
    }

    /**
     * Open all files in a folder recursively
     * @throws Exception anything
     */
    public void testOpenAll() throws Exception
    {
        final File folder = new File(TestUtils.rootPathRadarsat2);
        if(!folder.exists()) return;

        if(TestUtils.canTestReadersOnAllProducts())
            TestUtils.recurseReadFolder(folder, readerPlugin, reader, null, null);
    }

    private final static int size = 6000;
    private final static int iterations = 20;

    public void testBeamGeoTiffReaderDouble() throws IOException {
        if(!tiffFile1.exists()) return;

        for(int i=0; i < iterations; ++i) {
            final ProductData destBuffer = new ProductData.Double(size*size);
            geoTiffReader.readBandRasterData(product1.getBandAt(0), 0,0, size, size, destBuffer, ProgressMonitor.NULL);
        }
    }

    public void testBeamGeoTiffReaderInt() throws IOException {
        if(!tiffFile2.exists()) return;

        for(int i=0; i < iterations; ++i) {
            final ProductData destBuffer = new ProductData.Int(size*size);
            geoTiffReader.readBandRasterData(product2.getBandAt(0), 0,0, size, size, destBuffer, ProgressMonitor.NULL);
        }
    }

    public void testImageIOTiffReaderDouble() throws IOException {
        if(!tiffFile1.exists()) return;

        for(int i=0; i < iterations; ++i) {
            final ProductData destBuffer = new ProductData.Double(size*size);
            imageIOFile1.readImageIORasterBand(0,0,1,1, destBuffer, 0,0, size, size, 0, 0);
        }
    }

    public void testImageIOTiffReaderInt() throws IOException {
        if(!tiffFile2.exists()) return;

        for(int i=0; i < iterations; ++i) {
            final ProductData destBuffer = new ProductData.Int(size*size);
            imageIOFile2.readImageIORasterBand(0,0,1,1, destBuffer, 0,0, size, size, 0, 0);
        }
    }
}