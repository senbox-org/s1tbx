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
package org.esa.beam.dataio.dimap;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.GlobalTestTools;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.BeamConstants;

import java.io.File;
import java.io.IOException;

public class DimapWriteAndReadTest extends TestCase {

    private final DimapProductWriterPlugIn _writerPlugIn = new DimapProductWriterPlugIn();
    private final DimapProductReaderPlugIn _readerPlugIn = new DimapProductReaderPlugIn();
    private DimapProductWriter _writer;
    private DimapProductReader _reader;
    private File _ioDir;
    private Product _product;

    public DimapWriteAndReadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DimapWriteAndReadTest.class);
    }

    @Override
    protected void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
        _writer = new DimapProductWriter(_writerPlugIn);
        _reader = new DimapProductReader(_readerPlugIn);
        _ioDir = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "testproduct");
        _product = createProduct();
    }

    @Override
    protected void tearDown() {
        try {
            if (_writer != null) {
                _writer.close();
            }
            if (_reader != null) {
                _reader.close();
            }
        } catch (IOException e) {
        }
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    public void testWriteAndReadProductNodes_withoutSubsetInfo() throws IOException {
        final File file = new File(_ioDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        _writer.writeProductNodes(_product, file);
        writeAllBandRasterDataFully();
        Product currentProduct = _reader.readProductNodes(file, null);
        loadAllBandRasterData(currentProduct);

        assertEquals("", compareProducts(_product, currentProduct));
    }

    public void testWriteAndReadProductNodes_GivenFilenameWithoutExtension() {
        Product currentProduct = null;

        try {
            File file = new File(_ioDir, "testproduct");
            _writer.writeProductNodes(_product, file);
            writeAllBandRasterDataFully();
            file = new File(_ioDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
            currentProduct = _reader.readProductNodes(file, null);
            loadAllBandRasterData(currentProduct);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals("", compareProducts(_product, currentProduct));
    }

///////////////////////////////////////////////////////////////////////////////////////////
///////////////////           E N D     O F     P U B L I C              //////////////////
///////////////////////////////////////////////////////////////////////////////////////////

     private static void loadAllBandRasterData(Product product) throws IOException {
        final Band[] bands = product.getBands();
         for (final Band band : bands) {
             band.loadRasterData(ProgressMonitor.NULL);
         }
    }

    private void writeAllBandRasterDataFully() throws IOException {
        final Band[] bands = _product.getBands();
        for (final Band band : bands) {
            if (_product.getProductWriter().shouldWrite(band)) {
                band.writeRasterDataFully(ProgressMonitor.NULL);
            }
        }
    }

    private static String compareProducts(Product expProduct, Product currentProduct) {
        final StringBuffer diff = new StringBuffer();
        if (currentProduct == null) {
            diff.append("the current product is null \r\n");
        } else {
            if (!expProduct.getName().equals(currentProduct.getName())) {
                diff.append(
                        "Product_Name expected <" + expProduct.getName() + "> but was <" + currentProduct.getName() + ">\r\n");
            }
            if (!expProduct.getProductType().equals(currentProduct.getProductType())) {
                diff.append(
                        "Product_Type expected <" + expProduct.getProductType() + "> but was <" + currentProduct.getProductType() + ">\r\n");
            }
            if (expProduct.getSceneRasterWidth() != currentProduct.getSceneRasterWidth()) {
                diff.append(
                        "Product_SceneWidth expected <" + expProduct.getSceneRasterWidth() + "> but was <" + currentProduct.getSceneRasterWidth() + ">\r\n");
            }
            if (expProduct.getSceneRasterHeight() != currentProduct.getSceneRasterHeight()) {
                diff.append(
                        "Product_SceneHeight expected <" + expProduct.getSceneRasterHeight() + "> but was <" + currentProduct.getSceneRasterHeight() + ">\r\n");
            }
            if (expProduct.getNumBands() != currentProduct.getNumBands()) {
                diff.append(
                        "Product_numBands expected <" + expProduct.getNumBands() + "> but was <" + currentProduct.getNumBands() + ">\r\n");
            }
            compareBands(expProduct, currentProduct, diff);
            if (expProduct.getNumTiePointGrids() != currentProduct.getNumTiePointGrids()) {
                diff.append(
                        "Product_numTiePointGrids expected <" + expProduct.getNumTiePointGrids() + "> but was <" + currentProduct.getNumTiePointGrids() + ">\r\n");
            }
            compareTiePointGrids(expProduct, currentProduct, diff);
        }
        return diff.toString();
    }

    private static Product createProduct() {
        final int sceneRasterWidth = 129;
        final int sceneRasterHeight = 161;
        final Product product = new Product("name", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME, sceneRasterWidth,
                                            sceneRasterHeight);
        addFlagCoding(product);
        addIndexCoding(product);
        addBands(product);
        assertEquals("NumBands", 4, product.getNumBands());
        addTiePointGrids(product);
        assertEquals("NumTiePointGrids", 2, product.getNumTiePointGrids());
        return product;
    }

    private static void addFlagCoding(Product product) {
        final FlagCoding flagCoding = new FlagCoding("Flags");
        flagCoding.addFlag("land", 1, "Land Flag");
        flagCoding.addFlag("bright", 2, "Bright Flag");
        product.getFlagCodingGroup().add(flagCoding);
    }

    private static void addIndexCoding(Product product) {
        final IndexCoding indexCoding = new IndexCoding("Indexes");
        indexCoding.addIndex("land", 12, "Land Index");
        indexCoding.addIndex("bright", 13, "Bright Index");
        product.getIndexCodingGroup().add(indexCoding);
    }

    private static void compareTiePointGrids(Product expProduct, Product currentProduct, StringBuffer diff) {
        final String[] expTiePointGridNames = expProduct.getTiePointGridNames();
        final String[] currentTiePointGridNames = currentProduct.getTiePointGridNames();
        for (int i = 0; i < expTiePointGridNames.length; i++) {
            final TiePointGrid expTPGrid = expProduct.getTiePointGrid(expTiePointGridNames[i]);
            final TiePointGrid currentTPGrid = currentProduct.getTiePointGrid(currentTiePointGridNames[i]);
            if (!expTPGrid.getName().equals(currentTPGrid.getName())) {
                diff.append(
                        "Name of TiePointGrid " + i + " expected <" + expTPGrid.getName() + "> but was <" + currentTPGrid.getName() + ">\r\n");
            }
            if (expTPGrid.getDataType() != currentTPGrid.getDataType()) {
                diff.append(
                        "DataType of TiePointGrid " + i + " expected <" + expTPGrid.getDataType() + "> but was <" + currentTPGrid.getDataType() + ">\r\n");
            }
            if (expTPGrid.getRasterWidth() != currentTPGrid.getRasterWidth()) {
                diff.append(
                        "RasterWidth of TiePointGrid " + i + " expected <" + expTPGrid.getRasterWidth() + "> but was <" + currentTPGrid.getRasterWidth() + ">\r\n");
            }
            if (expTPGrid.getRasterHeight() != currentTPGrid.getRasterHeight()) {
                diff.append(
                        "RasterHeight of TiePointGrid " + i + " expected <" + expTPGrid.getRasterHeight() + "> but was <" + currentTPGrid.getRasterHeight() + ">\r\n");
            }
            if (expTPGrid.getOffsetX() != currentTPGrid.getOffsetX()) {
                diff.append(
                        "OffsetX of TiePointGrid " + i + " expected <" + expTPGrid.getOffsetX() + "> but was <" + currentTPGrid.getOffsetX() + ">\r\n");
            }
            if (expTPGrid.getOffsetY() != currentTPGrid.getOffsetY()) {
                diff.append(
                        "OffsetY of TiePointGrid " + i + " expected <" + expTPGrid.getOffsetY() + "> but was <" + currentTPGrid.getOffsetY() + ">\r\n");
            }
            if (expTPGrid.getSubSamplingX() != currentTPGrid.getSubSamplingX()) {
                diff.append(
                        "SubSamplingX of TiePointGrid " + i + " expected <" + expTPGrid.getSubSamplingX() + "> but was <" + currentTPGrid.getSubSamplingX() + ">\r\n");
            }
            if (expTPGrid.getSubSamplingY() != currentTPGrid.getSubSamplingY()) {
                diff.append(
                        "SubSamplingY of TiePointGrid " + i + " expected <" + expTPGrid.getSubSamplingY() + "> but was <" + currentTPGrid.getSubSamplingY() + ">\r\n");
            }
            if (!expTPGrid.getData().equalElems(currentTPGrid.getData())) {
                diff.append("Data of TiePointGrid " + i + " are not equal>\r\n");
            }
            if (!expTPGrid.getDescription().equals(currentTPGrid.getDescription())) {
                diff.append(
                        "Description of TiePointGrid " + i + " expected <" + expTPGrid.getDescription() + "> but was <" + currentTPGrid.getDescription() + ">\r\n");
            }
            if (!expTPGrid.getUnit().equals(currentTPGrid.getUnit())) {
                diff.append(
                        "Unit of TiePointGrid " + i + " expected <" + expTPGrid.getUnit() + "> but was <" + currentTPGrid.getUnit() + ">\r\n");
            }
        }
    }

    private static void addTiePointGrids(Product product) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        product.addTiePointGrid(createTiePointGrid("tpg1", sceneRasterWidth, sceneRasterHeight, 32, 32, 0, 0));
        product.addTiePointGrid(createTiePointGrid("tpg2", sceneRasterWidth, sceneRasterHeight, 16, 32, 21, 14));
    }

    private static TiePointGrid createTiePointGrid(String name, int sceneW, int sceneH, int stepX, int stepY, int offX,
                                                   int offY) {
        final int gridWidth = sceneW / stepX + 1;
        final int gridHeight = sceneH / stepY + 1;
        final float[] floats = new float[gridWidth * gridHeight];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = i * 3.54f;
        }
        final TiePointGrid tpg = new TiePointGrid(name,
                                                  gridWidth, gridHeight,
                                                  offX, offY,
                                                  stepX, stepY,
                                                  floats);
        tpg.setDescription(name + "-Description");
        tpg.setUnit(name + "-unit");
        return tpg;
    }

    private static void compareBands(Product expProduct, Product currentProduct, StringBuffer diff) {
        final Band[] expBands = expProduct.getBands();
        final Band[] currentBands = currentProduct.getBands();
        for (int i = 0; i < expBands.length; i++) {
            final Band expBand = expBands[i];
            final Band currentBand = currentBands[i];
            if (!expBand.getName().equals(currentBand.getName())) {
                diff.append(
                        "Name of Band " + i + " expected <" + expBand.getName() + "> but was <" + currentBand.getName() + ">\r\n");
            }
            if (!expBand.getDescription().equals(currentBand.getDescription())) {
                diff.append(
                        "Description of Band " + i + " expected <" + expBand.getDescription() + "> but was <" + currentBand.getDescription() + ">\r\n");
            }
            if (expBand.getDataType() != currentBand.getDataType()) {
                diff.append(
                        "DataType of Band " + i + " expected <" + expBand.getDataType() + "> but was <" + currentBand.getDataType() + ">\r\n");
            }
            if (expBand.getSceneRasterWidth() != currentBand.getSceneRasterWidth()) {
                diff.append(
                        "SceneRasterWidth of Band " + i + " expected <" + expBand.getSceneRasterWidth() + "> but was <" + currentBand.getSceneRasterWidth() + ">\r\n");
            }
            if (expBand.getSceneRasterHeight() != currentBand.getSceneRasterHeight()) {
                diff.append(
                        "SceneRasterHeight of Band " + i + " expected <" + expBand.getSceneRasterHeight() + "> but was <" + currentBand.getSceneRasterHeight() + ">\r\n");
            }
            if (expBand.getFlagCoding() != null && !(currentBand.getFlagCoding() != null)) {
                diff.append(
                        "FlagCoding of Band " + i + " expected non null\r\n");
            }
            if (expBand.getFlagCoding() != null && currentBand.getFlagCoding() != null && !expBand.getFlagCoding().getName().equals(currentBand.getFlagCoding().getName())) {
                diff.append(
                        "FlagCoding of Band " + i + " not equal\r\n");
            }
            if (expBand.getIndexCoding() != null && !(currentBand.getIndexCoding() != null)) {
                diff.append(
                        "IndexCoding of Band " + i + " expected non null\r\n");
            }
            if (expBand.getIndexCoding() != null && currentBand.getIndexCoding() != null && !expBand.getIndexCoding().getName().equals(currentBand.getIndexCoding().getName())) {
                diff.append(
                        "IndexCoding of Band " + i + " not equal\r\n");
            }
            if (currentBand.getData() == null) {
                diff.append("current Band " + i + " has no data>\r\n");
            }
            if (!expBand.getData().equalElems(currentBand.getData())) {
                diff.append("Data of Band " + i + " are not equal to expected data>\r\n");
            }
            final String validMaskExpression = expBand.getValidPixelExpression();
            if (validMaskExpression != null) {
                if (!validMaskExpression.equals(currentBand.getValidPixelExpression())) {
                    diff.append("ValidMaskExpression <" + currentBand.getValidPixelExpression() +
                                "> of Band " + i + " is not equal to expected mask <" +
                                validMaskExpression + ">\r\n");
                }
            }
        }
    }

    private static void addBands(Product product) {
        final String descriptionExpansion = "-Description";
        final String flagsBandName = "flags";
        final String indexesBandName = "indexes";
        final String band1Name = "band1";
        final String band2Name = "band2";
//        final String vbName = "vb1";
//        final String cfbName = "cfb1";
//        final String gfbName = "gfb1";

        product.addBand(flagsBandName, ProductData.TYPE_INT8);
        final Band flagsBand = product.getBand(flagsBandName);
        flagsBand.setDescription(flagsBandName + descriptionExpansion);
        flagsBand.setSampleCoding(product.getFlagCodingGroup().get(0));
        fillBandWithData(flagsBand);

        product.addBand(indexesBandName, ProductData.TYPE_INT16);
        final Band indexesBand = product.getBand(indexesBandName);
        indexesBand.setDescription(indexesBandName + descriptionExpansion);
        indexesBand.setSampleCoding(product.getIndexCodingGroup().get(0));
        fillBandWithData(indexesBand);

        product.addBand(band1Name, ProductData.TYPE_FLOAT32);
        final Band band1 = product.getBand(band1Name);
        band1.setDescription(band1Name + descriptionExpansion);
        fillBandWithData(band1);

        product.addBand(band2Name, ProductData.TYPE_INT8);
        final Band band2 = product.getBand(band2Name);
        band2.setDescription(band2Name + descriptionExpansion);
        fillBandWithData(band2);
    }

    private static void fillBandWithData(Band band) {
        final ProductData productData = band.createCompatibleRasterData();
        final int n = productData.getNumElems();
        for (int i = 0; i < n; i++) {
            productData.setElemDoubleAt(i, i * 2.4);
        }
        band.setData(productData);
    }
}
