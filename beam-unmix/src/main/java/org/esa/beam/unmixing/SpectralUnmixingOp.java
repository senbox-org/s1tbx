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
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.ConstrainedLSU;
import org.esa.beam.util.math.FullyConstrainedLSU;
import org.esa.beam.util.math.SpectralUnmixing;
import org.esa.beam.util.math.UnconstrainedLSU;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The "Unmix" operator implementation.
 */
public class SpectralUnmixingOp extends AbstractOperator implements ParameterConverter {

    private final String TYPE_1 = "Unconstrained LSU";
    private final String TYPE_2 = "Constrained LSU";
    private final String TYPE_3 = "Fully Constrained LSU";

    @SourceProduct
    Product sourceProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter
    boolean alterSourceProduct;

    @Parameter
    String[] sourceBandNames;

    @Parameter
    Endmember[] endmembers;

    @Parameter
    File endmemberFile;

    @Parameter(valueSet = {TYPE_1, TYPE_2, TYPE_3}, defaultValue = TYPE_2)
    String unmixingModelName;

    @Parameter(pattern = "[a-zA-Z_0-9]*", notNull = true, defaultValue = "_abundance")
    String targetBandNameSuffix;

    @Parameter(defaultValue = "1.0e-2")
    double epsilon;

    private transient Band[] sourceBands;
    private transient Band[] targetBands;
    private SpectralUnmixing spectralUnmixing;

    public SpectralUnmixingOp(OperatorSpi spi) {
        super(spi);
    }

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        configureSourceBands(configuration);
        configureEndmembers(configuration);
    }

    private void configureEndmembers(Xpp3Dom configuration) throws OperatorException {
        Xpp3Dom endmembersElement = configuration.getChild("endmembers");
        Xpp3Dom wavelengthsElement = endmembersElement.getChild("wavelengths");
        if (wavelengthsElement == null) {
            throw new OperatorException("Missing 'endmembers/wavelengths' element.");
        }
        double[] wavelengths = StringUtils.toDoubleArray(wavelengthsElement.getValue(), ",");

        Xpp3Dom[] endmemberElements = endmembersElement.getChildren("endmember");
        if (endmemberElements.length == 0) {
            throw new OperatorException("Missing 'endmembers/endmember' elements.");
        }
        endmembers = new Endmember[endmemberElements.length];
        for (int i = 0; i < endmemberElements.length; i++) {
            Xpp3Dom endmemberElement = endmemberElements[i];
            Xpp3Dom endmemberName = endmemberElement.getChild("name");
            if (endmemberName == null) {
                throw new OperatorException("Missing 'endmembers/endmember/name' element.");
            }
            Xpp3Dom radiationsElement = endmemberElement.getChild("radiations");
            if (radiationsElement == null) {
                throw new OperatorException("Missing 'endmembers/endmember/radiations' element.");
            }
            double[] radiations = StringUtils.toDoubleArray(radiationsElement.getValue(), ",");
            if (radiations.length != wavelengths.length) {
                throw new OperatorException("'Endmember number of wavelengths does not match number of radiations.");
            }
            endmembers[i] = new Endmember(endmemberName.getValue(), wavelengths, radiations);
        }
    }

    private void configureSourceBands(Xpp3Dom configuration) {
        Xpp3Dom sourceBandsElement = configuration.getChild("sourceBands");
        Xpp3Dom[] bandElements = sourceBandsElement.getChildren("band");
        sourceBandNames = new String[bandElements.length];
        for (int i = 0; i < bandElements.length; i++) {
            sourceBandNames[i] = bandElements[i].getValue();
        }
    }

    @Override
    protected Product initialize(ProgressMonitor pm) throws OperatorException {
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
            sourceBandNames = bandNameList.toArray(new String[0]);
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

    private void loadEndmemberFile() throws OperatorException {
        try {
            FileReader fileReader = new FileReader(endmemberFile);
            try {
                DiagramGraph[] diagramGraphs = DiagramGraphIO.readGraphs(fileReader);
                Endmember[] newEndmembers = convertGraphsToEndmembers(diagramGraphs);
                ArrayList<Endmember> list = new ArrayList<Endmember>();
                if (endmembers != null) {
                    for (Endmember endmember : endmembers) {
                        list.add(endmember);
                    }
                }
                for (Endmember endmember : newEndmembers) {
                    list.add(endmember);
                }
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

    @Override
    public void computeBand(Band band, Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetRaster.getRectangle();
        int j = getTargetBandIndex(targetRaster);
        if (j == -1) {
            return;
        }
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            double[][] ia = getLineSpectra(rectangle, y);
            double[][] oa = unmix(ia);
            setAbundances(rectangle, targetRaster, y, j, oa);
        }
    }

    @Override
    public void computeAllBands(Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException {
        for (int y = targetTileRectangle.y; y < targetTileRectangle.y + targetTileRectangle.height; y++) {
            double[][] ia = getLineSpectra(targetTileRectangle, y);
            double[][] oa = unmix(ia);
            for (int j = 0; j < targetBands.length; j++) {
                Raster targetRaster = getRaster(targetBands[j], targetTileRectangle);
                setAbundances(targetTileRectangle, targetRaster, y, j, oa);
            }
        }
    }

    private void setAbundances(Rectangle rectangle, Raster targetRaster, int y, int j, double[][] oa) {
        for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
            targetRaster.setDouble(x, y, oa[j][x - rectangle.x]);
        }
    }

    private double[][] getLineSpectra(Rectangle rectangle, int y) throws OperatorException {
        double[][] ia = new double[sourceBands.length][rectangle.width];
        for (int i = 0; i < sourceBands.length; i++) {
            Raster sourceRaster = getRaster(sourceBands[i], rectangle);
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                ia[i][x - rectangle.x] = sourceRaster.getDouble(x, y);
            }
        }
        return ia;
    }

    private int getTargetBandIndex(Raster targetRaster) {
        int j0 = -1;
        for (int j = 0; j < targetBands.length; j++) {
            Band targetBand = targetBands[j];
            if (targetRaster.getRasterDataNode() == targetBand) {
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


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(SpectralUnmixingOp.class, "SpectralUnmixing");
        }
    }


}
