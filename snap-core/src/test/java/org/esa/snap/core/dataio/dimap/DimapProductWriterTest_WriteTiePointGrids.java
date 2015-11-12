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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.BeamConstants;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DimapProductWriterTest_WriteTiePointGrids extends TestCase {

    private final DimapProductWriterPlugIn _writerPlugIn = new DimapProductWriterPlugIn();
    private DimapProductWriter _productWriter;
    private File _outputDir;
    private File _outputFile;

    public DimapProductWriterTest_WriteTiePointGrids(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(DimapProductWriterTest_WriteTiePointGrids.class);
    }

    @Override
    protected void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _productWriter = new DimapProductWriter(_writerPlugIn);
        _outputDir = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "testproduct");
        _outputFile = new File(_outputDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
    }

    @Override
    protected void tearDown() {
        if (_productWriter != null) {
            try {
                _productWriter.close();
            } catch (IOException e) {
            }
        }
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    public void testWriteProductNodes_TiePointGrid() {
        Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
                                      50, 25);
        float[] expectedArray = getTiePointData(10, 5);
        TiePointGrid tiePointGrid = new TiePointGrid("name", 10, 5, 0, 0, 5, 5, expectedArray);
        product.addTiePointGrid(tiePointGrid);

        try {
            _productWriter.writeProductNodes(product, _outputFile);
            _productWriter.close();
        } catch (IOException e) {
            fail("IOException not expected");
        }

        float[] currentArray = getCurrentByteArray(tiePointGrid);
        assertEquals(expectedArray.length, currentArray.length);

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], currentArray[i], 1e-8f);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///   End Of Public
    ///////////////////////////////////////////////////////////////////////////

    private float[] getTiePointData(int width, int height) {
        float[] tiePoints = new float[width * height];
        for (int x = 0; x < tiePoints.length; x++) {
            tiePoints[x] = x * 1000;
        }
        return tiePoints;
    }

    private float[] getCurrentByteArray(TiePointGrid grid) {
        FileImageInputStream inputStream = createInputStream(grid);
        int fileLength = new Long(inputStream.length()).intValue();
        int arrayLength = fileLength / ProductData.getElemSize(ProductData.TYPE_FLOAT32);
        float[] currentFloats = new float[arrayLength];
        try {
            inputStream.readFully(currentFloats, 0, arrayLength);
            inputStream.close();
        } catch (FileNotFoundException e) {
            fail("FileNotFoundException not expected");
        } catch (IOException e) {
            fail("IOException not expected");
        }
        return currentFloats;
    }

    private FileImageInputStream createInputStream(TiePointGrid grid) {
        File tiePointGridDir = new File(createDataDir(), DimapProductConstants.TIE_POINT_GRID_DIR_NAME);
        File file = new File(tiePointGridDir, grid.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
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

    private File createDataDir() {
        final String nameWithoutExtension = FileUtils.getFilenameWithoutExtension(_outputFile);
        return new File(_outputDir, nameWithoutExtension + ".data");
    }
}
