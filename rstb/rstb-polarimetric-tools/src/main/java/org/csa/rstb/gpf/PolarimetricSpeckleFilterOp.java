/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf;

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
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Applies a Polarimetric Speckle Filter to the data (covariance/coherency matrix data)
 */
@OperatorMetadata(alias="Polarimetric-Speckle-Filter",
        category = "Polarimetric Tools",
        description = "Polarimetric Speckle Reduction")
public class PolarimetricSpeckleFilterOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {BOXCAR_SPECKLE_FILTER, IDAN_FILTER, REFINED_LEE_FILTER, LEE_SIGMA_FILTER},
               defaultValue = REFINED_LEE_FILTER, label = "Filter")
    private String filter;

    @Parameter(description = "The boxcar filter size", interval = "(1, 100]", defaultValue = "5", label="Filter Size")
    private int filterSize = 5;

    @Parameter(valueSet = {NUM_LOOKS_1, NUM_LOOKS_2, NUM_LOOKS_3, NUM_LOOKS_4},
               defaultValue = NUM_LOOKS_1, label = "Window Size")
    private String numLooksStr = NUM_LOOKS_1;
    
    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
               defaultValue = WINDOW_SIZE_7x7, label = "Window Size")
    private String windowSize = WINDOW_SIZE_7x7; // window size for all filters except Lee Sigma filter

    @Parameter(valueSet = {WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
               defaultValue = WINDOW_SIZE_9x9, label = "Filter Window Size")
    private String filterWindowSizeStr = WINDOW_SIZE_9x9; // filter window size for Lee Sigma filter

    @Parameter(valueSet = {WINDOW_SIZE_3x3, WINDOW_SIZE_5x5}, defaultValue = WINDOW_SIZE_3x3,
               label = "Point target window Size")
    private String targetWindowSizeStr = WINDOW_SIZE_3x3; // window size for point target determination in Lee sigma

    @Parameter(description = "The Adaptive Neighbourhood size", interval = "(1, 200]", defaultValue = "50",
                label = "Adaptive Neighbourhood Size")
    private int anSize = 50;

    @Parameter(valueSet = {SIGMA_50_PERCENT, SIGMA_60_PERCENT, SIGMA_70_PERCENT, SIGMA_80_PERCENT, SIGMA_90_PERCENT},
               defaultValue = SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = SIGMA_90_PERCENT; // sigma value in Lee sigma

    private PolBandUtils.QuadSourceBand[] srcBandList;
    private int halfFilterSize = 0;
    private int convSize;
    private int numLooks;

    // parameters for Lee sigma filter
    private double I1, I2; // sigma range
    private int sigma;
    private double sigmaVP; // revised sigmaV used in MMSE filter
    private double sigmaVPSqr;
    private int filterWindowSize = 0;
    private int targetWindowSize = 0;
    private int halfTargetWindowSize = 0;
    private int targetSize = 5;

    private double sigmaV;
    private double sigmaVSqr;

    private int stride = 0;
    private int subWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private PolBandUtils.MATRIX sourceProductType = null;

    private static final double NonValidPixelValue = -1.0;

    public static final String BOXCAR_SPECKLE_FILTER = "Box Car Filter";
    public static final String REFINED_LEE_FILTER = "Refined Lee Filter";
    public static final String IDAN_FILTER = "IDAN Filter";
    public static final String LEE_SIGMA_FILTER = "Improved Lee Sigma Filter";

    public static final String WINDOW_SIZE_3x3 = "3x3";
    public static final String WINDOW_SIZE_5x5 = "5x5";
    public static final String WINDOW_SIZE_7x7 = "7x7";
    public static final String WINDOW_SIZE_9x9 = "9x9";
    public static final String WINDOW_SIZE_11x11 = "11x11";

    public static final String SIGMA_50_PERCENT = "0.5";
    public static final String SIGMA_60_PERCENT = "0.6";
    public static final String SIGMA_70_PERCENT = "0.7";
    public static final String SIGMA_80_PERCENT = "0.8";
    public static final String SIGMA_90_PERCENT = "0.9";

    public static final String NUM_LOOKS_1 = "1";
    public static final String NUM_LOOKS_2 = "2";
    public static final String NUM_LOOKS_3 = "3";
    public static final String NUM_LOOKS_4 = "4";

    public enum T3Elem {
        T11, T12_real, T12_imag, T13_real, T13_imag, T22, T23_real, T23_imag, T33
    }

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public PolarimetricSpeckleFilterOp() {
    }

    /**
     * Set speckle filter. This function is used by unit test only.
     * @param s The filter name.
     */
    public void SetFilter(final String s) {

        if (s.equals(BOXCAR_SPECKLE_FILTER) || s.equals(IDAN_FILTER) ||
            s.equals(REFINED_LEE_FILTER) || s.equals(LEE_SIGMA_FILTER)) {
            filter = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            getSourceImageDimension();

            setParameters();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Set filter size in case of refined Lee filter.
     */
    private void setParameters() {
        if (filter.equals(BOXCAR_SPECKLE_FILTER)) {
            setBoxcarParameters();
        } else if(filter.equals(REFINED_LEE_FILTER)) {
            setRefinedLeeParameters();
        } else if (filter.equals(IDAN_FILTER)) {
            setIDANParameters();
        } else if (filter.equals(LEE_SIGMA_FILTER)) {
            setLeeSigmaParameters();
        }
    }

    private void setBoxcarParameters() {
        halfFilterSize = filterSize/2;
    }

    private void setRefinedLeeParameters() {

        setNumLooks();

        if (windowSize.equals(WINDOW_SIZE_5x5)) {
            filterSize = 5;
            subWindowSize = 3;
            stride = 1;
        } else if (windowSize.equals(WINDOW_SIZE_7x7)) {
            filterSize = 7;
            subWindowSize = 3;
            stride = 2;
        } else if (windowSize.equals(WINDOW_SIZE_9x9)) {
            filterSize = 9;
            subWindowSize = 5;
            stride = 2;
        } else if (windowSize.equals(WINDOW_SIZE_11x11)) {
            filterSize = 11;
            subWindowSize = 5;
            stride = 3;
        } else {
            throw new OperatorException("Unknown window size: " + windowSize);
        }

        halfFilterSize = filterSize/2;
        convSize = filterSize*(halfFilterSize + 1);
        sigmaV = 1.0 / Math.sqrt(numLooks);
        sigmaVSqr = sigmaV*sigmaV;
    }

    private void setIDANParameters() {

        setNumLooks();
        // fileterSize in this case is used only in generating source rectangle
        filterSize = anSize*2;
        halfFilterSize = filterSize/2;
        sigmaV = 1.0 / Math.sqrt(numLooks);
        sigmaVSqr = sigmaV*sigmaV;
    }

    private void setLeeSigmaParameters() {

        setNumLooks();

        if (sigmaStr.equals(SIGMA_50_PERCENT)) {
            sigma = 5;
        } else if (sigmaStr.equals(SIGMA_60_PERCENT)) {
            sigma = 6;
        } else if (sigmaStr.equals(SIGMA_70_PERCENT)) {
            sigma = 7;
        } else if (sigmaStr.equals(SIGMA_80_PERCENT)) {
            sigma = 8;
        } else if (sigmaStr.equals(SIGMA_90_PERCENT)) {
            sigma = 9;
        } else {
            throw new OperatorException("Unknown sigma: " + sigmaStr);
        }

        if (filterWindowSizeStr.equals(WINDOW_SIZE_7x7)) {
            filterWindowSize = 7;
        } else if (filterWindowSizeStr.equals(WINDOW_SIZE_9x9)) {
            filterWindowSize = 9;
        } else if (filterWindowSizeStr.equals(WINDOW_SIZE_11x11)) {
            filterWindowSize = 11;
        } else {
            throw new OperatorException("Unknown filter window size: " + filterWindowSizeStr);
        }

        if (targetWindowSizeStr.equals(WINDOW_SIZE_3x3)) {
            targetWindowSize = 3;
        } else if (targetWindowSizeStr.equals(WINDOW_SIZE_5x5)) {
            targetWindowSize = 5;
        } else {
            throw new OperatorException("Unknown target window size: " + targetWindowSizeStr);
        }

        halfFilterSize = filterWindowSize/2;
        halfTargetWindowSize = targetWindowSize/2;
        sigmaV = 1.0/Math.sqrt(numLooks);
        sigmaVSqr = sigmaV*sigmaV;

        setSigmaRange();
    }

    private void setNumLooks() {
        numLooks = Integer.parseInt(numLooksStr);
    }

    private void setSigmaRange() {

        if (numLooks == 1) {

			if (sigma == 5) {
				I1 = 0.436;
				I2 = 1.920;
				sigmaVP = 0.4057;
			} else if (sigma == 6) {
				I1 = 0.343;
				I2 = 2.210;
				sigmaVP = 0.4954;
			} else if (sigma == 7) {
				I1 = 0.254;
				I2 = 2.582;
				sigmaVP = 0.5911;
			} else if (sigma == 8) {
				I1 = 0.168;
				I2 = 3.094;
				sigmaVP = 0.6966;
			} else if (sigma == 9) {
				I1 = 0.084;
				I2 = 3.941;
				sigmaVP = 0.8191;
			}

		} else if (numLooks == 2) {

			if (sigma == 5) {
				I1 = 0.582;
				I2 = 1.584;
				sigmaVP = 0.2763;
			} else if (sigma == 6) {
				I1 = 0.501;
				I2 = 1.755;
				sigmaVP = 0.3388;
			} else if (sigma == 7) {
				I1 = 0.418;
				I2 = 1.972;
				sigmaVP = 0.4062;
			} else if (sigma == 8) {
				I1 = 0.327;
				I2 = 2.260;
				sigmaVP = 0.4810;
			} else if (sigma == 9) {
				I1 = 0.221;
				I2 = 2.744;
				sigmaVP = 0.5699;
			}

		} else if (numLooks == 3) {

			if (sigma == 5) {
				I1 = 0.652;
				I2 = 1.458;
				sigmaVP = 0.2222;
			} else if (sigma == 6) {
				I1 = 0.580;
				I2 = 1.586;
				sigmaVP = 0.2736;
			} else if (sigma == 7) {
				I1 = 0.505;
				I2 = 1.751;
				sigmaVP = 0.3280;
			} else if (sigma == 8) {
				I1 = 0.419;
				I2 = 1.965;
				sigmaVP = 0.3892;
			} else if (sigma == 9) {
				I1 = 0.313;
				I2 = 2.320;
				sigmaVP = 0.4624;
			}

		} else if (numLooks == 4) {

			if (sigma == 5) {
				I1 = 0.694;
				I2 = 1.385;
				sigmaVP = 0.1921;
			} else if (sigma == 6) {
				I1 = 0.630;
				I2 = 1.495;
				sigmaVP = 0.2348;
			} else if (sigma == 7) {
				I1 = 0.560;
				I2 = 1.627;
				sigmaVP = 0.2825;
			} else if (sigma == 8) {
				I1 = 0.480;
				I2 = 1.804;
				sigmaVP = 0.3354;
			} else if (sigma == 9) {
				I1 = 0.378;
				I2 = 2.094;
				sigmaVP = 0.3991;
			}
        }

        sigmaVPSqr = sigmaVP*sigmaVP;
    }


    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        AbstractMetadata.getAbstractedMetadata(targetProduct).setAttributeInt(AbstractMetadata.polsarData, 1);
    }

    private void addSelectedBands() throws OperatorException {

        String[] bandNames = null;
        boolean copyInputBands = false;
        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            bandNames = PolBandUtils.getT3BandNames();
        } else {
            copyInputBands = true;
        }

        for(PolBandUtils.QuadSourceBand bandList : srcBandList) {
            String suffix = bandList.suffix;
            if(copyInputBands) {
                bandNames = new String[bandList.srcBands.length];
                int i=0;
                for(Band band : bandList.srcBands) {
                    bandNames[i++] = band.getName();
                }
                suffix = "";
            }
            final Band[] targetBands = PolBandUtils.addBands(targetProduct, bandNames, suffix);
            bandList.addTargetBands(targetBands);
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            if(filter.equals(BOXCAR_SPECKLE_FILTER)) {

                if (sourceProductType == PolBandUtils.MATRIX.FULL) {
                    boxcarFilterFullPol(targetTiles, targetRectangle);
                } else if(sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3 ||
                          sourceProductType == PolBandUtils.MATRIX.C4 || sourceProductType == PolBandUtils.MATRIX.T4) {
                    boxcarFilterC3T3C4T4(targetTiles, targetRectangle);
                } else {
                    throw new OperatorException("For Boxcar filter, only C3, T3, C4 and T4 are supported currently");
                }

            } else if(filter.equals(REFINED_LEE_FILTER)) {

                if (sourceProductType == PolBandUtils.MATRIX.FULL) {
                    refinedLeeFilterFullPol(targetTiles, targetRectangle);
                } else if(sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3 ||
                          sourceProductType == PolBandUtils.MATRIX.C4 || sourceProductType == PolBandUtils.MATRIX.T4) {
                    refinedLeeFilterC3T3C4T4(targetTiles, targetRectangle);
                } else {
                    throw new OperatorException("For Refined Lee filter, only C3, T3, C4 and T4 are supported currently");
                }

            } else if(filter.equals(IDAN_FILTER)) {

                if (sourceProductType == PolBandUtils.MATRIX.FULL ||
                    sourceProductType == PolBandUtils.MATRIX.C3 ||
                    sourceProductType == PolBandUtils.MATRIX.T3) {
                    idanFilter(targetTiles, targetRectangle);
                } else {
                    throw new OperatorException("For IDAN filter, only C3 and T3 are supported currently");
                }

            } else if (filter.equals(LEE_SIGMA_FILTER)) {

                if (sourceProductType == PolBandUtils.MATRIX.FULL ||
                    sourceProductType == PolBandUtils.MATRIX.C3 ||
                    sourceProductType == PolBandUtils.MATRIX.T3) {
                    leeSigmaFilter(targetTiles, targetRectangle);
                } else {
                    throw new OperatorException("For Lee Sigma filter, only C3 and T3 are supported currently");
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Filter full polarimetric data with Box Car filter for given tile.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void boxcarFilterFullPol(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("boxcar x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h);
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    // todo: Here for every pixel T3 is computed 5 times if the filter size is 5, should save some result
                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfFilterSize, sourceImageWidth, sourceImageHeight,
                                                      sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    for(Band targetBand : bandList.targetBands) {
                        final String targetBandName = targetBand.getName();
                        final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                        if(targetBandName.equals("T11") || targetBandName.contains("T11_"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[0][0]);
                        else if(targetBandName.contains("T12_real"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[0][1]);
                        else if(targetBandName.contains("T12_imag"))
                            dataBuffer.setElemFloatAt(idx, (float)Ti[0][1]);
                        else if(targetBandName.contains("T13_real"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[0][2]);
                        else if(targetBandName.contains("T13_imag"))
                            dataBuffer.setElemFloatAt(idx, (float)Ti[0][2]);
                        else if(targetBandName.equals("T22") || targetBandName.contains("T22_"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[1][1]);
                        else if(targetBandName.contains("T23_real"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[1][2]);
                        else if(targetBandName.contains("T23_imag"))
                            dataBuffer.setElemFloatAt(idx, (float)Ti[1][2]);
                        else if(targetBandName.equals("T33") || targetBandName.contains("T33_"))
                            dataBuffer.setElemFloatAt(idx, (float)Tr[2][2]);
                    }

                }
            }
        }
    }

    /**
     * Filter C3, T3, C4 or T4 data with Box Car filter for given tile.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void boxcarFilterC3T3C4T4(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("boxcar x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;

        final double[] neighborValues = new double[filterSize*filterSize];
        Tile targetTile, sourceTile;

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
            for(final Band targetBand : bandList.targetBands) {
                targetTile = targetTiles.get(targetBand);
                final ProductData dataBuffer = targetTile.getDataBuffer();
                sourceTile = getSourceTile(sourceProduct.getBand(targetBand.getName()), sourceTileRectangle);

                for (int y = y0; y < maxY; ++y) {
                    for (int x = x0; x < maxX; ++x) {

                        final int idx = targetTile.getDataBufferIndex(x, y);

                        getNeighborValues(x, y, sx0, sy0, sw, sh, sourceTile, neighborValues);

                        dataBuffer.setElemFloatAt(idx, (float)getMeanValue(neighborValues));
                    }
                }
            }
        }
    }

    /**
     * Filter the given tile of image with refined Lee filter.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     */
    private void refinedLeeFilterFullPol(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h);
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final double[][] data11Real = new double[sh][sw];
        final double[][] data12Real = new double[sh][sw];
        final double[][] data12Imag = new double[sh][sw];
        final double[][] data13Real = new double[sh][sw];
        final double[][] data13Imag = new double[sh][sw];
        final double[][] data22Real = new double[sh][sw];
        final double[][] data23Real = new double[sh][sw];
        final double[][] data23Imag = new double[sh][sw];
        final double[][] data33Real = new double[sh][sw];
        final double[][] span = new double[sh][sw];

        final TileIndex trgIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));
        final int filterSize2 = filterSize*filterSize;

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            createT3SpanImage(bandList.srcBands[0], sourceRectangle, dataBuffers, data11Real, data12Real, data12Imag,
                              data13Real, data13Imag, data22Real, data23Real, data23Imag, data33Real, span);

            final double[][] neighborSpanValues = new double[filterSize][filterSize];
            final double[][] neighborPixelValues = new double[filterSize][filterSize];

            final ProductData[] targetDataBuffers = new ProductData[9];

            for(final Band targetBand : bandList.targetBands) {
                final String trgBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if(targetDataBuffers[0] == null && (trgBandName.equals("T11") || trgBandName.contains("T11_")))
                    targetDataBuffers[0] = dataBuffer;
                else if(targetDataBuffers[1] == null && trgBandName.contains("T12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if(targetDataBuffers[2] == null && trgBandName.contains("T12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if(targetDataBuffers[3] == null && trgBandName.contains("T13_real"))
                    targetDataBuffers[3] = dataBuffer;
                else if(targetDataBuffers[4] == null && trgBandName.contains("T13_imag"))
                    targetDataBuffers[4] = dataBuffer;
                else if(targetDataBuffers[5] == null && (trgBandName.equals("T22") || trgBandName.contains("T22_")))
                    targetDataBuffers[5] = dataBuffer;
                else if(targetDataBuffers[6] == null && trgBandName.contains("T23_real"))
                    targetDataBuffers[6] = dataBuffer;
                else if(targetDataBuffers[7] == null && trgBandName.contains("T23_imag"))
                    targetDataBuffers[7] = dataBuffer;
                else if(targetDataBuffers[8] == null && (trgBandName.equals("T33") || trgBandName.contains("T33_")))
                    targetDataBuffers[8] = dataBuffer;
            }

            int i = 0;
            for (T3Elem elem : T3Elem.values()) {
                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int idx = trgIndex.getIndex(x);

                        int n = 0;
                        switch (elem) {
                            case T11:
                                n = getLocalData(x, y, sourceRectangle, data11Real, span, neighborPixelValues, neighborSpanValues);
                                i = 0;
                                break;

                            case T12_real:
                                n = getLocalData(x, y, sourceRectangle, data12Real, span, neighborPixelValues, neighborSpanValues);
                                i = 1;
                                break;

                            case T12_imag:
                                n = getLocalData(x, y, sourceRectangle, data12Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 2;
                                break;

                            case T13_real:
                                n = getLocalData(x, y, sourceRectangle, data13Real, span, neighborPixelValues, neighborSpanValues);
                                i = 3;
                                break;

                            case T13_imag:
                                n = getLocalData(x, y, sourceRectangle, data13Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 4;
                                break;

                            case T22:
                                n = getLocalData(x, y, sourceRectangle, data22Real, span, neighborPixelValues, neighborSpanValues);
                                i = 5;
                                break;

                            case T23_real:
                                n = getLocalData(x, y, sourceRectangle, data23Real, span, neighborPixelValues, neighborSpanValues);
                                i = 6;
                                break;

                            case T23_imag:
                                n = getLocalData(x, y, sourceRectangle, data23Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 7;
                                break;

                            case T33:
                                n = getLocalData(x, y, sourceRectangle, data33Real, span, neighborPixelValues, neighborSpanValues);
                                i = 8;
                                break;

                            default:
                                break;
                        }

                        if (n < filterSize2) {
                            targetDataBuffers[i].setElemFloatAt(
                                    idx, (float)computePixelValueUsingLocalStatistics(neighborPixelValues));
                        } else {
                            targetDataBuffers[i].setElemFloatAt(
                                    idx, (float)computePixelValueUsingEdgeDetection(neighborPixelValues, neighborSpanValues));
                        }
                    }
                }
            }
        }
    }

    /**
     * Filter C3, T3, C4 or T4 data for the given tile with refined Lee filter.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     */
    private void refinedLeeFilterC3T3C4T4(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;
        final int filterSize2 = filterSize*filterSize;
        
        final double[][] neighborSpanValues = new double[filterSize][filterSize];
        final double[][] neighborPixelValues = new double[filterSize][filterSize];

        final int syMax = sy0 + sh;
        final int sxMax = sx0 + sw;

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {

            final double[][] span = new double[sh][sw];
            createSpanImage(bandList.srcBands, sourceTileRectangle, span);

            for (Band targetBand : bandList.targetBands){
                final Tile targetTile = targetTiles.get(targetBand);
                final Tile sourceTile = getSourceTile(sourceProduct.getBand(targetBand.getName()), sourceTileRectangle);
                final TileIndex trgIndex = new TileIndex(targetTile);
                final TileIndex srcIndex = new TileIndex(sourceTile);
                final ProductData dataBuffer = targetTile.getDataBuffer();

                final float[] srcData = sourceTile.getDataBufferFloat();

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    final int yhalf = y - halfFilterSize;

                    for (int x = x0; x < maxX; ++x) {
                        final int xhalf = x - halfFilterSize;

                        final int n = getNeighborValuesWithoutBorderExt
                                (xhalf, yhalf, sx0, sy0, syMax, sxMax, neighborPixelValues, span, neighborSpanValues,
                                 srcIndex, srcData);

                        double v;
                        if (n < filterSize2) {
                            v = computePixelValueUsingLocalStatistics(neighborPixelValues);
                        } else {
                            v = computePixelValueUsingEdgeDetection(neighborPixelValues, neighborSpanValues);
                        }
                        dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)v);

                    }
                }
            }
        }
    }

    /**
     * Get source tile rectangle.
     * @param x0 X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfFilterSize) {
            sx0 -= halfFilterSize;
            sw += halfFilterSize;
        }

        if (y0 >= halfFilterSize) {
            sy0 -= halfFilterSize;
            sh += halfFilterSize;
        }

        if (x0 + w + halfFilterSize <= sourceImageWidth) {
            sw += halfFilterSize;
        }

        if (y0 + h + halfFilterSize <= sourceImageHeight) {
            sh += halfFilterSize;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Get pixel values in a filter size rectanglar region centered at the given pixel.
     * @param x X coordinate of a given pixel.
     * @param y Y coordinate of a given pixel.
     * @param sx0 X coordinate of pixel at upper left corner of source tile.
     * @param sy0 Y coordinate of pixel at upper left corner of source tile.
     * @param sw Source tile width.
     * @param sh Source tile height.
     * @param sourceTile The source tile.
     * @param neighborValues Array holding the pixel values.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private void getNeighborValues(final int x, final int y, final int sx0, final int sy0, final int sw, final int sh,
                                   final Tile sourceTile, final double[] neighborValues) {

        final ProductData sourceData = sourceTile.getDataBuffer();

        for (int i = 0; i < filterSize; ++i) {

            int xi = x - halfFilterSize + i;
            if (xi < sx0) {
                xi = sx0;
            } else if (xi >= sx0 + sw) {
                xi = sx0 + sw - 1;
            }

            final int stride = i*filterSize;
            for (int j = 0; j < filterSize; ++j) {

                int yj = y - halfFilterSize + j;
                if (yj < sy0) {
                    yj = sy0;
                } else if (yj >= sy0 + sh) {
                    yj = sy0 + sh - 1;
                }

                neighborValues[j + stride] = sourceData.getElemDoubleAt(sourceTile.getDataBufferIndex(xi, yj));
            }
        }
    }

    /**
     * Get the mean value of pixel intensities in a given rectanglar region.
     * @param neighborValues The pixel values in the given rectanglar region.
     * @return mean The mean value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the mean value.
     */
    private static double getMeanValue(final double[] neighborValues) {

        double mean = 0.0;
        for (double neighborValue : neighborValues) {
            mean += neighborValue;
        }
        mean /= neighborValues.length;

        return mean;
    }

    /**
     * Get the variance of pixel intensities in a given rectanglar region.
     * @param neighborValues The pixel values in the given rectanglar region.
     * @param mean of neighbourhood
     * @return var The variance value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the variance.
     */
    private static double getVarianceValue(final double[] neighborValues, final double mean) {

        double var = 0.0;
        if (neighborValues.length > 1) {

            for (double neighborValue : neighborValues) {
                final double diff = neighborValue - mean;
                var += diff * diff;
            }
            var /= neighborValues.length;
        }

        return var;
    }

    /**
     * Create Span image.
     * @param sourceBands the input bands
     * @param sourceTileRectangle The source tile rectangle.
     * @param span The span image.
     */
    private void createSpanImage(final Band[] sourceBands, final Rectangle sourceTileRectangle, final double[][] span) {

        // The pixel value of the span image is given by the trace of the covariance or coherence matrix for the pixel.
        Tile[] sourceTiles;
        if (sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3) {
            sourceTiles = new Tile[3];
        } else if (sourceProductType == PolBandUtils.MATRIX.C4 || sourceProductType == PolBandUtils.MATRIX.T4) {
            sourceTiles = new Tile[4];
        } else {
            throw new OperatorException("Polarimetric Matrix not supported");
        }

        for (final Band band : sourceBands) {
            final String bandName = band.getName();
            if (bandName.contains("11")) {
                sourceTiles[0] = getSourceTile(band, sourceTileRectangle);
            } else if (bandName.contains("22")) {
                sourceTiles[1] = getSourceTile(band, sourceTileRectangle);
            } else if (bandName.contains("33")) {
                sourceTiles[2] = getSourceTile(band, sourceTileRectangle);
            } else if (bandName.contains("44")) {
                sourceTiles[3] = getSourceTile(band, sourceTileRectangle);
            }
        }

        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        for (int y = sy0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            final int spanY = y-sy0;
            for (int x = sx0; x < maxX; ++x) {
                final int index = srcIndex.getIndex(x);

                double sum = 0.0;
                for (Tile srcTile:sourceTiles) {
                    sum += srcTile.getDataBuffer().getElemDoubleAt(index);
                }
                span[spanY][x-sx0] = sum/4;
            }
        }
    }

    /**
     * Get span image pixel values in a filter size rectanglar region centered at the given pixel.
     * @param xhalf X coordinate of the given pixel.
     * @param yhalf Y coordinate of the given pixel.
     * @param sx0 X coordinate of pixel at upper left corner of source tile.
     * @param sy0 Y coordinate of pixel at upper left corner of source tile.
     * @param neighborPixelValues 2-D array holding the pixel valuse
     * @param span The span image.
     * @param neighborSpanValues 2-D array holding the span image pixel valuse.
     * @return The number of valid pixels.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValuesWithoutBorderExt(
            final int xhalf, final int yhalf, final int sx0, final int sy0, final int syMax, final int sxMax,
            final double[][] neighborPixelValues, final double[][] span, double[][] neighborSpanValues,
            final TileIndex srcIndex, final float[] srcData) {

        int k = 0;
        for (int j = 0; j < filterSize; ++j) {
            final int yj = yhalf + j;

            if(yj < sy0 || yj >= syMax) {
                for (int i = 0; i < filterSize; ++i) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                }
                continue;
            }

            final int spanY = yj-sy0;
            srcIndex.calculateStride(yj);
            for (int i = 0; i < filterSize; ++i) {
                final int xi = xhalf + i;

                if (xi < sx0 || xi >= sxMax) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                } else {
                    neighborPixelValues[j][i] = srcData[srcIndex.getIndex(xi)];
                    neighborSpanValues[j][i] = span[spanY][xi-sx0];
                    k++;
                }
            }
        }

        return k;
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingLocalStatistics(final double[][] neighborPixelValues) {

        // here y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues);
        final double varY = getLocalVarianceValue(meanY, neighborPixelValues);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY*meanY*sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b*(neighborPixelValues[halfFilterSize][halfFilterSize] - meanY);
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param neighborSpanValues The span image pixel values in the neighborhood.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingEdgeDetection(final double[][] neighborPixelValues,
                                                       final double[][] neighborSpanValues) {

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(stride, subWindowSize, neighborSpanValues, subAreaMeans);

        int d = getDirection(subAreaMeans);

        final double[] spanPixels = new double[convSize];
        getNonEdgeAreaPixelValues(neighborSpanValues, d, spanPixels);

        final double meanY = getMeanValue(spanPixels);
        final double varY = getVarianceValue(spanPixels, meanY);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY*meanY*sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        final double b = varX / varY;

        final double[] covElemPixels = new double[convSize];
        getNonEdgeAreaPixelValues(neighborPixelValues, d, covElemPixels);
        final double meanZ = getMeanValue(covElemPixels);

        return meanZ + b*(neighborPixelValues[halfFilterSize][halfFilterSize] - meanZ);
    }

    /**
     * Comppute local mean for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local mean.
     */
    private double getLocalMeanValue(final double[][] neighborPixelValues) {
        int k = 0;
        double mean = 0;
        for (int j = 0; j < filterSize; ++j) {
            for (int i = 0; i < filterSize; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    mean += neighborPixelValues[j][i];
                    k++;
                }
            }
        }
        return mean/k;
    }

    /**
     * Comppute local variance for pixels in the neighborhood.
     * @param mean The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local variance.
     */
    private double getLocalVarianceValue(final double mean, final double[][] neighborPixelValues) {
        int k = 0;
        double var = 0.0;
        for (int j = 0; j < filterSize; ++j) {
            for (int i = 0; i < filterSize; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    final double diff = neighborPixelValues[j][i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }
        return var/(k-1);
    }

    /**
     * Compute mean values for the 3x3 sub-areas in the sliding window.
     * @param stride Stride for shifting sub-window within the sliding window.
     * @param subWindowSize Size of sub-area.
     * @param neighborPixelValues The pixel values in the sliding window.
     * @param subAreaMeans The 9 mean values.
     */
    private static void computeSubAreaMeans(final int stride, final int subWindowSize,
                                            final double[][] neighborPixelValues, double[][] subAreaMeans) {

        final double subWindowSizeSqr = subWindowSize*subWindowSize;
        for (int j = 0; j < 3; j++) {
            final int y0 = j*stride;
            for (int i = 0; i < 3; i++) {
                final int x0 = i*stride;

                double mean = 0.0;
                for (int y = y0; y < y0 + subWindowSize; y++) {
                    for (int x = x0; x < x0 + subWindowSize; x++) {
                        mean += neighborPixelValues[y][x];
                    }
                }
                subAreaMeans[j][i] = mean / subWindowSizeSqr;
            }
        }
    }

    /**
     * Get gradient direction.
     * @param subAreaMeans The mean values for the 3x3 sub-areas in the sliding window.
     * @return The direction.
     */
    private static int getDirection(final double[][] subAreaMeans) {

        final double[] gradient = new double[4];
        gradient[0] = subAreaMeans[0][2] + subAreaMeans[1][2] + subAreaMeans[2][2] -
                      subAreaMeans[0][0] - subAreaMeans[1][0] - subAreaMeans[2][0];

        gradient[1] = subAreaMeans[0][1] + subAreaMeans[0][2] + subAreaMeans[1][2] -
                      subAreaMeans[1][0] - subAreaMeans[2][0] - subAreaMeans[2][1];

        gradient[2] = subAreaMeans[0][0] + subAreaMeans[0][1] + subAreaMeans[0][2] -
                      subAreaMeans[2][0] - subAreaMeans[2][1] - subAreaMeans[2][2];

        gradient[3] = subAreaMeans[0][0] + subAreaMeans[0][1] + subAreaMeans[1][0] -
                      subAreaMeans[1][2] - subAreaMeans[2][1] - subAreaMeans[2][2];

        int direction = 0;
        double maxGradient = -1.0;
        for (int i = 0; i < 4; i++) {
            double absGrad = Math.abs(gradient[i]);
            if (maxGradient < absGrad) {
                maxGradient = absGrad;
                direction = i;
            }
        }

        if (gradient[direction] > 0.0) {
            direction += 4;
        }
        
        return direction;
    }

    /**
     * Get pixel values from the non-edge area indicated by the given direction.
     * @param neighborPixelValues The pixel values in the filterSize by filterSize neighborhood.
     * @param d The direction index.
     * @param pixels The array of pixels.
     */
    private void getNonEdgeAreaPixelValues(final double[][] neighborPixelValues, final int d, double[] pixels) {

        switch (d) {
        case 0: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = halfFilterSize; x < filterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 1: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = y; x < filterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 2: {

            int k = 0;
            for (int y = 0; y <= halfFilterSize; y++) {
                for (int x = 0; x < filterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 3: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = 0; x < filterSize - y; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 4: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = 0; x <= halfFilterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 5: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = 0; x < y + 1; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 6: {

            int k = 0;
            for (int y = halfFilterSize; y < filterSize; y++) {
                for (int x = 0; x < filterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 7: {

            int k = 0;
            for (int y = 0; y < filterSize; y++) {
                for (int x = filterSize - 1 - y; x < filterSize; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        }
        }
    }

    private int getLocalData(final int xc, final int yc, final Rectangle sourceRectangle, final double[][] data,
                             final double[][] span, double[][] neighborPixelValues, double[][] neighborSpanValues) {

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int syMax = sy0 + sh;
        final int sxMax = sx0 + sw;
        final int yhalf = yc - halfFilterSize;
        final int xhalf = xc - halfFilterSize;

        int k = 0;
        for (int j = 0; j < filterSize; ++j) {
            final int yj = yhalf + j;

            if(yj < sy0 || yj >= syMax) {
                for (int i = 0; i < filterSize; ++i) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                }
                continue;
            }

            final int spanY = yj-sy0;
            for (int i = 0; i < filterSize; ++i) {
                final int xi = xhalf + i;

                if (xi < sx0 || xi >= sxMax) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                } else {
                    neighborPixelValues[j][i] = data[spanY][xi-sx0];
                    neighborSpanValues[j][i] = span[spanY][xi-sx0];
                    k++;
                }
            }
        }

        return k;
    }

    /**
     * Create Span image.
     * @param sourceRectangle The source tile rectangle.
     * @param span The span image.
     */
    private void createT3SpanImage(final Band srcBand0,
            final Rectangle sourceRectangle, final ProductData[] dataBuffers, final double[][] data11Real,
            final double[][] data12Real, final double[][] data12Imag, final double[][] data13Real,
            final double[][] data13Imag, final double[][] data22Real, final double[][] data23Real,
            final double[][] data23Imag, final double[][] data33Real, final double[][] span) {

        // The pixel value of the span image is given by the trace of the covariance or coherence matrix for the pixel.
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;

        final TileIndex srcIndex = new TileIndex(getSourceTile(srcBand0, sourceRectangle));

        final double[][] Mr = new double[3][3];
        final double[][] Mi = new double[3][3];

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] Sr = new double[2][2];
            final double[][] Si = new double[2][2];

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getComplexScatterMatrix(index, dataBuffers, Sr, Si);
                    PolOpUtils.computeCoherencyMatrixT3(Sr, Si, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2])/4.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getCoherencyMatrixT3(index, dataBuffers, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2])/4.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getCovarianceMatrixC3(index, dataBuffers, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2])/4.0;
                }
            }

        } else {
            throw new OperatorException("Polarimetric Matrix not supported");
        }
    }


    /**
     * Filter full polarimetric data with IDAN filter for given tile.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void idanFilter(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        // System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h);
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final double[][] data11Real = new double[sh][sw];
        final double[][] data12Real = new double[sh][sw];
        final double[][] data12Imag = new double[sh][sw];
        final double[][] data13Real = new double[sh][sw];
        final double[][] data13Imag = new double[sh][sw];
        final double[][] data22Real = new double[sh][sw];
        final double[][] data23Real = new double[sh][sw];
        final double[][] data23Imag = new double[sh][sw];
        final double[][] data33Real = new double[sh][sw];
        final double[][] span = new double[sh][sw];

        final TileIndex trgIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            createT3SpanImage(bandList.srcBands[0], sourceRectangle, dataBuffers,
                              data11Real, data12Real, data12Imag, data13Real, data13Imag,
                              data22Real, data23Real, data23Imag, data33Real, span);

            final ProductData[] targetDataBuffers = new ProductData[9];

            for(final Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if(targetBandName.contains("11") || targetBandName.contains("11_"))
                    targetDataBuffers[0] = dataBuffer;
                else if(targetBandName.contains("12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if(targetBandName.contains("12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if(targetBandName.contains("13_real"))
                    targetDataBuffers[3] = dataBuffer;
                else if(targetBandName.contains("13_imag"))
                    targetDataBuffers[4] = dataBuffer;
                else if(targetBandName.contains("22") || targetBandName.contains("22_"))
                    targetDataBuffers[5] = dataBuffer;
                else if(targetBandName.contains("23_real"))
                    targetDataBuffers[6] = dataBuffer;
                else if(targetBandName.contains("23_imag"))
                    targetDataBuffers[7] = dataBuffer;
                else if(targetBandName.contains("33") || targetBandName.contains("33_"))
                    targetDataBuffers[8] = dataBuffer;
            }

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    final Seed seed = getInitialSeed(x, y, sx0, sy0, sw, sh, data11Real, data22Real, data33Real);

                    final Pix[] anPixelList = getIDANPixels(x, y, sx0, sy0, sw, sh,
                                                            data11Real, data22Real, data33Real, seed);

                    final double b = computeFilterScaleParam(sx0, sy0, anPixelList, span);

                    int i = 0;
                    double value = 0.0;
                    for (final T3Elem elem : T3Elem.values()) {
                        switch (elem) {
                            case T11:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data11Real, b);
                                i = 0;
                                break;

                            case T12_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Real, b);
                                i = 1;
                                break;

                            case T12_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Imag, b);
                                i = 2;
                                break;

                            case T13_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data13Real, b);
                                i = 3;
                                break;

                            case T13_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data13Imag, b);
                                i = 4;
                                break;

                            case T22:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data22Real, b);
                                i = 5;
                                break;

                            case T23_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data23Real, b);
                                i = 6;
                                break;

                            case T23_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data23Imag, b);
                                i = 7;
                                break;

                            case T33:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data33Real, b);
                                i = 8;
                                break;

                            default:
                                break;
                        }

                        targetDataBuffers[i].setElemFloatAt(idx, (float)value);
                    }
                }
            }
        }
    }

    /**
     * Compute the initial seed value for given pixel. The marginal median in a 3x3 neighborhood of the given pixel
     * is computed and used as the seed value.
     * @param xc X coordinate of the given pixel
     * @param yc Y coordinate of the given pixel
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw Width of the source rectangle
     * @param sh Height of the source rectangle
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @return seed The computed initial seed value
     */
    private static Seed getInitialSeed(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] data11Real, final double[][] data22Real, final double[][] data33Real) {

        // define vector p = [d11 d22 d33], then the seed is the marginal median of all vectors in the 3x3 window
        final double[] d11 = new double[9];
        final double[] d22 = new double[9];
        final double[] d33 = new double[9];

        int r, c;
        int k = 0;
        for (int y = yc - 1; y <= yc + 1; y++) {
            for (int x = xc - 1; x <= xc + 1; x++) {
                if (x >= sx0 && x < sx0 + sw && y >= sy0 && y < sy0 + sh) {
                    r = y - sy0;
                    c = x - sx0;
                    d11[k] = data11Real[r][c];
                    d22[k] = data22Real[r][c];
                    d33[k] = data33Real[r][c];
                    k++;
                }
            }
        }

        Arrays.sort(d11, 0, k);
        Arrays.sort(d22, 0, k);
        Arrays.sort(d33, 0, k);

        final int med = k/2;
        final Seed seed = new Seed();
        seed.value[0] = d11[med];
        seed.value[1] = d22[med];
        seed.value[2] = d33[med];
        seed.calculateAbsolutes();
        return seed;
    }

    /**
     * Find all pixels in the adaptive neighbourhood of a given pixel.
     * @param xc X coordinate of the given pixel
     * @param yc Y coordinate of the given pixel
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw Width of the source rectangle
     * @param sh Height of the source rectangle
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed The initial seed value
     * @return anPixelList List of pixels in the adaptive neighbourhood
     */
    private Pix[] getIDANPixels(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                               final double[][] data11Real, final double[][] data22Real, final double[][] data33Real,
                               final Seed seed) {

        // 1st run of region growing with IDAN50 threshold and initial seed, qualified pixel goes to anPixelList,
        // non-qualified pixel goes to "background pixels" list
        final double threshold50 = 2*sigmaV;
        final List<Pix> anPixelList = new ArrayList<Pix>(anSize);
        final Pix[] bgPixelList = regionGrowing(xc, yc, sx0, sy0, sw, sh, data11Real, data22Real, data33Real,
                                                seed, threshold50, anPixelList);

        // update seed with the pixels in AN
        final Seed newSeed = new Seed();
        if (!anPixelList.isEmpty()) {
            for (Pix pixel : anPixelList) {
                newSeed.value[0] += data11Real[pixel.y - sy0][pixel.x - sx0];
                newSeed.value[1] += data22Real[pixel.y - sy0][pixel.x - sx0];
                newSeed.value[2] += data33Real[pixel.y - sy0][pixel.x - sx0];
            }
            newSeed.value[0] /= anPixelList.size();
            newSeed.value[1] /= anPixelList.size();
            newSeed.value[2] /= anPixelList.size();
        } else {
            newSeed.value[0] = seed.value[0];
            newSeed.value[1] = seed.value[1];
            newSeed.value[2] = seed.value[2];
        }
        newSeed.calculateAbsolutes();

        // 2nd run of region growing with IDAN95 threshold, the new seed and "background pixels" i.e. pixels rejected
        // in the 1st run of region growing are checked and added to AN
        final double threshold95 = 6*sigmaV;
        reExamBackgroundPixels(sx0, sy0, data11Real, data22Real, data33Real, newSeed, threshold95,
                               anPixelList, bgPixelList);

        if (anPixelList.isEmpty()) {
            return new Pix[] { new Pix(xc, yc) };
        }
        return anPixelList.toArray(new Pix[anPixelList.size()]);
    }

    /**
     * Find pixels in the adaptive neighbourhood (AN) of a given pixel using region growing method.
     * @param xc X coordinate of the given pixel
     * @param yc Y coordinate of the given pixel
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw Width of the source rectangle
     * @param sh Height of the source rectangle
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed The initial seed value for AN
     * @param threshold Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @return bgPixelList List of pixels rejected in searching for AN pixels
     */
    private Pix[] regionGrowing(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                               final double[][] data11Real, final double[][] data22Real, final double[][] data33Real,
                               final Seed seed, final double threshold, final List<Pix> anPixelList) {

        final int rc = yc - sy0;
        final int cc = xc - sx0;
        final Map<Integer, Boolean> visited = new HashMap<Integer, Boolean>(anSize+8);
        final List<Pix> bgPixelList = new ArrayList<Pix>(anSize);

        if (distance(data11Real[rc][cc], data22Real[rc][cc], data33Real[rc][cc], seed) < threshold) {
            visited.put(rc*sw + cc, true);
            anPixelList.add(new Pix(xc, yc));
        } else {
            bgPixelList.add(new Pix(xc, yc));
        }

        final List<Pix> front = new ArrayList<Pix>(anSize);
        front.add(new Pix(xc, yc));
        final List<Pix> newfront = new ArrayList<Pix>(anSize);

        final int width = sx0 + sw;
        final int height = sy0 + sh;
        int r, c;
        Integer index;

        while (anPixelList.size() < anSize && !front.isEmpty()) {
            newfront.clear();

            for (final Pix p : front) {

                final int[] x = {p.x-1,   p.x, p.x+1, p.x-1, p.x+1, p.x-1,   p.x, p.x+1};
                final int[] y = {p.y-1, p.y-1, p.y-1,   p.y,   p.y, p.y+1, p.y+1, p.y+1};

                for (int i = 0; i < 8; i++) {

                    if (x[i] >= sx0 && x[i] < width && y[i] >= sy0 && y[i] < height) {
                        r = y[i] - sy0;
                        c = x[i] - sx0;
                        index = r*sw+c;
                        if(visited.get(index) == null) {
                            final Pix newPos = new Pix(x[i], y[i]);
                            if (distance(data11Real[r][c], data22Real[r][c], data33Real[r][c], seed) < threshold) {
                                visited.put(index, true);
                                anPixelList.add(newPos);
                                newfront.add(newPos);
                            } else {
                                bgPixelList.add(newPos);
                            }
                        }
                    }
                }
                if(anPixelList.size() > anSize) {
                    break;
                }
            }
            front.clear();
            front.addAll(newfront);
        }
        return bgPixelList.toArray(new Pix[bgPixelList.size()]);
    }

    private final static class Pix {
        final int x, y;
        public Pix(final int xx, final int yy) {
            x = xx;
            y = yy;
        }
    }

    /**
     * Cmpute distance between vector p and a given seed vector.
     * @param p0 Vector
     * @param p1 Vector
     * @param p2 Vector
     * @param seed Vector
     * @return Distance
     */
    private static double distance(final double p0, final double p1, final double p2, final Seed seed) {
        return Math.abs(p0 - seed.value[0])/seed.absValue[0] +
               Math.abs(p1 - seed.value[1])/seed.absValue[1] +
               Math.abs(p2 - seed.value[2])/seed.absValue[2];
    }

    /**
     * Re-exam the pixels that are rejected in the region growing process and add them to AN if qualified.
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed The seed value for AN
     * @param threshold Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @param bgPixelList List of pixels rejected in searching for AN pixels
     */
    private static void reExamBackgroundPixels(final int sx0, final int sy0, final double[][] data11Real,
                                        final double[][] data22Real,  final double[][] data33Real,
                                        final Seed seed, final double threshold,
                                        final List<Pix> anPixelList, final Pix[] bgPixelList) {
        int r, c;
        for (final Pix pixel : bgPixelList) {
            r = pixel.y - sy0;
            c = pixel.x - sx0;
            if (distance(data11Real[r][c], data22Real[r][c], data33Real[r][c], seed) < threshold) {
                anPixelList.add(new Pix(pixel.x, pixel.y));
            }
        }
    }

    /**
     * Compute scale parameter b for MMSE filter.
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param anPixelList List of pixels in AN
     * @param span Span image in source rectangle
     * @return The scale parameter b
     */
    private double computeFilterScaleParam(
            final int sx0, final int sy0, final Pix[] anPixelList, final double[][] span) {

        final double[] spanPixels = new double[anPixelList.length];
        int k = 0;
        for (Pix pixel : anPixelList) {
            spanPixels[k++] = span[pixel.y - sy0][pixel.x - sx0];
        }

        return computeMMSEWeight(spanPixels, sigmaVSqr);
    }

    /**
     * Compute MMSE filtered value for given pixel.
     * @param x X coordinate of the given pixel
     * @param y Y  coordinate of the given pixel
     * @param sx0 X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0 Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param anPixelList List of pixels in AN
     * @param data Data in source rectangle
     * @param b The scale parameter
     * @return The filtered value
     */
    private static double getIDANFilteredValue(final int x, final int y, final int sx0, final int sy0,
                                               final Pix[] anPixelList, final double[][] data, final double b) {

        double mean = 0.0;
        for (final Pix pixel : anPixelList) {
            mean += data[pixel.y - sy0][pixel.x - sx0];
        }
        mean /= anPixelList.length;

        return mean + b*(data[y - sy0][x - sx0] - mean);
    }

    private static class Seed {
        final double[] value = new double[3];
        final double[] absValue = new double[3];

        public void calculateAbsolutes() {
            absValue[0] = Math.abs(value[0]);
            absValue[1] = Math.abs(value[1]);
            absValue[2] = Math.abs(value[2]);
        }
    }


    private void leeSigmaFilter(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        // System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h);
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final TileIndex trgIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));
        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] sourceDataBuffers = new ProductData[bandList.srcBands.length];

            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = getSourceTile(bandList.srcBands[i], sourceRectangle);
                sourceDataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

            final ProductData[] targetDataBuffers = new ProductData[9];
            for(final Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if(targetBandName.contains("11") || targetBandName.contains("11_"))
                    targetDataBuffers[0] = dataBuffer;
                else if(targetBandName.contains("12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if(targetBandName.contains("12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if(targetBandName.contains("13_real"))
                    targetDataBuffers[3] = dataBuffer;
                else if(targetBandName.contains("13_imag"))
                    targetDataBuffers[4] = dataBuffer;
                else if(targetBandName.contains("22") || targetBandName.contains("22_"))
                    targetDataBuffers[5] = dataBuffer;
                else if(targetBandName.contains("23_real"))
                    targetDataBuffers[6] = dataBuffer;
                else if(targetBandName.contains("23_imag"))
                    targetDataBuffers[7] = dataBuffer;
                else if(targetBandName.contains("33") || targetBandName.contains("33_"))
                    targetDataBuffers[8] = dataBuffer;
            }

            Z98 z98 = new Z98();
            computeZ98Values(sourceTiles[0], sourceRectangle, sourceDataBuffers, z98);

            double[][] Tr = new double[3][3];
            double[][] Ti = new double[3][3];

            int xx, yy, trgIdx, srcIdx;
            boolean[][] isPointTarget = new boolean[h][w];
            T3[][] filterWindowT3 = null;
            T3[][] targetWindowT3 = null;

            for (int y = y0; y < maxY; ++y) {
                yy = y - y0;
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(y);

                for (int x = x0; x < maxX; ++x) {
                    xx = x - x0;
                    trgIdx = trgIndex.getIndex(x);
                    srcIdx = srcIndex.getIndex(x);

                    PolOpUtils.getT3(srcIdx, sourceProductType, sourceDataBuffers, Tr, Ti);

                    if (y - halfFilterSize < sy0 || y + halfFilterSize > sy0 + sh - 1 ||
                        x - halfFilterSize < sx0 || x + halfFilterSize > sx0 + sw - 1) {

                        filterWindowT3 = new T3[filterWindowSize][filterWindowSize];
                        getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowT3);
                        final int n = setPixelsInSigmaRange(filterWindowT3);
                        computeFilteredT3(filterWindowT3, n, sigmaVSqr, Tr, Ti);
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    if (isPointTarget[yy][xx]) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

					targetWindowT3 = new T3[targetWindowSize][targetWindowSize];
                    getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], targetWindowT3);

					if(checkPointTarget(z98, targetWindowT3, isPointTarget, x0, y0, w, h)) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
					}

                    double[] sigmaRangeT11 = new double[2];
                    double[] sigmaRangeT22 = new double[2];
                    double[] sigmaRangeT33 = new double[2];
                    computeSigmaRange(targetWindowT3, 0, sigmaRangeT11);
                    computeSigmaRange(targetWindowT3, 1, sigmaRangeT22);
                    computeSigmaRange(targetWindowT3, 2, sigmaRangeT33);

					filterWindowT3 = new T3[filterWindowSize][filterWindowSize];
                    getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowT3);

                    final int n = selectPixelsInSigmaRange(sigmaRangeT11, sigmaRangeT22, sigmaRangeT33, filterWindowT3);
                    if (n == 0) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    computeFilteredT3(filterWindowT3, n, sigmaVPSqr, Tr, Ti);
                    saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                }
            }
        }
    }

    private void computeZ98Values(final Tile sourceTile, final Rectangle sourceRectangle,
                                  final ProductData[] sourceDataBuffers, Z98 z98) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int z98Index = (int)(sw*sh*0.98);

        double[] t11 = new double[sw*sh];
        double[] t22 = new double[sw*sh];
        double[] t33 = new double[sw*sh];

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        int k = 0;
        for (int y = sy0; y < maxY; y++) {
            srcIndex.calculateStride(y);
            for (int x = sx0; x < maxX; x++) {
                final int index = srcIndex.getIndex(x);
                PolOpUtils.getT3(index, sourceProductType, sourceDataBuffers, Tr, Ti);
                t11[k] = Tr[0][0];
                t22[k] = Tr[1][1];
                t33[k] = Tr[2][2];
                k++;
            }
        }

        Arrays.sort(t11);
        Arrays.sort(t22);
        Arrays.sort(t33);

        z98.t11 = t11[z98Index];
        z98.t22 = t22[z98Index];
        z98.t33 = t33[z98Index];
    }

    private static void saveT3(final double[][] Tr, final double[][] Ti,
                               final int idx, final ProductData[] targetDataBuffers) {

        targetDataBuffers[0].setElemFloatAt(idx, (float)Tr[0][0]); // T11
        targetDataBuffers[1].setElemFloatAt(idx, (float)Tr[0][1]); // T12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float)Ti[0][1]); // T12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float)Tr[0][2]); // T13_real
        targetDataBuffers[4].setElemFloatAt(idx, (float)Ti[0][2]); // T13_imag
        targetDataBuffers[5].setElemFloatAt(idx, (float)Tr[1][1]); // T22
        targetDataBuffers[6].setElemFloatAt(idx, (float)Tr[1][2]); // T23_real
        targetDataBuffers[7].setElemFloatAt(idx, (float)Ti[1][2]); // T23_imag
        targetDataBuffers[8].setElemFloatAt(idx, (float) Tr[2][2]); // T33
    }

    private void getWindowPixelT3s(final int x, final int y, final ProductData[] sourceDataBuffers,
                                   final int sx0, final int sy0, final int sw, final int sh,
                                   final Tile sourceTile, T3[][] windowPixelT3) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int windowSize = windowPixelT3.length;
        final int halfWindowSize = windowSize/2;

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        int yy, xx;
        for (int j = 0; j < windowSize; j++) {
			yy = y - halfWindowSize + j;
            srcIndex.calculateStride(yy);
            for (int i = 0; i < windowSize; i++) {
				xx = x - halfWindowSize + i;
                if (yy >= sy0 && yy <= sy0 + sh - 1 && xx >= sx0 && xx <= sx0 + sw - 1) {
                    final int srcIdx = srcIndex.getIndex(xx);
                    PolOpUtils.getT3(srcIdx, sourceProductType, sourceDataBuffers, Tr, Ti);
                    windowPixelT3[j][i] = new T3(xx, yy, Tr, Ti);
                }
            }
        }
    }

	private boolean checkPointTarget(final Z98 z98, final T3[][] targetWindowT3, boolean[][] isPointTarget,
                                     final int x0, final int y0, final int w, final int h) {
	
		if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[0][0] > z98.t11) {
            if (getClusterSize(z98.t11, targetWindowT3, 0) > targetSize) {
                markClusterPixels(isPointTarget, z98.t11, targetWindowT3, x0, y0, w, h, 0);
                return true;
            }
		}

		if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[1][1] > z98.t22) {
            if (getClusterSize(z98.t22, targetWindowT3, 1) > targetSize) {
                markClusterPixels(isPointTarget, z98.t22, targetWindowT3, x0, y0, w, h, 1);
                return true;
            }
		}

		if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[2][2] > z98.t33) {
            if (getClusterSize(z98.t33, targetWindowT3, 2) > targetSize) {
                markClusterPixels(isPointTarget, z98.t33, targetWindowT3, x0, y0, w, h, 2);
                return true;
            }
		}

        return false;
	}

    private int getClusterSize(final double threshold, final T3[][] targetWindowT3, final int elemIdx) {

        int clusterSize = 0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowT3[j][i].Tr[elemIdx][elemIdx] > threshold) {
                    clusterSize++;
                }
            }
        }
        return clusterSize;
    }

    private void markClusterPixels(
            boolean[][] isPointTarget, final double threshold, final T3[][] targetWindowT3,
            final int x0, final int y0, final int w, final int h, final int elemIdx) {

        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowT3[j][i].Tr[elemIdx][elemIdx] > threshold &&
                    targetWindowT3[j][i].y >= y0 && targetWindowT3[j][i].y < y0 + h &&
                    targetWindowT3[j][i].x >= x0 && targetWindowT3[j][i].x < x0 + w) {

                    isPointTarget[targetWindowT3[j][i].y - y0][targetWindowT3[j][i].x - x0] = true;
                }
            }
        }
    }

    private void computeSigmaRange(T3[][] targetWindowT3, final int elemIdx, double[] sigmaRange) {

        final double[] data = new double[targetWindowSize*targetWindowSize];
        int k = 0;
        double mean = 0.0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                data[k] = targetWindowT3[j][i].Tr[elemIdx][elemIdx];
                mean += data[k];
                k++;
            }
        }
        mean /= k;

        final double b = computeMMSEWeight(data, sigmaVSqr);
        final double filtered = mean + b*(data[k/2] - mean);

        sigmaRange[0] = filtered*I1;
        sigmaRange[1] = filtered*I2;
    }

    private static double computeMMSEWeight(final double[] dataArray, final double sigmaVSqr) {

        final double meanY = getMeanValue(dataArray);
        final double varY = getVarianceValue(dataArray, meanY);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY*meanY*sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        return varX / varY;
    }

    private int setPixelsInSigmaRange(final T3[][] filterWindowT3) {
        int n = 0;
        for (int j = 0; j < filterWindowSize; j++) {
            for (int i = 0; i < filterWindowSize; i++) {
                if (filterWindowT3[j][i] != null) {
                    filterWindowT3[j][i].inSigmaRange = true;
                    n++;
                }
            }
        }
        return n;
    }

    private int selectPixelsInSigmaRange(final double[] sigmaRangeT11, final double[] sigmaRangeT22,
                                         final double[] sigmaRangeT33, T3[][] filterWindowT3) {

        int numPixelsInSigmaRange = 0;
        for (int j = 0; j < filterWindowSize; j++) {
            for (int i = 0; i < filterWindowSize; i++) {
                if (filterWindowT3[j][i] != null &&
                    filterWindowT3[j][i].Tr[0][0] >= sigmaRangeT11[0] &&
                    filterWindowT3[j][i].Tr[0][0] <= sigmaRangeT11[1] &&
                    filterWindowT3[j][i].Tr[1][1] >= sigmaRangeT22[0] &&
                    filterWindowT3[j][i].Tr[1][1] <= sigmaRangeT22[1] &&
                    filterWindowT3[j][i].Tr[2][2] >= sigmaRangeT33[0] &&
                    filterWindowT3[j][i].Tr[2][2] <= sigmaRangeT33[1]) {

                    filterWindowT3[j][i].inSigmaRange = true;
                    numPixelsInSigmaRange++;
                }
            }
        }
        return numPixelsInSigmaRange;
    }

    private void computeFilteredT3(final T3[][] filterWindowT3, final int n, final double sigmaVSqr,
                                   double[][] Tr, double[][] Ti) {

        double[] span = new double[n];
        getSpan(filterWindowT3, span);
        final double b = computeMMSEWeight(span, sigmaVSqr);
        filterT3(filterWindowT3, b, n, Tr, Ti);
    }

    private void getSpan(final T3[][] filterWindowT3, double[] span) {

        int k = 0;
        for (int j = 0; j < filterWindowSize; j++) {
            for (int i = 0; i < filterWindowSize; i++) {
                if (filterWindowT3[j][i] != null && filterWindowT3[j][i].inSigmaRange) {
                    span[k++] = filterWindowT3[j][i].Tr[0][0] +
                                filterWindowT3[j][i].Tr[1][1] +
                                filterWindowT3[j][i].Tr[2][2];
                }
            }
        }
    }

    private void filterT3(final T3[][] filterWindowT3, final double b, final int numPixelsInSigmaRange,
                                   double[][] filteredTr, double[][] filteredTi) {

        final double[][] meanTr = new double[3][3];
        final double[][] meanTi = new double[3][3];

        for (int j = 0; j < filterWindowSize; j++) {
            for (int i = 0; i < filterWindowSize; i++) {
                if (filterWindowT3[j][i] != null && filterWindowT3[j][i].inSigmaRange) {

                    for (int m = 0; m < 3; m++) {
                        for (int n = 0; n < 3; n++) {
                            meanTr[m][n] += filterWindowT3[j][i].Tr[m][n];
                            meanTi[m][n] += filterWindowT3[j][i].Ti[m][n];
                        }
                    }
                }
            }
        }

        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 3; n++) {
                meanTr[m][n] /= numPixelsInSigmaRange;
                meanTi[m][n] /= numPixelsInSigmaRange;
            }
        }

        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 3; n++) {
                filteredTr[m][n] = (1-b)*meanTr[m][n] + b*filterWindowT3[halfFilterSize][halfFilterSize].Tr[m][n];
                filteredTi[m][n] = (1-b)*meanTi[m][n] + b*filterWindowT3[halfFilterSize][halfFilterSize].Ti[m][n];
            }
        }
    }



    public final static class Z98 {
        public double t11;
        public double t22;
        public double t33;
    }

	public final static class T3 {
		public int x = -1;
		public int y = -1;
		public final double[][] Tr = new double[3][3];
		public final double[][] Ti = new double[3][3];
        public boolean inSigmaRange = false;

		public T3(final int x, final int y, final double[][] Tr, final double[][] Ti) {
			this.x = x;
			this.y = y;
            for (int a=0;a<Tr.length;a++) {
                System.arraycopy(Tr[a],0,this.Tr[a],0,Tr[a].length);
                System.arraycopy(Ti[a],0,this.Ti[a],0,Ti[a].length);
            }
		}
		
		public T3() {
		}
	}


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PolarimetricSpeckleFilterOp.class);
            setOperatorUI(PolarimetricSpeckleFilterOpUI.class);
        }
    }
}

