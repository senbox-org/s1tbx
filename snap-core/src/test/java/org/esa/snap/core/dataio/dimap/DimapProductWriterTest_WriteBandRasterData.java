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
package org.esa.snap.core.dataio.dimap;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.BeamConstants;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DimapProductWriterTest_WriteBandRasterData extends TestCase {

    private final DimapProductWriterPlugIn _writerPlugIn = new DimapProductWriterPlugIn();
    private DimapProductWriter _productWriter;
    private File _outputDir;
    private File _outputFile;

    public DimapProductWriterTest_WriteBandRasterData(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DimapProductWriterTest_WriteBandRasterData.class);
    }

    @Override
    protected void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _productWriter = (DimapProductWriter) _writerPlugIn.createWriterInstance();
        _outputDir = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "testproduct");
        _outputFile = new File(_outputDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
    }

    @Override
    protected void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    public void testWriteBandRasterData() throws IOException {
        int sceneWidth = 16;
        int sceneHeight = 12;
        int offsetX = 1;
        int offsetY = 1;
        int sourceWidth = sceneWidth - 2;
        int sourceHeight = sceneHeight - 2;
        Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
                                      sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        _productWriter.writeProductNodes(product, _outputFile);
        ProductData sourceBuffer = getFilledSourceData(sourceWidth * sourceHeight);
        _productWriter.writeBandRasterData(band, offsetX, offsetY, sourceWidth, sourceHeight, sourceBuffer,
                                           ProgressMonitor.NULL);
        _productWriter.close();

        byte[] expectedArray = prepareExpectedArrayInt8(sceneWidth, sceneHeight);
        byte[] currentArray = getCurrentByteArray(band);
        assertEquals(expectedArray.length, currentArray.length);

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], currentArray[i]);
        }
    }

    public void testWriteBandRasterData_SourceBuffer_toSmall() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
                                      sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            //Make the sourceBuffer to small
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight - 1);
            _productWriter.writeBandRasterData(band, 0, 0, sceneWidth, sceneHeight, sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because sourceBuffer is to small");
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
    }

    public void testWriteBandRasterData_SourceBuffer_toBig() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
                                      sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            //Make the sourceBuffer to big
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight + 1);
            _productWriter.writeBandRasterData(band, 0, 0, sceneWidth, sceneHeight, sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because sourceBuffer is to big");
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
    }

    public void testWriteBandRasterData_SourceRegionIsOutOfBandsRaster() {
        int sceneWidth = 16;
        int sceneHeight = 12;
        Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
                                      sceneWidth, sceneHeight);
        Band band = new Band("band", ProductData.TYPE_INT8, sceneWidth, sceneHeight);
        product.addBand(band);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException not expected");
        }

        //buffer is outside at the left side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = -1;
            _productWriter.writeBandRasterData(band,
                                               makeOutside, 0,
                                               sceneWidth, sceneHeight,
                                               sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the top side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = -1;
            _productWriter.writeBandRasterData(band,
                                               0, makeOutside,
                                               sceneWidth, sceneHeight,
                                               sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the right side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = 1;
            _productWriter.writeBandRasterData(band,
                                               makeOutside, 0,
                                               sceneWidth, sceneHeight,
                                               sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        //buffer is outside at the botom side
        try {
            ProductData sourceBuffer = getSourceData(sceneWidth * sceneHeight);
            int makeOutside = 1;
            _productWriter.writeBandRasterData(band,
                                               0, makeOutside,
                                               sceneWidth, sceneHeight,
                                               sourceBuffer, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because region is ot of band's region");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException not expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            _productWriter.close();
        } catch (IOException e) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///   End Of Public
    ///////////////////////////////////////////////////////////////////////////

    private ProductData getFilledSourceData(int size) {
        ProductData data = getSourceData(size);
        byte[] bytes = new byte[data.getNumElems()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 85;
        }
        data.setElems(bytes);
        return data;
    }

    private ProductData getSourceData(int size) {
        return ProductData.createInstance(ProductData.TYPE_INT8, (size));
    }

    private byte[] prepareExpectedArrayInt8(int width, int height) {
        byte[] bytes = new byte[width * height];
        byte zero = 0;
        byte fill = 85;

//      This loop fills the array like these scheme
//        00000000   # -> fill
//        0######0   . -> variable size filled with #
//        0#....#0
//        0######0
//        00000000
        for (int y = 0; y < height; y++) {
            byte filler = (y == 0 || y == height - 1) ? zero : fill;
            for (int x = 0; x < width; x++) {
                bytes[y * width + x] = x == 0 || x == width - 1 ? zero : filler;
            }
        }
        return bytes;
    }

    private byte[] getCurrentByteArray(Band band) {
        FileImageInputStream inputStream = createInputStream(band);
        int fileLength = new Long(inputStream.length()).intValue();
        byte[] currentBytes = new byte[fileLength];
        try {
            inputStream.readFully(currentBytes);
            inputStream.close();
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return currentBytes;
    }

    private FileImageInputStream createInputStream(Band band) {
        final String nameWithoutExtension = FileUtils.getFilenameWithoutExtension(_outputFile);
        File dataDir = new File(_outputDir, nameWithoutExtension + ".data");
        File file = new File(dataDir, band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
        assertEquals(true, file.exists());
        FileImageInputStream inputStream = null;
        try {
            inputStream = new FileImageInputStream(file);
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return inputStream;
    }
}
