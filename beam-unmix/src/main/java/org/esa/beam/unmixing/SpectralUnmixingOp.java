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
import org.esa.beam.framework.gpf.*;
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
 * Implements a spectral umnixing algorithm.
 */
@OperatorMetadata(alias = "SpectralUnmixing",
                  version = "1.0",
                  authors = "Helmut Schiller, Norman Fomferra",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Spectral umnixing algorithm.")
public class SpectralUnmixingOp extends Operator {

    private final String TYPE_1 = "Unconstrained LSU";
    private final String TYPE_2 = "Constrained LSU";
    private final String TYPE_3 = "Fully Constrained LSU";

    @SourceProduct
    Product sourceProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter
    boolean alterSourceProduct;

    @Parameter(alias = "sourceBands", elemAlias = "band")
    String[] sourceBandNames;

    @Parameter(xmlConverter = EndmembersXmlConverter.class)
    Endmember[] endmembers;

    @Parameter
    File endmemberFile;

    @Parameter(valueSet = {TYPE_1, TYPE_2, TYPE_3}, defaultValue = TYPE_2)
    String unmixingModelName;

    @Parameter(pattern = "[a-zA-Z_0-9]*", notNull = true, defaultValue = "_abundance")
    String targetBandNameSuffix;

    @Parameter(defaultValue = "10.0", description = "Error used for wavelength matching.")
    double epsilon;

    private transient Band[] sourceBands;
    private transient Band[] targetBands;
    private transient SpectralUnmixing spectralUnmixing;

    @Override
    public Product initialize() throws OperatorException {
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
            sourceBands[i] = sourceBand;
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        if (alterSourceProduct) {
            targetProduct = sourceProduct;
        } else {
            targetProduct = new Product(sourceProduct.getName() + "_unmixed",
                                        "SpectralUnmixing", width, height);
            ProductUtils.copyMetadata(sourceProduct, targetProduct);
        }

        int numSourceBands = sourceBands.length;
        int numEndmembers = endmembers.length;

        targetBands = new Band[numEndmembers];
        for (int j = 0; j < numEndmembers; j++) {
            targetBands[j] = targetProduct.addBand(endmembers[j].getName() + targetBandNameSuffix, ProductData.TYPE_FLOAT32);
        }

        if (sourceProduct != targetProduct) {
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        }

        double[][] doubles = new double[numSourceBands][numEndmembers];
        for (int j = 0; j < numEndmembers; j++) {
            Endmember endmember = endmembers[j];
            double[] wavelengths = endmember.getWavelengths();
            double[] radiations = endmember.getRadiations();
            for (int i = 0; i < numSourceBands; i++) {
                Band sourceBand = sourceBands[i];
                float wavelength = sourceBand.getSpectralWavelength();
                int k = findValueIndex(wavelengths, wavelength);
                if (k == -1) {
                    throw new OperatorException(String.format("Band %s: No matching endmember wavelength found (%f nm)", sourceBand.getName(), wavelength));
                }
                doubles[i][j] = radiations[k];
            }
        }

        if (TYPE_1.equals(unmixingModelName)) {
            spectralUnmixing = new UnconstrainedLSU(new Matrix(doubles));
        } else if (TYPE_2.equals(unmixingModelName)) {
            spectralUnmixing = new ConstrainedLSU(new Matrix(doubles));
        } else if (TYPE_3.equals(unmixingModelName)) {
            spectralUnmixing = new FullyConstrainedLSU(new Matrix(doubles));
        } else if (unmixingModelName == null) {
            spectralUnmixing = new UnconstrainedLSU(new Matrix(doubles));
        }
        return targetProduct;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        int j = getTargetBandIndex(targetTile);
        if (j == -1) {
            return;
        }
        Tile[] sourceRaster = getSourceTiles(rectangle, pm);
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            double[][] ia = getLineSpectra(sourceRaster, rectangle, y);
            double[][] oa = unmix(ia);
            setAbundances(rectangle, targetTile, y, j, oa);
            checkForCancelation(pm);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException {
        Tile[] targetRaster = new Tile[targetBands.length];
        Tile[] sourceRaster = getSourceTiles(targetTileRectangle, pm);
        for (int j = 0; j < targetBands.length; j++) {
            targetRaster[j] = targetTiles.get(targetBands[j]);
        }
        for (int y = targetTileRectangle.y; y < targetTileRectangle.y + targetTileRectangle.height; y++) {
            double[][] ia = getLineSpectra(sourceRaster, targetTileRectangle, y);
            double[][] oa = unmix(ia);
            for (int j = 0; j < targetBands.length; j++) {
                setAbundances(targetTileRectangle, targetRaster[j], y, j, oa);
                checkForCancelation(pm);
            }
        }
    }

    @Override
    public ParameterConverter getConfigurationConverter() {
        return new SpectralUnmixingConfigConverter();
    }

    private Tile[] getSourceTiles(Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        Tile[] sourceRaster = new Tile[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            sourceRaster[i] = getSourceTile(sourceBands[i], rectangle, pm);
        }
        return sourceRaster;
    }

    private void setAbundances(Rectangle rectangle, Tile targetTile, int y, int j, double[][] oa) {
        for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
            targetTile.setSample(x, y, oa[j][x - rectangle.x]);
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
        int j0 = -1;
        for (int j = 0; j < targetBands.length; j++) {
            Band targetBand = targetBands[j];
            if (targetTile.getRasterDataNode() == targetBand) {
                j0 = j;
                break;
            }
        }
        return j0;
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

    private int findValueIndex(double[] values, double value) {
        return findValueIndex(values, value, epsilon);
    }

    private static int findValueIndex(double[] values, double value, double epsilon) {
        for (int i = 0; i < values.length; i++) {
            double wavelength = values[i];
            if (Math.abs(wavelength - value) <= epsilon) {
                return i;
            }
        }
        return -1;
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
