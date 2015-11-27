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
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.core.util.BeamConstants;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

        assertEquals("", compareProducts(_product, currentProduct));
    }

    public void testWriteAndReadProductNodes_GivenFilenameWithoutExtension() throws IOException {
        Product currentProduct = null;

        File file = new File(_ioDir, "testproduct");
        _writer.writeProductNodes(_product, file);
        writeAllBandRasterDataFully();
        file = new File(_ioDir, "testproduct" + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        currentProduct = _reader.readProductNodes(file, null);

        assertEquals("", compareProducts(_product, currentProduct));
    }

///////////////////////////////////////////////////////////////////////////////////////////
///////////////////           E N D     O F     P U B L I C              //////////////////
///////////////////////////////////////////////////////////////////////////////////////////

    private void writeAllBandRasterDataFully() throws IOException {
        final Band[] bands = _product.getBands();
        for (final Band band : bands) {
            if (_product.getProductWriter().shouldWrite(band)) {
                band.writeRasterDataFully(ProgressMonitor.NULL);
            }
        }
    }

    private static String compareProducts(Product expProduct, Product currentProduct) throws IOException {
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
        assertEquals("NumBands", 5, product.getNumBands());
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
            if (expTPGrid.getGridWidth() != currentTPGrid.getGridWidth()) {
                diff.append(
                            "GridWidth of TiePointGrid " + i + " expected <" + expTPGrid.getGridWidth() + "> but was <" + currentTPGrid.getGridWidth() + ">\r\n");
            }
            if (expTPGrid.getGridHeight() != currentTPGrid.getGridHeight()) {
                diff.append(
                            "GridHeight of TiePointGrid " + i + " expected <" + expTPGrid.getGridHeight() + "> but was <" + currentTPGrid.getGridHeight() + ">\r\n");
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
            if (!expTPGrid.getGridData().equalElems(currentTPGrid.getGridData())) {
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

    private static void compareBands(Product expProduct, Product currentProduct, StringBuffer diff) throws IOException {
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
            if (expBand.getRasterWidth() != currentBand.getRasterWidth()) {
                diff.append(
                        "RasterWidth of Band " + i + " expected <" + expBand.getRasterWidth() + "> but was <" + currentBand.getRasterWidth() + ">\r\n");
            }
            if (expBand.getRasterHeight() != currentBand.getRasterHeight()) {
                diff.append(
                        "RasterHeight of Band " + i + " expected <" + expBand.getRasterHeight() + "> but was <" + currentBand.getRasterHeight() + ">\r\n");
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
            final String validMaskExpression = expBand.getValidPixelExpression();
            if (validMaskExpression != null) {
                if (!validMaskExpression.equals(currentBand.getValidPixelExpression())) {
                    diff.append("ValidMaskExpression <" + currentBand.getValidPixelExpression() +
                                "> of Band " + i + " is not equal to expected mask <" +
                                validMaskExpression + ">\r\n");
                }
            }
            final AffineTransform expTransform = expBand.getImageToModelTransform();
            final AffineTransform curTransform = currentBand.getImageToModelTransform();
            if (!ObjectUtils.equalObjects(expTransform, curTransform)) {
                diff.append("The image to model transform of band " + i + " is not equal to the expected transform.");
            }
            compareBandData(expBand, currentBand, i, diff);
        }
        for (int i = 0; i < expBands.length; i++) {
            final Band expBand = expBands[i];
            final Band currentBand = currentBands[i];
            final String[] ancillaryRelations = expBand.getAncillaryRelations();
            if (!Arrays.equals(ancillaryRelations, currentBand.getAncillaryRelations())) {
                diff.append("The ancillary relations of expected band " + i + " are not equal to the current band.\r\n");
            }
        }
        for (int i = 0; i < expBands.length; i++) {
            final Band expBand = expBands[i];
            final Band currentBand = currentBands[i];
            final RasterDataNode[] expectedVariables = expBand.getAncillaryVariables();
            final RasterDataNode[] actualVariables = currentBand.getAncillaryVariables();
            for (int j = 0; j < expectedVariables.length; j++) {
                final RasterDataNode expectedVariable = expectedVariables[j];
                final RasterDataNode actualVariable = actualVariables[j];
                final String expVarName = expectedVariable.getName();
                final String actVarName = actualVariable.getName();
                if (!expVarName.equals(actVarName)) {
                    diff.append("Ancillary variable named '" + expVarName + "' expected atband " + i + ".\r\n");
                }
            }
        }
    }

    private static void compareBandData(Band expBand, Band currentBand, int i, StringBuffer diff) throws IOException {
        expBand.loadRasterData(ProgressMonitor.NULL);
        currentBand.loadRasterData(ProgressMonitor.NULL);
        if (currentBand.getData() == null) {
            diff.append("current Band " + i + " has no data>\r\n");
        }
        if (!expBand.getData().equalElems(currentBand.getData())) {
            diff.append("Data of Band " + i + " are not equal to expected data>\r\n");
        }
    }

    private static void addBands(Product product) {
        final String descriptionExpansion = "-Description";
        final String flagsBandName = "flags";
        final String indexesBandName = "indexes";
        final String band1Name = "band1";
        final String band2Name = "band2";
        final String uncBandName = "uncertainty";
//        final String vbName = "vb1";
//        final String cfbName = "cfb1";
//        final String gfbName = "gfb1";

        final Band flagsBand = product.addBand(flagsBandName, ProductData.TYPE_INT8);
        flagsBand.setDescription(flagsBandName + descriptionExpansion);
        flagsBand.setSampleCoding(product.getFlagCodingGroup().get(0));
        fillBandWithData(flagsBand);

        final Band indexesBand = product.addBand(indexesBandName, ProductData.TYPE_INT16);
        indexesBand.setDescription(indexesBandName + descriptionExpansion);
        indexesBand.setSampleCoding(product.getIndexCodingGroup().get(0));
        fillBandWithData(indexesBand);

        final Band band1 = product.addBand(band1Name, ProductData.TYPE_FLOAT32);
        band1.setDescription(band1Name + descriptionExpansion);
        fillBandWithData(band1);

        final Band band2 = product.addBand(band2Name, ProductData.TYPE_INT8);
        band2.setDescription(band2Name + descriptionExpansion);
        band2.setImageToModelTransform(new AffineTransform(new double[]{1.2, 2.3, 3.4, 4.5, 5.6, 6.7}));
        fillBandWithData(band2);

        final Band uncBand = product.addBand(uncBandName, ProductData.TYPE_FLOAT32);
        uncBand.setDescription(uncBandName + descriptionExpansion);
        fillBandWithData(uncBand, 0.1);

        band1.addAncillaryVariable(uncBand, "uncertainty");
    }

    private static void fillBandWithData(Band band) {
        fillBandWithData(band, 1);
    }

    private static void fillBandWithData(Band band, double scaleFactor) {
        final ProductData productData = band.createCompatibleRasterData();
        final int n = productData.getNumElems();
        for (int i = 0; i < n; i++) {
            productData.setElemDoubleAt(i, i * 2.4 * scaleFactor);
        }
        band.setData(productData);
    }
}
