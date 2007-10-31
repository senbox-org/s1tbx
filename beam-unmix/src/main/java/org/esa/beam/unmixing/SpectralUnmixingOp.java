/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.unmixing;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.ConstrainedLSU;
import org.esa.beam.util.math.FullyConstrainedLSU;
import org.esa.beam.util.math.SpectralUnmixing;
import org.esa.beam.util.math.UnconstrainedLSU;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Implements a spectral unmixing algorithm.
 */
@OperatorMetadata(alias = "Unmix",
                  version = "1.0",
                  authors = "Norman Fomferra, Helmut Schiller",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Performs a linear spectral unmixing.")
public class SpectralUnmixingOp extends Operator {

    private final String TYPE_1 = "Unconstrained LSU";
    private final String TYPE_2 = "Constrained LSU";
    private final String TYPE_3 = "Fully Constrained LSU";

    @SourceProduct(description = "The source product.")
    Product sourceProduct;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    @Parameter(description = "The list of spectral bands providing the source spectrum.", alias = "sourceBands", itemAlias = "band")
    String[] sourceBandNames;

    @Parameter(description = "The list of endmember spectra. Wavelengths must be given in nanometers.", itemAlias = "endmember")
    Endmember[] endmembers;

    @Parameter(description = "A text file containing (additional) endmembers in a table. Wavelengths must be given in nanometers.")
    File endmemberFile;

    @Parameter(description = "The unmixing model.", valueSet = {TYPE_1, TYPE_2, TYPE_3}, defaultValue = TYPE_2)
    String unmixingModelName;

    @Parameter(description = "The suffix for the generated abundance band names (name = endmember + suffix).", pattern = "[a-zA-Z_0-9]*", notNull = true, defaultValue = "_abundance")
    String abundanceBandNameSuffix;

    @Parameter(description = "The suffix for the generated error band names (name = source + suffix).", pattern = "[a-zA-Z_0-9]*", notNull = true, defaultValue = "_error")
    String errorBandNameSuffix;

    @Parameter(description = "If 'true', error bands for all source bands will be generated.", defaultValue = "false")
    boolean computeErrorBands;

    @Parameter(description = "Minimum spectral bandwidth used for endmember wavelength matching.", defaultValue = "10.0", interval = "(0,*)", unit = "nm")
    double minBandwidth;

    private Band[] sourceBands;
    private Band[] abundanceBands;
    private Band[] errorBands;
    private Band summaryErrorBand;
    private SpectralUnmixing spectralUnmixing;

    @Override
    public void initialize() throws OperatorException {
        if (computeErrorBands) {
            deactivateComputeTileMethod();
        }


        if (endmemberFile != null) {
            loadEndmemberFile();
        }

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            Band[] bands = sourceProduct.getBands();
            ArrayList<String> bandNameList = new ArrayList<String>();
            for (Band band : bands) {
                if (band.getSpectralWavelength() > 0) {
                    bandNameList.add(band.getName());
                }
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        validateParameters();

        sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            String sourceBandName = sourceBandNames[i];
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            if (sourceBand.getSpectralWavelength() <= 0) {
                throw new OperatorException("Source band without spectral wavelength: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }

        int numSourceBands = sourceBands.length;
        int numEndmembers = endmembers.length;

        if (numSourceBands < numEndmembers) {
            throw new OperatorException("Number of source bands must be >= number of endmembers.");
        }

        double[][] lsuMatrixElements = new double[numSourceBands][numEndmembers];
        for (int j = 0; j < numEndmembers; j++) {
            Endmember endmember = endmembers[j];
            double[] wavelengths = endmember.getWavelengths();
            double[] radiations = endmember.getRadiations();
            for (int i = 0; i < numSourceBands; i++) {
                Band sourceBand = sourceBands[i];
                float wavelength = sourceBand.getSpectralWavelength();
                float bandwidth = sourceBand.getSpectralBandwidth();
                int k = findEndmemberSpectralIndex(wavelengths, wavelength, Math.max(bandwidth, minBandwidth));
                if (k == -1) {
                    throw new OperatorException(String.format("Band %s: No matching endmember wavelength found (%f nm)", sourceBand.getName(), wavelength));
                }
                lsuMatrixElements[i][j] = radiations[k];
            }
        }

        if (TYPE_1.equals(unmixingModelName)) {
            spectralUnmixing = new UnconstrainedLSU(new Matrix(lsuMatrixElements));
        } else if (TYPE_2.equals(unmixingModelName)) {
            spectralUnmixing = new ConstrainedLSU(new Matrix(lsuMatrixElements));
        } else if (TYPE_3.equals(unmixingModelName)) {
            spectralUnmixing = new FullyConstrainedLSU(new Matrix(lsuMatrixElements));
        } else if (unmixingModelName == null) {
            spectralUnmixing = new UnconstrainedLSU(new Matrix(lsuMatrixElements));
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName() + "_unmixed",
                                    "SpectralUnmixing", width, height);

        abundanceBands = new Band[numEndmembers];
        for (int i = 0; i < numEndmembers; i++) {
            abundanceBands[i] = targetProduct.addBand(endmembers[i].getName() + abundanceBandNameSuffix, ProductData.TYPE_FLOAT32);
        }

        if (computeErrorBands) {
            errorBands = new Band[numSourceBands];
            for (int i = 0; i < errorBands.length; i++) {
                final String erroBandName = sourceBands[i].getName() + errorBandNameSuffix;
                errorBands[i] = targetProduct.addBand(erroBandName, ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralAttributes(sourceBands[i], errorBands[i]);
            }
            summaryErrorBand = targetProduct.addBand("summary_error", ProductData.TYPE_FLOAT32);
            summaryErrorBand.setDescription("Root mean square error");
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        int i = getTargetBandIndex(targetTile);
        if (i == -1) {
            return;
        }
        Tile[] sourceRaster = getSourceTiles(rectangle, pm);
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            double[][] ia = getLineSpectra(sourceRaster, rectangle, y);
            double[][] oa = unmix(ia);
            setAbundances(rectangle, targetTile, y, oa[i]);
            checkForCancelation(pm);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException {
        Tile[] abundanceTiles = new Tile[abundanceBands.length];
        Tile[] intensityTiles = getSourceTiles(targetTileRectangle, pm);
        for (int i = 0; i < abundanceBands.length; i++) {
            abundanceTiles[i] = targetTiles.get(abundanceBands[i]);
        }
        Tile[] errorTiles = null;
        Tile summaryErrorTile = null;
        if (computeErrorBands) {
            errorTiles = new Tile[intensityTiles.length];
            for (int i = 0; i < errorTiles.length; i++) {
                errorTiles[i] = targetTiles.get(errorBands[i]);
            }
            summaryErrorTile = targetTiles.get(summaryErrorBand);
        }
        for (int y = targetTileRectangle.y; y < targetTileRectangle.y + targetTileRectangle.height; y++) {
            double[][] ia = getLineSpectra(intensityTiles, targetTileRectangle, y);
            double[][] oa = unmix(ia);
            for (int i = 0; i < abundanceBands.length; i++) {
                setAbundances(targetTileRectangle, abundanceTiles[i], y, oa[i]);
                checkForCancelation(pm);
            }
            if (computeErrorBands) {
                final double[][] ia2 = mix(oa);
                computeErrorTiles(targetTileRectangle, errorTiles, summaryErrorTile, y, ia, ia2);
            }
        }
    }

    private static void computeErrorTiles(Rectangle rectangle, Tile[] errorTilesc, Tile summaryErrorTile, int y, double[][] ia, double[][] ia2) {
        final double[] errSqrSumRow = new double[rectangle.width];
        for (int i = 0; i < errorTilesc.length; i++) {
            final Tile errorTile = errorTilesc[i];
            final double[] iaRow = ia[i];
            final double[] ia2Row = ia2[i];
            double err;
            int j = 0;
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                err = iaRow[j] - ia2Row[j];
                errorTile.setSample(x, y, err);
                errSqrSumRow[j] += err * err;
                j++;
            }
        }
        if (summaryErrorTile != null) {
            int j = 0;
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                summaryErrorTile.setSample(x, y, Math.sqrt(errSqrSumRow[j] / errorTilesc.length));
                j++;
            }
        }
    }

    private Tile[] getSourceTiles(Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        Tile[] sourceRaster = new Tile[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            sourceRaster[i] = getSourceTile(sourceBands[i], rectangle, pm);
        }
        return sourceRaster;
    }

    private static void setAbundances(Rectangle rectangle, Tile targetTile, int y, double[] oaRow) {
        int i = 0;
        for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
            targetTile.setSample(x, y, oaRow[i]);
            i++;
        }
    }


    private double[][] getLineSpectra(Tile[] sourceRasters, Rectangle rectangle, int y) throws OperatorException {
        double[][] ia = new double[sourceBands.length][rectangle.width];
        for (int i = 0; i < sourceBands.length; i++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                ia[i][x - rectangle.x] = sourceRasters[i].getSampleDouble(x, y);
            }
        }
        return ia;
    }

    private int getTargetBandIndex(Tile targetTile) {
        int index = -1;
        for (int i = 0; i < abundanceBands.length; i++) {
            Band targetBand = abundanceBands[i];
            if (targetTile.getRasterDataNode() == targetBand) {
                index = i;
                break;
            }
        }
        return index;
    }

    private double[][] unmix(double[][] ia) {
        Matrix im = new Matrix(ia);
        Matrix om = spectralUnmixing.unmix(im);
        return om.getArray();
    }

    private double[][] mix(double[][] ia) {
        Matrix im = new Matrix(ia);
        Matrix om = spectralUnmixing.mix(im);
        return om.getArray();
    }

    public static int findEndmemberSpectralIndex(double[] endmemberWavelengths, double sourceBandWavelength, double maxBandwidth) {
        double minDelta = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < endmemberWavelengths.length; i++) {
            final double delta = Math.abs(endmemberWavelengths[i] - sourceBandWavelength);
            if (delta <= maxBandwidth && delta <= minDelta) {
                minDelta = delta;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public static Endmember[] convertGraphsToEndmembers(DiagramGraph[] diagramGraphs) {
        Endmember[] endmembers = new Endmember[diagramGraphs.length];
        for (int i = 0; i < diagramGraphs.length; i++) {
            DiagramGraph diagramGraph = diagramGraphs[i];
            int numValues = diagramGraph.getNumValues();
            double[] wavelengths = new double[numValues];
            double[] radiations = new double[numValues];
            for (int j = 0; j < numValues; j++) {
                wavelengths[j] = diagramGraph.getXValueAt(j);
                radiations[j] = diagramGraph.getYValueAt(j);
            }
            endmembers[i] = new Endmember(diagramGraph.getYName(), wavelengths, radiations);
        }
        return endmembers;
    }


    private void loadEndmemberFile() throws OperatorException {
        try {
            FileReader fileReader = new FileReader(endmemberFile);
            try {
                DiagramGraph[] diagramGraphs = DiagramGraphIO.readGraphs(fileReader);
                Endmember[] newEndmembers = convertGraphsToEndmembers(diagramGraphs);
                ArrayList<Endmember> list = new ArrayList<Endmember>();
                if (endmembers != null) {
                    list.addAll(Arrays.asList(endmembers));
                }
                list.addAll(Arrays.asList(newEndmembers));
                endmembers = list.toArray(new Endmember[list.size()]);
            } finally {
                fileReader.close();
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private void validateParameters() throws OperatorException {
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            throw new OperatorException("Parameter 'sourceBandNames' not set.");
        }
        if (endmemberFile == null && (endmembers == null || endmembers.length == 0)) {
            throw new OperatorException("Parameter 'endmemberFile' and 'endmembers' not set.");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SpectralUnmixingOp.class);
        }
    }
}
