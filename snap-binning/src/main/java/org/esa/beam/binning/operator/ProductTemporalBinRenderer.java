/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.binning.ProductCustomizer;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.TemporalBinRenderer;
import org.esa.beam.binning.Vector;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A renderer that renders temporal bins into {@link Product}s.
 *
 * @author Norman Fomferra
 */
public final class ProductTemporalBinRenderer implements TemporalBinRenderer {

    private final Product product;
    private final int rasterWidth;
    private final ProductData numObsLine;
    private final ProductData numPassesLine;
    private final Band[] outputBands;
    private final List<Band> customizerBands;
    private final ProductData[] outputLines;
    private final Band numObsBand;
    private final Band numPassesBand;
    private final float[] fillValues;
    private final File outputFile;
    private int yLast;
    private final ProductWriter productWriter;
    private final Rectangle outputRegion;

    public ProductTemporalBinRenderer(String[] featureNames,
                                      File outputFile,
                                      String outputFormat,
                                      Rectangle outputRegion,
                                      double pixelSize,
                                      ProductData.UTC startTime,
                                      ProductData.UTC endTime,
                                      ProductCustomizer productCustomizer,
                                      MetadataElement... metadataElements) throws IOException {

        productWriter = ProductIO.getProductWriter(outputFormat);
        if (productWriter == null) {
            throw new IllegalArgumentException("No writer found for output format " + outputFormat);
        }

        this.outputRegion = new Rectangle(outputRegion);
        this.outputFile = outputFile;

        CrsGeoCoding geoCoding = createMapGeoCoding(outputRegion, pixelSize);

        product = new Product(outputFile.getName(), "BINNED-L3", outputRegion.width, outputRegion.height);
        product.setProductWriter(productWriter);
        product.setPreferredTileSize(64, 64);
        product.setGeoCoding(geoCoding);
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        for (MetadataElement metadataElement : metadataElements) {
            product.getMetadataRoot().addElement(metadataElement);
        }

        Band localNumObsBand = product.addBand("num_obs", ProductData.TYPE_INT32);
        localNumObsBand.setNoDataValue(-1);
        localNumObsBand.setNoDataValueUsed(true);

        Band localNumPassesBand = product.addBand("num_passes", ProductData.TYPE_INT16);
        localNumPassesBand.setNoDataValue(-1);
        localNumPassesBand.setNoDataValueUsed(true);

        boolean numObsIsFeature = false;
        boolean numPassesIsFeature = false;
        for (String name : featureNames) {
            switch (name) {
                case "num_obs":
                    numObsIsFeature = true;
                    break;
                case "num_passes":
                    numPassesIsFeature = true;
                    break;
                default:
                    Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
                    band.setNoDataValue(Float.NaN);
                    band.setNoDataValueUsed(true);
                    break;
            }
        }

        if (productCustomizer != null) {
            productCustomizer.customizeProduct(product);
            customizerBands = new ArrayList<>(product.getNumBands());
            Collections.addAll(customizerBands, product.getBands());
        } else {
             customizerBands = Collections.emptyList();
        }
        numObsBand = product.getBand("num_obs");
        if (numObsBand != null && !numObsIsFeature) {
            numObsLine = numObsBand.createCompatibleRasterData(outputRegion.width, 1);
            if (!customizerBands.isEmpty()) {
                customizerBands.remove(numObsBand);
            }
        } else {
            numObsLine = null;
        }
        numPassesBand = product.getBand("num_passes");
        if (numPassesBand != null && !numPassesIsFeature) {
            numPassesLine = numPassesBand.createCompatibleRasterData(outputRegion.width, 1);
            if (!customizerBands.isEmpty()) {
                customizerBands.remove(numPassesBand);
            }
        } else {
            numPassesLine = null;
        }

        outputBands = new Band[featureNames.length];
        outputLines = new ProductData[featureNames.length];
        for (int i = 0; i < featureNames.length; i++) {
            String name = featureNames[i];
            outputBands[i] = product.getBand(name);
            outputLines[i] = outputBands[i].createCompatibleRasterData(outputRegion.width, 1);
            if (!customizerBands.isEmpty()) {
                customizerBands.remove(outputBands[i]);
            }
        }

        this.rasterWidth = outputRegion.width;
        this.fillValues = new float[outputBands.length];
        for (int i = 0; i < outputBands.length; i++) {
            fillValues[i] = (float) outputBands[i].getNoDataValue();
        }
    }

    @Override
    public Rectangle getRasterRegion() {
        return outputRegion;
    }

    @Override
    public void begin() throws IOException {
        final File parentFile = outputFile.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        productWriter.writeProductNodes(product, outputFile);
        initLine();
        this.yLast = 0;
    }

    @Override
    public void end() throws IOException {
        completeLine();
        for (Band band : customizerBands) {
            if (productWriter.shouldWrite(band)) {
                band.writeRasterDataFully(ProgressMonitor.NULL);
            }
        }
        productWriter.close();
        product.closeIO();
    }

    @Override
    public void renderBin(int x, int y, TemporalBin temporalBin, Vector outputVector) throws IOException {
        if (y != yLast) {
            completeLine();
            yLast = y;
        }
        setData(x, temporalBin, outputVector);
    }

    @Override
    public void renderMissingBin(int x, int y) throws IOException {
        if (y != yLast) {
            completeLine();
            yLast = y;
        }
        setNoData(x);
    }

    private void completeLine() throws IOException {
        writeLine(yLast);
        initLine();
    }

    private void writeLine(int y) throws IOException {
        if (numObsBand != null && numObsLine != null) {
            productWriter.writeBandRasterData(numObsBand, 0, y, rasterWidth, 1, numObsLine, ProgressMonitor.NULL);
        }
        if (numPassesBand != null && numPassesLine != null) {
            productWriter.writeBandRasterData(numPassesBand, 0, y, rasterWidth, 1, numPassesLine, ProgressMonitor.NULL);
        }
        for (int i = 0; i < outputBands.length; i++) {
            productWriter.writeBandRasterData(outputBands[i], 0, y, rasterWidth, 1, outputLines[i], ProgressMonitor.NULL);
        }
    }

    private void initLine() {
        for (int x = 0; x < rasterWidth; x++) {
            setNoData(x);
        }
    }

    private void setData(int x, TemporalBin temporalBin, Vector outputVector) {
        if (numObsLine != null) {
            numObsLine.setElemIntAt(x, temporalBin.getNumObs());
        }
        if (numPassesLine != null) {
            numPassesLine.setElemIntAt(x, temporalBin.getNumPasses());
        }
        for (int i = 0; i < outputBands.length; i++) {
            outputLines[i].setElemFloatAt(x, outputVector.get(i));
        }
    }

    private void setNoData(int x) {
        if (numObsLine != null) {
            numObsLine.setElemIntAt(x, -1);
        }
        if (numPassesLine != null) {
            numPassesLine.setElemIntAt(x, -1);
        }
        for (int i = 0; i < outputBands.length; i++) {
            outputLines[i].setElemFloatAt(x, fillValues[i]);
        }
    }

    private static CrsGeoCoding createMapGeoCoding(Rectangle outputRegion, double pixelSize) {
        CrsGeoCoding geoCoding;
        try {
            geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                         outputRegion.width,
                                         outputRegion.height,
                                         -180.0 + pixelSize * outputRegion.x,
                                         90.0 - pixelSize * outputRegion.y,
                                         pixelSize,
                                         pixelSize,
                                         0.0, 0.0);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
        return geoCoding;
    }
}
