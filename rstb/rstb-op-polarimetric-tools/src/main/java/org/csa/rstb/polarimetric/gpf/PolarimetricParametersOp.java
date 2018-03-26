/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.decompositions.hAAlpha;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
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
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compute polarimetric parameters for both quad-pol and compact-pol products.
 */

@OperatorMetadata(alias = "Polarimetric-Parameters",
        category = "Radar/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Compute general polarimetric parameters")
public final class PolarimetricParametersOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Use mean coherency or covariance matrix", defaultValue = "true", label = "Use Mean Matrix")
    private boolean useMeanMatrix = true;

    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size X")
    private String windowSizeXStr = "5";

    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size Y")
    private String windowSizeYStr = "5";

    @Parameter(description = "Output Span", defaultValue = "true", label = "Span")
    private boolean outputSpan = true;

    @Parameter(description = "Output pedestal height", defaultValue = "false", label = "Pedestal Height")
    private boolean outputPedestalHeight = false;
    @Parameter(description = "Output RVI", defaultValue = "false", label = "Radar Vegetation Index")
    private boolean outputRVI = false;
    @Parameter(description = "Output RFDI", defaultValue = "false", label = "Radar Forest Degradation Index")
    private boolean outputRFDI = false;

    @Parameter(description = "Output CSI", defaultValue = "false", label = "Canopy Structure Index")
    private boolean outputCSI = false;
    @Parameter(description = "Output VSI", defaultValue = "false", label = "Volume Scattering Index")
    private boolean outputVSI = false;
    @Parameter(description = "Output BMI", defaultValue = "false", label = "Biomass Index")
    private boolean outputBMI = false;
    @Parameter(description = "Output ITI", defaultValue = "false", label = "Interaction Index")
    private boolean outputITI = false;

    @Parameter(description = "Output Co-Pol HH/VV", defaultValue = "false", label = "HH/VV Ratio")
    private boolean outputHHVVRatio = false;
    @Parameter(description = "Output Cross-Pol HH/HV", defaultValue = "false", label = "HH/HV Ratio")
    private boolean outputHHHVRatio = false;
    @Parameter(description = "Output Cross-Pol VV/VH", defaultValue = "false", label = "VV/VH Ratio")
    private boolean outputVVVHRatio = false;

    private FilterWindow window;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private boolean isComplex;
    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;
    private Band hhBand = null, hvBand = null, vvBand = null, vhBand = null;

    private final static String PRODUCT_SUFFIX = "_PP";
    private enum BandType { Span, PedestalHeight, RVI, RFDI, CSI, VSI, BMI, ITI, HHVVRatio, HHHVRatio, VVVHRatio }

    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            isComplex = validator.isComplex();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            if (outputSpan || outputPedestalHeight || outputRVI) {
                if (sourceProductType == PolBandUtils.MATRIX.LCHCP || sourceProductType == PolBandUtils.MATRIX.RCHCP ||
                        sourceProductType == PolBandUtils.MATRIX.C2) {
                    throw new OperatorException("A quad-pol product is expected as input.");
                } else if (sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3 ||
                        sourceProductType == PolBandUtils.MATRIX.FULL) {
                    if(!isComplex && (outputSpan || outputPedestalHeight)) {
                        throw new OperatorException("A T3,C3, or quad-pol slc product is expected as input for span and pedistal height.");
                    }
                } else {
                    throw new OperatorException("A quad-pol product is expected as input.");
                }
            }

            for (Band srcBand : sourceProduct.getBands()) {
                String unit = srcBand.getUnit();
                String bandName = srcBand.getName().toUpperCase();
                if (unit.equals(Unit.INTENSITY)) {
                    if (bandName.contains("_HH")) {
                        hhBand = srcBand;
                    } else if (bandName.contains("_HV")) {
                        hvBand = srcBand;
                    } else if (bandName.contains("_VV")) {
                        vvBand = srcBand;
                    } else if (bandName.contains("_VH")) {
                        vhBand = srcBand;
                    }
                }
            }

            if ((outputHHVVRatio || outputCSI || outputBMI) && (hhBand == null || vvBand == null)) {
                throw new OperatorException("Input product containing HH and VV bands is required");
            }
            if ((outputRFDI || outputHHHVRatio) && (hhBand == null || hvBand == null)) {
                throw new OperatorException("Input product containing HH and HV bands is required");
            }
            if (outputVVVHRatio && (vvBand == null || vhBand == null)) {
                throw new OperatorException("Input product containing VV and VH bands is required");
            }
            if (outputVSI && (hhBand == null || vvBand == null || hvBand == null || vhBand == null)) {
                throw new OperatorException("Input product containing HH, VV, HV and VH bands is required");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            window = new FilterWindow(Integer.parseInt(windowSizeXStr), Integer.parseInt(windowSizeYStr));

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final String[] targetBandNames = getTargetBandNames();

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, targetBandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }

        if(targetProduct.getNumBands() == 0) {
            throw new OperatorException("No output bands selected");
        }
    }

    private String[] getTargetBandNames() {
        final List<String> targetBandNameList = new ArrayList<>(13);

        if (outputSpan) {
            targetBandNameList.add(BandType.Span.toString());
        }
        if (outputPedestalHeight) {
            targetBandNameList.add(BandType.PedestalHeight.toString());
        }
        if (outputRVI) {
            targetBandNameList.add(BandType.RVI.toString());
        }
        if (outputRFDI) {
            targetBandNameList.add(BandType.RFDI.toString());
        }
        if (outputCSI) {
            targetBandNameList.add(BandType.CSI.toString());
        }
        if (outputVSI) {
            targetBandNameList.add(BandType.VSI.toString());
        }
        if (outputBMI) {
            targetBandNameList.add(BandType.BMI.toString());
        }
        if (outputITI) {
            targetBandNameList.add(BandType.ITI.toString());
        }
        if (outputHHVVRatio) {
            targetBandNameList.add(BandType.HHVVRatio.toString());
        }
        if (outputHHHVRatio) {
            targetBandNameList.add(BandType.HHHVRatio.toString());
        }
        if (outputVVVHRatio) {
            targetBandNameList.add(BandType.VVVHRatio.toString());
        }

        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absRoot != null) {
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        }
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        final Rectangle sourceRectangle = window.getSourceTileRectangle(x0, y0, w, h, sourceImageWidth, sourceImageHeight);

        final boolean computePolarimetricParam = isComplex && (outputSpan || outputPedestalHeight || outputRVI);

        Tile hhTile = null, hvTile = null, vvTile = null, vhTile = null;
        if (hhBand != null) {
            hhTile = getSourceTile(hhBand, sourceRectangle);
        }
        if (hvBand != null) {
            hvTile = getSourceTile(hvBand, sourceRectangle);
        }
        if (vvBand != null) {
            vvTile = getSourceTile(vvBand, sourceRectangle);
        }
        if (vhBand != null) {
            vhTile = getSourceTile(vhBand, sourceRectangle);
        }

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int j = 0; j < bandList.srcBands.length; j++) {
                    final Band srcBand = bandList.srcBands[j];
                    sourceTiles[j] = getSourceTile(srcBand, sourceRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }
                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
                PolarimetricParameters param = null;

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    srcIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = trgIndex.getIndex(x);

                        if (computePolarimetricParam) {
                            if (useMeanMatrix) {
                                PolOpUtils.getMeanCoherencyMatrix(x, y,
                                                                  window.getHalfWindowSizeX(), window.getHalfWindowSizeY(),
                                                                  sourceImageWidth, sourceImageHeight, sourceProductType,
                                                                  srcIndex, dataBuffers, Tr, Ti);
                            } else {
                                PolOpUtils.getCoherencyMatrixT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Tr, Ti);
                            }

                            param = computePolarimetricParameters(Tr, Ti);
                        }

                        float hh=0, hv=0, vv=0, vh=0;
                        if (hhTile != null) {
                            hh = (float)Math.sqrt(hhTile.getSampleFloat(x, y));
                        }
                        if(hvTile != null) {
                            hv = (float)Math.sqrt(hvTile.getSampleFloat(x, y));
                        }
                        if(vvTile != null) {
                            vv = (float)Math.sqrt(vvTile.getSampleFloat(x, y));
                        }
                        if(vhTile != null) {
                            vh = (float)Math.sqrt(vhTile.getSampleFloat(x, y));
                        }

                        for (final TileData tileData : tileDataList) {

                            if (outputSpan && tileData.bandType.equals(BandType.Span)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) param.Span);
                            }
                            if (outputPedestalHeight && tileData.bandType.equals(BandType.PedestalHeight)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) param.PedestalHeight);
                            }
                            if (outputRVI && tileData.bandType.equals(BandType.RVI)) {
                                if(param == null) {
                                    tileData.dataBuffer.setElemFloatAt(tgtIdx, (8*hv)/(hh + vv + 2*hv));
                                } else {
                                    tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) param.RVI);
                                }
                            }

                            if (outputRFDI && tileData.bandType.equals(BandType.RFDI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (hh - hv) / (hh + hv));
                            }
                            if (outputCSI && tileData.bandType.equals(BandType.CSI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, vv / (vv + hh));
                            }
                            if (outputBMI && tileData.bandType.equals(BandType.BMI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (vv + hh) / 2.0f);
                            }
                            if (outputVSI && tileData.bandType.equals(BandType.VSI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (hv + vh) / (hh + vv + hv + vh));
                            }
                            if (outputITI && tileData.bandType.equals(BandType.ITI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, hh / vv);
                            }
                            if (outputHHVVRatio && tileData.bandType.equals(BandType.HHVVRatio)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, hh / vv);
                            }
                            if (outputHHHVRatio && tileData.bandType.equals(BandType.HHHVRatio)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, hh / hv);
                            }
                            if (outputVVVHRatio && tileData.bandType.equals(BandType.VVVHRatio)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, vv / vh);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;
        final BandType bandType;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
            this.bandType = BandType.valueOf(bandName);
        }
    }

    /**
     * Compute general polarimetric parameters for given coherency matrix.
     *
     * @param Tr Real part of the mean coherency matrix.
     * @param Ti Imaginary part of the mean coherency matrix.
     * @return The general polarimetric parameters.
     */
    public static PolarimetricParameters computePolarimetricParameters(final double[][] Tr, final double[][] Ti) {

        final PolarimetricParameters parameters = new PolarimetricParameters();

        parameters.Span = 2 * (Tr[0][0] + Tr[1][1] + Tr[2][2]);
        hAAlpha.HAAlpha data = hAAlpha.computeHAAlpha(Tr, Ti);
        parameters.PedestalHeight = data.lambda3 / data.lambda1;
        parameters.RVI = 4.0 * data.lambda3 / (data.lambda1 + data.alpha2 + data.lambda3);

        return parameters;
    }

    public static class PolarimetricParameters {
        public double Span;
        public double PedestalHeight;
        public double RVI;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PolarimetricParametersOp.class);
        }
    }
}