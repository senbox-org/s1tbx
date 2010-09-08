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

package org.esa.beam;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.CsvReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// This class is used to convert the provided validation data set into a set of test products.

// It converts the provided binary file in to DIMAP data product and generates 3 subsets for
// testing purpose (radiance test input, smile test input and expected result).
class TestProductGenerator {

    private static final String NAN_ELEM = "-NaN";
    private static final int BAND_COUNT = 15;

    private TestProductGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Wrong number of arguments.");
            System.out.println("Use: <resultBinaryFilePath> <templateProductFilePath> ");
            System.exit(1);
        }

        final File binaryFile = new File(args[0]);
        final Product templateProduct = ProductIO.readProduct(args[1]);

        float[][] bandData;
        final int size = templateProduct.getSceneRasterWidth() * templateProduct.getSceneRasterHeight();
        bandData = new float[BAND_COUNT][size];

        readData(binaryFile, bandData);

        final Product targetProduct = createTargetProduct(templateProduct);
        addBands(targetProduct, bandData);

        final File outputDir = templateProduct.getFileLocation().getParentFile();
        ProductIO.writeProduct(targetProduct, new File(outputDir, String.format("%s.dim", targetProduct.getName())),
                               ProductIO.DEFAULT_FORMAT_NAME, false);

        final Product testInputProduct = createTestSubset(templateProduct, outputDir, "TestInput_Rad");
        createSmiledTestSubset(testInputProduct, outputDir);
        createTestSubset(targetProduct, outputDir, "ExpectedOutput");
    }

    private static void createSmiledTestSubset(Product testInputProduct, File outputDir) throws IOException {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final SmileCorrectionOp.Spi smileOpSpi = new SmileCorrectionOp.Spi();
        registry.addOperatorSpi(smileOpSpi);

        try {
            final Product testInputSmileProduct = GPF.createProduct("SmileCorr", GPF.NO_PARAMS, testInputProduct);
            final MetadataElement metadataRoot = testInputSmileProduct.getMetadataRoot();
            metadataRoot.removeElement(metadataRoot.getElement("Processing_Graph"));
            final File outFile = new File(outputDir, String.format("%s.dim", testInputSmileProduct.getName()));
            ProductIO.writeProduct(testInputSmileProduct, outFile, ProductIO.DEFAULT_FORMAT_NAME, false);
        } finally {
            registry.removeOperatorSpi(smileOpSpi);
        }
    }

    private static Product createTestSubset(Product product, File outputDir, String productNameSuffix) throws
                                                                                                       IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(0, 100, product.getSceneRasterWidth(), 6);
        final String subsetName = product.getName().substring(0, 23) + productNameSuffix;
        final Product subset = ProductSubsetBuilder.createProductSubset(product,
                                                                        subsetDef, subsetName,
                                                                        product.getDescription());
        final MetadataElement metadataRoot = subset.getMetadataRoot();
        metadataRoot.removeElement(metadataRoot.getElement("history"));
        ProductIO.writeProduct(subset, new File(outputDir, String.format("%s.dim", subset.getName())),
                               ProductIO.DEFAULT_FORMAT_NAME, false);
        return subset;
    }

    private static void addBands(Product target, float[][] bandData) {
        for (int i = 0; i < bandData.length; i++) {
            final float[] data = bandData[i];
            final Band band = target.getBand(String.format("refl_eq_%d", i + 1));
            swapData(data, band.getSceneRasterWidth(), band.getSceneRasterHeight());
            band.setData(ProductData.createInstance(data));
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
    }

    private static Product createTargetProduct(Product templateProduct) throws IOException {
        final ProductSubsetDef def = new ProductSubsetDef();
        def.addNodeName("detector_index");
        def.addNodeName("l1_flags");
        def.addNodeNames(templateProduct.getTiePointGridNames());
        final Product target = templateProduct.createSubset(def,
                                                            String.format("%s_equalized", templateProduct.getName()),
                                                            "Product for testing of equalization algorithm");
        final MetadataElement metadataRoot = target.getMetadataRoot();
        metadataRoot.removeElement(metadataRoot.getElement("history"));
        ProductUtils.copyMetadata(templateProduct, target);
        target.setAutoGrouping("refl_eq");

        for (int i = 1; i <= BAND_COUNT; i++) {
            final Band band = target.addBand(String.format("refl_eq_%d", i), ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(templateProduct.getBand(String.format("radiance_%d", i)), band);
        }

        // move l1_flags and detector_index bands at the end of the band list
        final Band flags = target.getBand("l1_flags");
        target.removeBand(flags);
        target.addBand(flags);
        final Band detector_index = target.getBand("detector_index");
        target.removeBand(detector_index);
        target.addBand(detector_index);
        return target;
    }

    private static void readData(File binaryFile, float[][] bandData) throws IOException {
        final FileReader reader = new FileReader(binaryFile);
        final CsvReader csvReader = new CsvReader(reader, new char[]{' '});
        try {
            int sampleIndex = 0;
            int bandIndex = 0;
            String[] stringValues = csvReader.readRecord();
            while (stringValues != null) {
                for (String stringValue : stringValues) {
                    float value = Float.NaN;
                    if (!stringValue.equals(NAN_ELEM)) {
                        value = Float.parseFloat(stringValue.trim());
                    }
                    bandData[bandIndex][sampleIndex] = value;
                    sampleIndex++;
                    if (bandData[bandIndex].length == sampleIndex) {
                        bandIndex++;
                        sampleIndex = 0;
                    }
                }
                stringValues = csvReader.readRecord();
            }

        } finally {
            csvReader.close();
        }
    }

    private static void swapData(float[] data, int bandWidth, int bandHeight) {
        for (int y = 0; y < bandHeight; y++) {
            final int fromIndex = bandWidth * y;
            final int toIndex = fromIndex + bandWidth;
            final float[] line = Arrays.copyOfRange(data, fromIndex, toIndex);
            ArrayUtils.swapArray(line);
            System.arraycopy(line, 0, data, fromIndex, line.length);
        }
    }
}
