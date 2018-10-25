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
package org.esa.snap.unmixing;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.CsvReader;
import org.esa.snap.core.util.math.ConstrainedLSU;
import org.esa.snap.core.util.math.FullyConstrainedLSU;
import org.esa.snap.core.util.math.SpectralUnmixing;
import org.esa.snap.core.util.math.UnconstrainedLSU;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implements a spectral unmixing algorithm.
 */
@OperatorMetadata(alias = "Unmix",
        category = "Raster/Image Analysis",
        version = "1.0",
        authors = "Norman Fomferra, Helmut Schiller",
        copyright = "(c) 2007 by Brockmann Consult",
        description = "Performs a linear spectral unmixing.")
public class SpectralUnmixingOp extends Operator {

    private final String UC_LSU = "Unconstrained LSU";
    private final String C_LSU = "Constrained LSU";
    private final String FC_LSU = "Fully Constrained LSU";

    @SourceProduct(alias="source", description = "The source product.")
    Product sourceProduct;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    @Parameter(description = "The list of spectral bands providing the source spectrum.", alias = "sourceBands", itemAlias = "band", rasterDataNodeType = Band.class)
    String[] sourceBandNames;

    @Parameter(description = "The list of endmember spectra. Wavelengths must be given in nanometers.", itemAlias = "endmember")
    Endmember[] endmembers;

    @Parameter(description = "A text file containing (additional) endmembers in a table. Wavelengths must be given in nanometers.")
    File endmemberFile;

    @Parameter(description = "The unmixing model.", valueSet = {UC_LSU, C_LSU, FC_LSU}, defaultValue = C_LSU)
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
    private boolean computeTileMethodUsable;

    public SpectralUnmixingOp() {
        computeTileMethodUsable = true;
    }


    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }

    public Endmember[] getEndmembers() {
        return endmembers;
    }

    public void setEndmembers(Endmember[] endmembers) {
        this.endmembers = endmembers;
    }

    public File getEndmemberFile() {
        return endmemberFile;
    }

    public void setEndmemberFile(File endmemberFile) {
        this.endmemberFile = endmemberFile;
    }

    public String getUnmixingModelName() {
        return unmixingModelName;
    }

    public void setUnmixingModelName(String unmixingModelName) {
        this.unmixingModelName = unmixingModelName;
    }

    public String getAbundanceBandNameSuffix() {
        return abundanceBandNameSuffix;
    }

    public void setAbundanceBandNameSuffix(String abundanceBandNameSuffix) {
        this.abundanceBandNameSuffix = abundanceBandNameSuffix;
    }

    public String getErrorBandNameSuffix() {
        return errorBandNameSuffix;
    }

    public void setErrorBandNameSuffix(String errorBandNameSuffix) {
        this.errorBandNameSuffix = errorBandNameSuffix;
    }

    public boolean getComputeErrorBands() {
        return computeErrorBands;
    }

    public void setComputeErrorBands(boolean computeErrorBands) {
        this.computeErrorBands = computeErrorBands;
    }

    public double getMinBandwidth() {
        return minBandwidth;
    }

    public void setMinBandwidth(double minBandwidth) {
        this.minBandwidth = minBandwidth;
    }

    @Override
    public boolean canComputeTile() {
        return !computeErrorBands;
    }

    @Override
    public boolean canComputeTileStack() {
        return true;
    }

    @Override
    public void initialize() throws OperatorException {

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
        ensureSingleRasterSize(sourceBands);

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

        if (UC_LSU.equals(unmixingModelName)) {
            spectralUnmixing = new UnconstrainedLSU(lsuMatrixElements);
        } else if (C_LSU.equals(unmixingModelName)) {
            spectralUnmixing = new ConstrainedLSU(lsuMatrixElements);
        } else if (FC_LSU.equals(unmixingModelName)) {
            spectralUnmixing = new FullyConstrainedLSU(lsuMatrixElements);
        } else if (unmixingModelName == null) {
            spectralUnmixing = new UnconstrainedLSU(lsuMatrixElements);
        }

        int width = sourceBands[0].getRasterWidth();
        int height = sourceBands[0].getRasterHeight();

        targetProduct = new Product(sourceProduct.getName() + "_unmixed", "SpectralUnmixing", width, height);

        abundanceBands = new Band[numEndmembers];
        for (int i = 0; i < numEndmembers; i++) {
            abundanceBands[i] = targetProduct.addBand(endmembers[i].getName() + abundanceBandNameSuffix, ProductData.TYPE_FLOAT32);
        }

        if (computeErrorBands) {
            errorBands = new Band[numSourceBands];
            for (int i = 0; i < errorBands.length; i++) {
                final String erroBandName = sourceBands[i].getName() + errorBandNameSuffix;
                errorBands[i] = targetProduct.addBand(erroBandName, ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBands[i], errorBands[i]);
            }
            summaryErrorBand = targetProduct.addBand("summary_error", ProductData.TYPE_FLOAT32);
            summaryErrorBand.setDescription("Root mean square error");
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        if (sourceProduct.getSceneRasterSize().equals(targetProduct.getSceneRasterSize())) {
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        int i = getTargetBandIndex(targetTile);
        if (i == -1) {
            return;
        }
        Tile[] sourceRaster = getSourceTiles(rectangle);
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            double[][] ia = getLineSpectra(sourceRaster, rectangle, y);
            double[][] oa = unmix(ia);
            setAbundances(rectangle, targetTile, y, oa[i]);
            checkForCancellation();
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException {
        Tile[] abundanceTiles = new Tile[abundanceBands.length];
        Tile[] intensityTiles = getSourceTiles(targetTileRectangle);
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
                checkForCancellation();
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

    private Tile[] getSourceTiles(Rectangle rectangle) throws OperatorException {
        Tile[] sourceRaster = new Tile[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            sourceRaster[i] = getSourceTile(sourceBands[i], rectangle);
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
        return spectralUnmixing.unmix(ia);
    }

    private double[][] mix(double[][] ia) {
        return spectralUnmixing.mix(ia);
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

    private void loadEndmemberFile() throws OperatorException {
        try {
            try (FileReader fileReader = new FileReader(endmemberFile)) {
                List<Endmember> newEndmembers = readGraphs(fileReader);
                ArrayList<Endmember> list = new ArrayList<>();
                if (endmembers != null) {
                    list.addAll(Arrays.asList(endmembers));
                }
                list.addAll(newEndmembers);
                endmembers = list.toArray(new Endmember[list.size()]);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }
    private static List<Endmember> readGraphs(Reader reader) throws IOException {

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});
        List<Endmember> endmemberList = new ArrayList<>(5);
        List<double[]> dataRecords = new ArrayList<>(20);

        String[] headerRecord = csvReader.readRecord();
        while (true) {
            if (headerRecord.length < 2) {
                throw new IOException("Invalid format.");
            }
            String[] record = csvReader.readRecord();
            if (record == null) {
                break;
            }
            double[] dataRecord = toDoubles(record);
            if (dataRecord != null) {
                if (dataRecord.length != headerRecord.length) {
                    throw new IOException("Invalid format.");
                }
                dataRecords.add(dataRecord);
            } else {
                readGraphGroup(headerRecord, dataRecords, endmemberList);
                headerRecord = record;
            }
        }
        readGraphGroup(headerRecord, dataRecords, endmemberList);
        return endmemberList;
    }

    public static double[] toDoubles(String[] textRecord) throws IOException {
        double[] doubleRecord = new double[textRecord.length];
        for (int i = 0; i < textRecord.length; i++) {
            try {
                doubleRecord[i] = Double.valueOf(textRecord[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return doubleRecord;
    }

    private static void readGraphGroup(String[] headerRecord, List<double[]> dataRecords, List<Endmember> endmemberList) {
        if (dataRecords.size() > 0) {
            double[] xValues = new double[dataRecords.size()];
            for (int j = 0; j < dataRecords.size(); j++) {
                xValues[j] = dataRecords.get(j)[0];
            }
            double[] dataRecord0 = dataRecords.get(0);
            for (int i = 1; i < dataRecord0.length; i++) {
                double[] yValues = new double[dataRecords.size()];
                for (int j = 0; j < dataRecords.size(); j++) {
                    yValues[j] = dataRecords.get(j)[i];
                }
                endmemberList.add(new Endmember(headerRecord[i], xValues, yValues));
            }
        }
        dataRecords.clear();
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
