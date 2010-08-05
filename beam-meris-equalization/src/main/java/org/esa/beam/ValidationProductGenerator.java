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
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.CsvReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// This class is used to convert the provided validation data set into a product.
// The Envisat product corresponding to the data set is used as template to create
// the validation product.
class ValidationProductGenerator {

    private static final String NAN_ELEM = "-NaN";
    private static final int BAND_WIDTH = 1121;
    private static final int BAND_HEIGHT = 305;
    private static final int BAND_SIZE = BAND_WIDTH * BAND_HEIGHT;
    private static final int BAND_COUNT = 15;

    private ValidationProductGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Too few arguments.");
            System.out.println("Use: <binaryFilePath> <templateProductFilePath>");
            System.exit(1);
        }

        final File binaryFile = new File(args[0]);
        float[][] bandData = new float[BAND_COUNT][BAND_SIZE];
        readData(binaryFile, bandData);

        final Product templateProduct = ProductIO.readProduct(args[1]);
        final Product targetProduct = createTargetProduct(templateProduct);
        addBands(targetProduct, bandData);

        final File outputDir = templateProduct.getFileLocation().getParentFile();
        ProductIO.writeProduct(targetProduct, new File(outputDir, String.format("%s.dim", targetProduct.getName())),
                               ProductIO.DEFAULT_FORMAT_NAME, false);
    }

    private static void addBands(Product target, float[][] bandData) {
        for (int i = 0; i < bandData.length; i++) {
            final float[] data = bandData[i];
            swapData(data);
            final Band band = target.getBand(String.format("refl_eq_%d", i + 1));
            band.setData(ProductData.createInstance(data));
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
                    if (BAND_SIZE == sampleIndex) {
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

    private static void swapData(float[] data) {
        for (int y = 0; y < BAND_HEIGHT; y++) {
            final int fromIndex = BAND_WIDTH * y;
            final int toIndex = fromIndex + BAND_WIDTH;
            final float[] line = Arrays.copyOfRange(data, fromIndex, toIndex);
            ArrayUtils.swapArray(line);
            System.arraycopy(line, 0, data, fromIndex, line.length);
        }
    }
}
