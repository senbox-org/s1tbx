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
import org.esa.s1tbx.io.PolBandUtils;
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
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
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

    @Parameter(description = "Output Span", defaultValue = "true",
            label = "Span")
    private boolean outputSpan = true;

    @Parameter(description = "Output pedestal height", defaultValue = "false",
            label = "Pedestal Height")
    private boolean outputPedestalHeight = false;

    @Parameter(description = "Output RVI", defaultValue = "false",
            label = "Radar Vegetation Index")
    private boolean outputRVI = false;

    @Parameter(description = "Output RFDI", defaultValue = "false",
            label = "Radar Forest Degradation Index")
    private boolean outputRFDI = false;

    @Parameter(description = "Output HH/HV", defaultValue = "false",
            label = "HH/HV Ratio")
    private boolean outputHHHVRatio = false;

    private int windowSizeX = 0;
    private int windowSizeY = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean isCompactPol = false;
    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;
    private Band hhBand = null, hvBand = null;

    /**
     * Set output parameter. This function is used by unit test only.
     *
     * @param s The output parameter string.
     */
    public void SetOutputParameter(final String s) {

        switch (s) {
            case "Span":
                outputSpan = true;
                break;
            case "Pedestal Height":
                outputPedestalHeight = true;
                break;
            case "RVI":
                outputRVI = true;
                break;
            case "RFDI":
                outputRFDI = true;
                break;
            case "HHHVRatio":
                outputHHHVRatio = true;
                break;
            default:
                throw new OperatorException(s + " is an invalid parameter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            if (outputSpan || outputPedestalHeight || outputRVI) {
                if (sourceProductType == PolBandUtils.MATRIX.LCHCP || sourceProductType == PolBandUtils.MATRIX.RCHCP ||
                        sourceProductType == PolBandUtils.MATRIX.C2) {
                    isCompactPol = true;
                } else if (sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3 ||
                        sourceProductType == PolBandUtils.MATRIX.FULL) {
                    isCompactPol = false;
                } else {
                    throw new OperatorException("A quad-pol product is expected.");
                }
            }
            if(isCompactPol) {
                throw new OperatorException("A quad-pol product is expected.");
            }

            if (outputRFDI || outputHHHVRatio) {
                for (Band srcBand : sourceProduct.getBands()) {
                    if (srcBand.getUnit().equals(Unit.INTENSITY)) {
                        if (srcBand.getName().toUpperCase().contains("HH")) {
                            hhBand = srcBand;
                        } else if (srcBand.getName().toUpperCase().contains("HV")) {
                            hvBand = srcBand;
                        }
                        if (hhBand != null && hvBand != null) {
                            break;
                        }
                    }
                }
            }

            if (!outputSpan && !outputPedestalHeight && !outputRVI && !outputRFDI && !outputHHHVRatio) {
                throw new OperatorException("Please select parameters to output.");
            }
            if ((outputRFDI || outputHHHVRatio) && (hhBand == null || hvBand == null)) {
                throw new OperatorException("Input product containing HH and HV bands is required");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            windowSizeX = Integer.parseInt(windowSizeXStr);
            windowSizeY = Integer.parseInt(windowSizeYStr);
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

        targetProduct = new Product(sourceProduct.getName(),
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
    }

    private String[] getTargetBandNames() {
        final List<String> targetBandNameList = new ArrayList<>(13);

        if (outputSpan) {
            targetBandNameList.add("Span");
        }
        if (outputPedestalHeight) {
            targetBandNameList.add("PedestalHeight");
        }
        if (outputRVI) {
            targetBandNameList.add("RVI");
        }
        if (outputRFDI) {
            targetBandNameList.add("RFDI");
        }
        if (outputHHHVRatio) {
            targetBandNameList.add("HHHVRatio");
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

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
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

        final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h, windowSizeX, windowSizeY);
        final int halfWindowSize = windowSizeX / 2;

        final boolean computePolarimetricParam = outputSpan || outputPedestalHeight || outputRVI;

        Tile hhTile = null, hvTile = null;
        if (hhBand != null) {
            hhTile = getSourceTile(hhBand, sourceRectangle);
        }
        if (hvBand != null) {
            hvTile = getSourceTile(hvBand, sourceRectangle);
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
                PolOpUtils.PolarimetricParameters cp = null;

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    srcIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = trgIndex.getIndex(x);

                        if (computePolarimetricParam) {
                            if (useMeanMatrix) {
                                PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, halfWindowSize, sourceImageWidth,
                                                                  sourceImageHeight, sourceProductType, srcIndex, dataBuffers, Tr, Ti);
                            } else {
                                PolOpUtils.getT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Tr, Ti);
                            }

                            cp = PolOpUtils.computePolarimetricParameters(Tr, Ti);
                        }

                        for (final TileData tileData : tileDataList) {

                            if (outputSpan && tileData.bandName.equals("Span")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cp.Span);
                            }
                            if (outputPedestalHeight && tileData.bandName.equals("PedestalHeight")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cp.PedestalHeight);
                            }
                            if (outputRVI && tileData.bandName.equals("RVI")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cp.RVI);
                            }
                            if (hhTile != null && hvTile != null) {
                                final float hh = hhTile.getSampleFloat(x, y);
                                final float hv = hvTile.getSampleFloat(x, y);

                                if (outputRFDI && tileData.bandName.equals("RFDI")) {
                                    tileData.dataBuffer.setElemFloatAt(tgtIdx, (hh - hv) / (hh + hv));
                                }
                                if (outputHHHVRatio && tileData.bandName.equals("HHHVRatio")) {
                                    tileData.dataBuffer.setElemFloatAt(tgtIdx, hh / hv);
                                }
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0          X coordinate of pixel at the upper left corner of the target tile.
     * @param y0          Y coordinate of pixel at the upper left corner of the target tile.
     * @param w           The width of the target tile.
     * @param h           The height of the target tile.
     * @param windowSizeX The sliding window width.
     * @param windowSizeY The sliding window height.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h,
                                             final int windowSizeX, final int windowSizeY) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;
        final int halfWindowSizeX = windowSizeX / 2;
        final int halfWindowSizeY = windowSizeY / 2;

        if (x0 >= halfWindowSizeX) {
            sx0 -= halfWindowSizeX;
            sw += halfWindowSizeX;
        }

        if (y0 >= halfWindowSizeY) {
            sy0 -= halfWindowSizeY;
            sh += halfWindowSizeY;
        }

        if (x0 + w + halfWindowSizeX <= sourceImageWidth) {
            sw += halfWindowSizeX;
        }

        if (y0 + h + halfWindowSizeY <= sourceImageHeight) {
            sh += halfWindowSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
        }
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