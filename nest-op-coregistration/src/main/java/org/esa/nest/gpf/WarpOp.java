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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.AutoCloseOptionPane;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.ResourceUtils;
import org.jblas.DoubleMatrix;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Image co-registration is fundamental for Interferometry SAR (InSAR) imaging and its applications, such as
 * DEM map generation and analysis. To obtain a high quality InSAR image, the individual complex images need
 * to be co-registered to sub-pixel accuracy. The co-registration is accomplished through an alignment of a
 * master image with a slave image.
 * <p/>
 * To achieve the alignment of master and slave images, the first step is to generate a set of uniformly
 * spaced ground control points (GCPs) in the master image, along with the corresponding GCPs in the slave
 * image. Details of the generation of the GCP pairs are given in GCPSelectionOperator. The next step is to
 * construct a warp distortion function from the computed GCP pairs and generate co-registered slave image.
 * <p/>
 * This operator computes the warp function from the master-slave GCP pairs for given polynomial order.
 * Basically coefficients of two polynomials are determined from the GCP pairs with each polynomial for
 * one coordinate of the image pixel. With the warp function determined, the co-registered image can be
 * obtained by mapping slave image pixels to master image pixels. In particular, for each pixel position in
 * the master image, warp function produces its corresponding pixel position in the slave image, and the
 * pixel value is computed through interpolation. The following interpolation methods are available:
 * <p/>
 * 1. Nearest-neighbour interpolation
 * 2. Bilinear interpolation
 * 3. Bicubic interpolation
 * 4. Bicubic2 interpolation
 */

@OperatorMetadata(alias = "Warp",
        category = "SAR Tools\\Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Create Warp Function And Get Co-registrated Images")
public class WarpOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The RMS threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "1.0",
            label = "RMS Threshold")
    private float rmsThreshold = 1.0f;

    @Parameter(description = "The order of WARP polynomial function", valueSet = {"1", "2", "3"}, defaultValue = "2",
            label = "Warp Polynomial Order")
    private int warpPolynomialOrder = 2;

    @Parameter(valueSet = {NEAREST_NEIGHBOR, BILINEAR, BICUBIC, BICUBIC2,
            TRI, CC4P, CC6P, TS6P, TS8P, TS16P}, defaultValue = BILINEAR, label = "Interpolation Method")
    private String interpolationMethod = BILINEAR;

    private Interpolation interp = null;
    private InterpolationTable interpTable = null;

    @Parameter(description = "Show the Residuals file in a text viewer", defaultValue = "false", label = "Show Residuals")
    private boolean openResidualsFile = false;

    private Band masterBand = null;
    private Band masterBand2 = null;
    private boolean complexCoregistration = false;
    private boolean warpDataAvailable = false;

    public static final String NEAREST_NEIGHBOR = "Nearest-neighbor interpolation";
    public static final String BILINEAR = "Bilinear interpolation";
    public static final String BICUBIC = "Bicubic interpolation";
    public static final String BICUBIC2 = "Bicubic2 interpolation";
    public static final String RECT = "Step function (nearest-neighbor)";
    public static final String TRI = "Linear interpolation";
    public static final String CC4P = "Cubic convolution (4 points)";
    public static final String CC6P = "Cubic convolution (6 points)";
    public static final String TS6P = "Truncated sinc (6 points)";
    public static final String TS8P = "Truncated sinc (8 points)";
    public static final String TS16P = "Truncated sinc (16 points)";

    private final Map<Band, Band> sourceRasterMap = new HashMap<Band, Band>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<Band, Band>(10);
    private final Map<Band, WarpData> warpDataMap = new HashMap<Band, WarpData>(10);

    private String processedSlaveBand;
    private String[] masterBandNames = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public WarpOp() {
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
            // clear any old residual file
            final File residualsFile = getResidualsFile(sourceProduct);
            if (residualsFile.exists()) {
                residualsFile.delete();
            }

            masterBand = sourceProduct.getBandAt(0);
            if (masterBand.getUnit() != null && masterBand.getUnit().equals(Unit.REAL) && sourceProduct.getNumBands() > 1) {
                complexCoregistration = true;
                masterBand2 = sourceProduct.getBandAt(1);
            }

            // The following code is temporary
            if (complexCoregistration) {

                if (interpolationMethod.equals(CC4P)) {
                    createInSARInterpTable(4);

                } else if (interpolationMethod.equals(CC6P)) {
                    createInSARInterpTable(6);

                } else if (interpolationMethod.equals(TS6P)) {
                    createInSARInterpTable(6);

                } else if (interpolationMethod.equals(TS8P)) {
                    createInSARInterpTable(8);

                } else if (interpolationMethod.equals(TS16P)) {
                    createInSARInterpTable(16);

                } else {
                    interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                }
            } else { // detected products

                if (interpolationMethod.equals(NEAREST_NEIGHBOR)) {
                    interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                } else if (interpolationMethod.equals(BILINEAR)) {
                    interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                } else if (interpolationMethod.equals(BICUBIC)) {
                    interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
                } else if (interpolationMethod.equals(BICUBIC2)) {
                    interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
                }
            }

            createTargetProduct();

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            processedSlaveBand = absRoot.getAttributeString("processed_slave");

        } catch (Throwable e) {
            openResidualsFile = true;
            OperatorUtils.catchOperatorException(getId(), e);
        } 
    }

    private void createInSARInterpTable(final int numberOfKernelPoints) {
        final int subsampleBits = 7;
        final int precisionBits = 32;
        final int padding = numberOfKernelPoints/2 - 1;
        final double[] kernelAxis = InSARInterpolationKernels.defineXAxis(numberOfKernelPoints);
        final double[] lutInsar = InSARInterpolationKernels.constructKernel(kernelAxis, interpolationMethod);
        final float data[] = new float[lutInsar.length];
        int i = 0;
        for(double lut : lutInsar) {
            data[i++] = (float)lut;
        }
        interpTable = new InterpolationTable(padding, numberOfKernelPoints, subsampleBits, precisionBits, data);
    }

    private void addSlaveGCPs(final WarpData warpData, final String bandName) {

        final GeoCoding targetGeoCoding = targetProduct.getGeoCoding();
        final ProductNodeGroup<Placemark> targetGCPGroup = targetProduct.getGcpGroup(targetProduct.getBand(bandName));
        targetGCPGroup.removeAll();

        for (int i = 0; i < warpData.slaveGCPList.size(); ++i) {
            final Placemark sPin = warpData.slaveGCPList.get(i);
            final Placemark tPin = Placemark.createPointPlacemark(GcpDescriptor.getInstance(),
                    sPin.getName(),
                    sPin.getLabel(),
                    sPin.getDescription(),
                    sPin.getPixelPos(),
                    sPin.getGeoPos(),
                    targetGeoCoding);

            targetGCPGroup.add(tPin);
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

        masterBandNames = StackUtils.getMasterBandNames(sourceProduct);

        final int numSrcBands = sourceProduct.getNumBands();
        int inc = 1;
        if (complexCoregistration)
            inc = 2;
        for (int i = 0; i < numSrcBands; i += inc) {
            final Band srcBand = sourceProduct.getBandAt(i);
            Band targetBand;
            if (srcBand == masterBand || srcBand == masterBand2 ||
                    StringUtils.contains(masterBandNames, srcBand.getName())) {
                targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                targetBand = targetProduct.addBand(srcBand.getName(), ProductData.TYPE_FLOAT32);
                ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            }
            sourceRasterMap.put(targetBand, srcBand);

            if (complexCoregistration) {
                final Band srcBandQ = sourceProduct.getBandAt(i + 1);
                Band targetBandQ;
                if (srcBand == masterBand || srcBand == masterBand2 ||
                        StringUtils.contains(masterBandNames, srcBand.getName())) {
                    targetBandQ = ProductUtils.copyBand(srcBandQ.getName(), sourceProduct, targetProduct, false);
                    targetBandQ.setSourceImage(srcBandQ.getSourceImage());
                } else {
                    targetBandQ = targetProduct.addBand(srcBandQ.getName(), ProductData.TYPE_FLOAT32);
                    ProductUtils.copyRasterDataNodeProperties(srcBandQ, targetBandQ);
                }
                sourceRasterMap.put(targetBandQ, srcBandQ);

                complexSrcMap.put(srcBandQ, srcBand);
                final String suffix = '_'+OperatorUtils.getSuffixFromBandName(srcBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBand, targetBandQ, suffix);
                ReaderUtils.createVirtualPhaseBand(targetProduct, targetBand, targetBandQ, suffix);
            }
        }

        // coregistrated image should have the same geo-coding as the master image
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
        updateTargetProductMetadata();
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);
    }

    private synchronized void getWarpData(final Rectangle targetRectangle) throws OperatorException {

        if (warpDataAvailable) {
            return;
        }

        // find first real slave band
        final Band targetBand = targetProduct.getBand(processedSlaveBand);
        // force getSourceTile to computeTiles on GCPSelection
        final Tile sourceRaster = getSourceTile(sourceRasterMap.get(targetBand), targetRectangle);

        // for all slave bands or band pairs compute a warp
        final int numSrcBands = sourceProduct.getNumBands();
        int inc = 1;
        if (complexCoregistration)
            inc = 2;

        boolean appendFlag = false;
        for (int i = 0; i < numSrcBands; i += inc) {

            final Band srcBand = sourceProduct.getBandAt(i);
            if (srcBand == masterBand || srcBand == masterBand2 ||
                StringUtils.contains(masterBandNames, srcBand.getName()))
                continue;

            ProductNodeGroup<Placemark> slaveGCPGroup = sourceProduct.getGcpGroup(srcBand);
            if (slaveGCPGroup.getNodeCount() < 3) {
                // find others for same slave product
                final String slvProductName = StackUtils.getSlaveProductName(sourceProduct, srcBand, null);
                for(Band band : sourceProduct.getBands()) {
                    if(band != srcBand) {
                        final String productName = StackUtils.getSlaveProductName(sourceProduct, band, null);
                        if(slvProductName != null && slvProductName.equals(productName)) {
                            slaveGCPGroup = sourceProduct.getGcpGroup(band);
                            if (slaveGCPGroup.getNodeCount() >= 3)
                                break;
                        }
                    }
                }
            }
            final WarpData warpData = new WarpData(slaveGCPGroup);
            warpDataMap.put(srcBand, warpData);

            if (slaveGCPGroup.getNodeCount() < 3) {
                warpData.notEnoughGCPs = true;
                continue;
                //throw new OperatorException(slaveGCPGroup.getNodeCount() +
                //        " GCPs survived. Try using more GCPs or a larger window");
            }

            int parseIdex = 0;
            final ProductNodeGroup<Placemark> masterGCPGroup = sourceProduct.getGcpGroup(masterBand);

            computeWARPPolynomial(warpData, warpPolynomialOrder, masterGCPGroup); // compute initial warp polynomial
            if(warpData.notEnoughGCPs) continue;
            outputCoRegistrationInfo(
                    sourceProduct, warpPolynomialOrder, warpData, appendFlag, 0.0f, parseIdex, srcBand.getName());

            appendFlag = true;
            if (warpData.rmsMean > rmsThreshold && eliminateGCPsBasedOnRMS(warpData, (float) warpData.rmsMean)) {
                final float threshold = (float) warpData.rmsMean;
                computeWARPPolynomial(warpData, warpPolynomialOrder, masterGCPGroup); // compute 2nd warp polynomial
                if(warpData.notEnoughGCPs) continue;
                outputCoRegistrationInfo(
                        sourceProduct, warpPolynomialOrder, warpData, appendFlag, threshold, ++parseIdex, srcBand.getName());
            }

            if (warpData.rmsMean > rmsThreshold && eliminateGCPsBasedOnRMS(warpData, (float) warpData.rmsMean)) {
                final float threshold = (float) warpData.rmsMean;
                computeWARPPolynomial(warpData, warpPolynomialOrder, masterGCPGroup); // compute 3rd warp polynomial
                if(warpData.notEnoughGCPs) continue;
                outputCoRegistrationInfo(
                        sourceProduct, warpPolynomialOrder, warpData, appendFlag, threshold, ++parseIdex, srcBand.getName());
            }

            eliminateGCPsBasedOnRMS(warpData, rmsThreshold);
            computeWARPPolynomial(warpData, warpPolynomialOrder, masterGCPGroup); // compute final warp polynomial
            if(warpData.notEnoughGCPs) continue;
            outputCoRegistrationInfo(
                    sourceProduct, warpPolynomialOrder, warpData, appendFlag, rmsThreshold, ++parseIdex, srcBand.getName());

            addSlaveGCPs(warpData, srcBand.getName());
        }

        announceGCPWarning();

        if (openResidualsFile) {
            final File residualsFile = getResidualsFile(sourceProduct);
            if (Desktop.isDesktopSupported() && residualsFile.exists()) {
                try {
                    Desktop.getDesktop().open(residualsFile);
                } catch (Exception e) {
                    System.out.println("Error opening residuals file "+e.getMessage());
                    // do nothing
                }
            }
        }

        // update metadata
        writeWarpDataToMetadata();

        warpDataAvailable = true;
    }

    private void writeWarpDataToMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final Set<Band> bandSet = warpDataMap.keySet();

        for(Band band : bandSet) {
            final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, band.getName(), true);

            MetadataElement warpDataElem = bandElem.getElement("WarpData");
            if(warpDataElem == null) {
                warpDataElem = new MetadataElement("WarpData");
                bandElem.addElement(warpDataElem);
            } else {
                // empty out element
                final MetadataAttribute[] attribList = warpDataElem.getAttributes();
                for(MetadataAttribute attrib : attribList) {
                    warpDataElem.removeAttribute(attrib);
                }
            }

            final WarpData warpData = warpDataMap.get(band);
            if(warpData.numValidGCPs > 0) {
                for (int i = 0; i < warpData.numValidGCPs; i++) {
                    final MetadataElement gcpElem = new MetadataElement("GCP"+i);
                    warpDataElem.addElement(gcpElem);

                    gcpElem.setAttributeDouble("mst_x", warpData.masterGCPCoords[2 * i]);
                    gcpElem.setAttributeDouble("mst_y", warpData.masterGCPCoords[2 * i + 1]);

                    gcpElem.setAttributeDouble("slv_x", warpData.slaveGCPCoords[2 * i]);
                    gcpElem.setAttributeDouble("slv_y", warpData.slaveGCPCoords[2 * i + 1]);

                    if (!warpData.notEnoughGCPs) {
                        gcpElem.setAttributeDouble("rms", warpData.rms[i]);
                    }
                }
            }
        }
    }     

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    //@Override
   // public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
   //         throws OperatorException {
    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        //System.out.println("WARPOperator: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if(!warpDataAvailable) {
                getWarpData(targetRectangle);
            }

                final Band srcBand = sourceRasterMap.get(targetBand);
                if (srcBand == null)
                    return;
                Band realSrcBand = complexSrcMap.get(srcBand);
                if (realSrcBand == null)
                    realSrcBand = srcBand;

                // create source image
                final Tile sourceRaster = getSourceTile(srcBand, targetRectangle);

                if(pm.isCanceled())
                    return;

                final WarpData warpData = warpDataMap.get(realSrcBand);
                if(warpData.notEnoughGCPs)
                    return;

                final RenderedImage srcImage = sourceRaster.getRasterDataNode().getSourceImage();

                // get warped image
                final RenderedOp warpedImage = createWarpImage(warpData.jaiWarp, srcImage);

                // copy warped image data to target
                final float[] dataArray = warpedImage.getData(targetRectangle).getSamples(x0, y0, w, h, 0, (float[]) null);

                targetTile.setRawSamples(ProductData.createInstance(dataArray));

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Compute WARP polynomial function using master and slave GCP pairs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     * @param masterGCPGroup      The master GCPs.
     */
    public static void computeWARPPolynomial(
            final WarpData warpData, final int warpPolynomialOrder, final ProductNodeGroup<Placemark> masterGCPGroup) {

        getNumOfValidGCPs(warpData, warpPolynomialOrder);

        getMasterAndSlaveGCPCoordinates(warpData, masterGCPGroup);
        if(warpData.notEnoughGCPs) return;

        warpData.computeWARP(warpPolynomialOrder);

        computeRMS(warpData, warpPolynomialOrder);
    }

    /**
     * Get the number of valid GCPs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     * @throws OperatorException The exceptions.
     */
    private static void getNumOfValidGCPs(
            final WarpData warpData, final int warpPolynomialOrder) throws OperatorException {

        warpData.numValidGCPs = warpData.slaveGCPList.size();
        final int requiredGCPs = (warpPolynomialOrder + 2) * (warpPolynomialOrder + 1) / 2;
        if (warpData.numValidGCPs < requiredGCPs) {
            warpData.notEnoughGCPs = true;
            //throw new OperatorException("Order " + warpPolynomialOrder + " requires " + requiredGCPs +
            //        " GCPs, valid GCPs are " + warpData.numValidGCPs + ", try a larger RMS threshold.");
        }
    }

    /**
     * Get GCP coordinates for master and slave bands.
     *
     * @param warpData       Stores the warp information per band.
     * @param masterGCPGroup The master GCPs.
     */
    private static void getMasterAndSlaveGCPCoordinates(
            final WarpData warpData, final ProductNodeGroup<Placemark> masterGCPGroup) {

        warpData.masterGCPCoords = new float[2 * warpData.numValidGCPs];
        warpData.slaveGCPCoords = new float[2 * warpData.numValidGCPs];

        for (int i = 0; i < warpData.numValidGCPs; ++i) {

            final Placemark sPin = warpData.slaveGCPList.get(i);
            final PixelPos sGCPPos = sPin.getPixelPos();
            //System.out.println("WARP: slave gcp[" + i + "] = " + "(" + sGCPPos.x + "," + sGCPPos.y + ")");

            final Placemark mPin = masterGCPGroup.get(sPin.getName());
            final PixelPos mGCPPos = mPin.getPixelPos();
            //System.out.println("WARP: master gcp[" + i + "] = " + "(" + mGCPPos.x + "," + mGCPPos.y + ")");

            final int j = 2 * i;
            warpData.masterGCPCoords[j] = (float)mGCPPos.x;
            warpData.masterGCPCoords[j + 1] = (float)mGCPPos.y;
            warpData.slaveGCPCoords[j] = (float)sGCPPos.x;
            warpData.slaveGCPCoords[j + 1] = (float)sGCPPos.y;
        }
    }

    /**
     * Compute root mean square error of the warped GCPs for given WARP function and given GCPs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     */
    private static void computeRMS(final WarpData warpData, final int warpPolynomialOrder) {

        // compute RMS for all valid GCPs
        warpData.rms = new float[warpData.numValidGCPs];
        warpData.colResiduals = new float[warpData.numValidGCPs];
        warpData.rowResiduals = new float[warpData.numValidGCPs];
        final PixelPos slavePos = new PixelPos(0.0f, 0.0f);
        for (int i = 0; i < warpData.rms.length; i++) {
            final int i2 = 2 * i;
            getWarpedCoords(warpData,
                    warpPolynomialOrder,
                    warpData.masterGCPCoords[i2],
                    warpData.masterGCPCoords[i2 + 1],
                    slavePos);
            final double dX = slavePos.x - warpData.slaveGCPCoords[i2];
            final double dY = slavePos.y - warpData.slaveGCPCoords[i2 + 1];
            warpData.colResiduals[i] = (float) dX;
            warpData.rowResiduals[i] = (float) dY;
            warpData.rms[i] = (float) Math.sqrt(dX * dX + dY * dY);
        }

        // compute some statistics
        warpData.rmsMean = 0.0;
        warpData.rowResidualMean = 0.0;
        warpData.colResidualMean = 0.0;
        double rms2Mean = 0.0;
        double rowResidual2Mean = 0.0;
        double colResidual2Mean = 0.0;

        for (int i = 0; i < warpData.rms.length; i++) {
            warpData.rmsMean += warpData.rms[i];
            rms2Mean += warpData.rms[i] * warpData.rms[i];
            warpData.rowResidualMean += warpData.rowResiduals[i];
            rowResidual2Mean += warpData.rowResiduals[i] * warpData.rowResiduals[i];
            warpData.colResidualMean += warpData.colResiduals[i];
            colResidual2Mean += warpData.colResiduals[i] * warpData.colResiduals[i];
        }
        warpData.rmsMean /= warpData.rms.length;
        rms2Mean /= warpData.rms.length;
        warpData.rowResidualMean /= warpData.rms.length;
        rowResidual2Mean /= warpData.rms.length;
        warpData.colResidualMean /= warpData.rms.length;
        colResidual2Mean /= warpData.rms.length;

        warpData.rmsStd = Math.sqrt(rms2Mean - warpData.rmsMean * warpData.rmsMean);
        warpData.rowResidualStd = Math.sqrt(rowResidual2Mean - warpData.rowResidualMean * warpData.rowResidualMean);
        warpData.colResidualStd = Math.sqrt(colResidual2Mean - warpData.colResidualMean * warpData.colResidualMean);
    }

    /**
     * Eliminate master and slave GCP pairs that have root mean square error greater than given threshold.
     *
     * @param warpData  Stores the warp information per band.
     * @param threshold Threshold for eliminating GCPs.
     * @return True if some GCPs are eliminated, false otherwise.
     */
    public static boolean eliminateGCPsBasedOnRMS(final WarpData warpData, final float threshold) {

        final List<Placemark> pinList = new ArrayList<Placemark>();
        if(warpData.slaveGCPList.size() < warpData.rms.length) {
            warpData.notEnoughGCPs = true;
            return true;
        }
        for (int i = 0; i < warpData.rms.length; i++) {
            if (warpData.rms[i] >= threshold) {
                pinList.add(warpData.slaveGCPList.get(i));
                //System.out.println("WARP: slave gcp[" + i + "] is eliminated");
            }
        }

        for (Placemark aPin : pinList) {
            warpData.slaveGCPList.remove(aPin);
        }

        return !pinList.isEmpty();
    }

    /**
     * Compute warped GCPs.
     *
     * @param warpData            The WARP polynomial.
     * @param warpPolynomialOrder The WARP polynomial order.
     * @param mX                  The x coordinate of master GCP.
     * @param mY                  The y coordinate of master GCP.
     * @param slavePos            The warped GCP position.
     * @throws OperatorException The exceptions.
     */
    public static void getWarpedCoords(final WarpData warpData, final int warpPolynomialOrder,
                                       final double mX, final double mY, final PixelPos slavePos)
            throws OperatorException {

        final double[] xCoeffs = warpData.xCoef;
        final double[] yCoeffs = warpData.yCoef;
        if (xCoeffs.length != yCoeffs.length) {
            throw new OperatorException("WARP has different number of coefficients for X and Y");
        }

        final int numOfCoeffs = xCoeffs.length;
        switch (warpPolynomialOrder) {
            case 1: {
                if (numOfCoeffs != 3) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                slavePos.x = (float)(xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY);

                slavePos.y = (float)(yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY);
                break;
            }
            case 2: {
                if (numOfCoeffs != 6) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = (float)(xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY);

                slavePos.y = (float)(yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY);
                break;
            }
            case 3: {
                if (numOfCoeffs != 10) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = (float)(xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY +
                        xCoeffs[6] * mXmX * mX + xCoeffs[7] * mX * mXmY + xCoeffs[8] * mXmY * mY + xCoeffs[9] * mYmY * mY);

                slavePos.y = (float)(yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY +
                        yCoeffs[6] * mXmX * mX + yCoeffs[7] * mX * mXmY + yCoeffs[8] * mXmY * mY + yCoeffs[9] * mYmY * mY);
                break;
            }
            default:
                throw new OperatorException("Incorrect WARP degree");
        }
    }

    /**
     * Output co-registration information to file.
     *
     * @param sourceProduct       The source product.
     * @param warpPolynomialOrder The order of Warp polinomial.
     * @param warpData            Stores the warp information per band.
     * @param appendFlag          Boolean flag indicating if the information is output to file in appending mode.
     * @param threshold           The threshold for elinimating GCPs.
     * @param parseIndex          Index for parsing GCPs.
     * @param bandName            the band name
     * @throws OperatorException The exceptions.
     */
    public static void outputCoRegistrationInfo(final Product sourceProduct, final int warpPolynomialOrder,
                                                 final WarpData warpData, final boolean appendFlag,
                                                 final float threshold, final int parseIndex, final String bandName)
            throws OperatorException {

        float[] xCoeffs = null;
        float[] yCoeffs = null;

        if (!warpData.notEnoughGCPs) {
            xCoeffs = warpData.jaiWarp.getXCoeffs();
            yCoeffs = warpData.jaiWarp.getYCoeffs();
        }

        final File residualFile = getResidualsFile(sourceProduct);
        PrintStream p = null; // declare a print stream object

        try {
            final FileOutputStream out = new FileOutputStream(residualFile.getAbsolutePath(), appendFlag);

            // Connect print stream to the output stream
            p = new PrintStream(out);

            p.println();

            if (!appendFlag) {
                p.println();
                p.format("Transformation degree = %d", warpPolynomialOrder);
                p.println();
            }

            p.println();
            p.print("------------------------ Band: " + bandName + " (Parse " + parseIndex + ") ------------------------");
            p.println();

            if (!warpData.notEnoughGCPs) {
                p.println();
                p.println("WARP coefficients:");
                for (float xCoeff : xCoeffs) {
                    p.print(xCoeff + ", ");
                }

                p.println();
                for (float yCoeff : yCoeffs) {
                    p.print(yCoeff + ", ");
                }
                p.println();
            }

            if (appendFlag) {
                p.println();
                p.format("RMS Threshold: %5.2f", threshold);
                p.println();
            }

            p.println();
            if (appendFlag) {
                p.print("Valid GCPs after parse " + parseIndex + " :");
            } else {
                p.print("Initial Valid GCPs:");
            }
            p.println();

            if (!warpData.notEnoughGCPs) {

                p.println();
                p.println("No. | Master GCP x | Master GCP y | Slave GCP x |" +
                        " Slave GCP y | Row Residual | Col Residual |    RMS    |");
                p.println("-------------------------------------------------" +
                        "--------------------------------------------------------");
                for (int i = 0; i < warpData.rms.length; i++) {
                    p.format("%2d  |%13.3f |%13.3f |%12.3f |%12.3f |%13.3f |%13.3f |%10.3f |",
                            i, warpData.masterGCPCoords[2 * i], warpData.masterGCPCoords[2 * i + 1],
                            warpData.slaveGCPCoords[2 * i], warpData.slaveGCPCoords[2 * i + 1],
                            warpData.rowResiduals[i], warpData.colResiduals[i], warpData.rms[i]);
                    p.println();
                }

                p.println();
                p.print("Row residual mean = " + warpData.rowResidualMean);
                p.println();
                p.print("Row residual std = " + warpData.rowResidualStd);
                p.println();

                p.println();
                p.print("Col residual mean = " + warpData.colResidualMean);
                p.println();
                p.print("Col residual std = " + warpData.colResidualStd);
                p.println();

                p.println();
                p.print("RMS mean = " + warpData.rmsMean);
                p.println();
                p.print("RMS std = " + warpData.rmsStd);
                p.println();

            } else {

                p.println();
                p.println("No. | Master GCP x | Master GCP y | Slave GCP x | Slave GCP y |");
                p.println("---------------------------------------------------------------");
                for (int i = 0; i < warpData.numValidGCPs; i++) {
                    p.format("%2d  |%13.3f |%13.3f |%12.3f |%12.3f |",
                            i, warpData.masterGCPCoords[2 * i], warpData.masterGCPCoords[2 * i + 1],
                            warpData.slaveGCPCoords[2 * i], warpData.slaveGCPCoords[2 * i + 1]);
                    p.println();
                }
            }
            p.println();
            p.println();

        } catch (IOException exc) {
            throw new OperatorException(exc);
        } finally {
            if (p != null)
                p.close();
        }
    }

    private static File getResidualsFile(final Product sourceProduct) {
        String fileName = sourceProduct.getName() + "_residual.txt";
        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if (!appUserDir.exists()) {
            appUserDir.mkdirs();
        }
        return new File(appUserDir.toString(), fileName);
    }

    /**
     * Create warped image.
     *
     * @param warp     The WARP polynomial.
     * @param srcImage The source image.
     * @return The warped image.
     */
    private RenderedOp createWarpImage(WarpPolynomial warp, final RenderedImage srcImage) {

        // reformat source image by casting pixel values from ushort to float
        final ParameterBlock pb1 = new ParameterBlock();
        pb1.addSource(srcImage);
        pb1.add(DataBuffer.TYPE_FLOAT);
        final RenderedImage srcImageFloat = JAI.create("format", pb1);

        // get warped image
        final ParameterBlock pb2 = new ParameterBlock();
        pb2.addSource(srcImageFloat);
        pb2.add(warp);

        if (interp != null) {
            pb2.add(interp);
        } else if (interpTable != null) {
            pb2.add(interpTable);
        }

        return JAI.create("warp", pb2);
    }

    public static class WarpData {
        public final List<Placemark> slaveGCPList = new ArrayList<Placemark>();
        private WarpPolynomial jaiWarp = null;
        public double[] xCoef = null;
        public double[] yCoef = null;

        public int numValidGCPs = 0;
        public boolean notEnoughGCPs = false;
        public float[] rms = null;
        public float[] rowResiduals = null;
        public float[] colResiduals = null;
        public float[] masterGCPCoords = null;
        public float[] slaveGCPCoords = null;

        public double rmsStd = 0;
        public double rmsMean = 0;
        public double rowResidualStd = 0;
        public double rowResidualMean = 0;
        public double colResidualStd = 0;
        public double colResidualMean = 0;

        public WarpData(ProductNodeGroup<Placemark> slaveGCPGroup) {
            for (int i = 0; i < slaveGCPGroup.getNodeCount(); ++i) {
                slaveGCPList.add(slaveGCPGroup.get(i));
            }
        }

        /**
         * Compute WARP function using master and slave GCPs.
         *
         * @param warpPolynomialOrder The WARP polynimal order.
         */
        public void computeWARP(final int warpPolynomialOrder) {

            jaiWarp = WarpPolynomial.createWarp(slaveGCPCoords, //source
                    0,
                    masterGCPCoords, // destination
                    0,
                    2 * numValidGCPs,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F,
                    warpPolynomialOrder);


            final float[] jaiXCoefs = jaiWarp.getXCoeffs();
            final float[] jaiYCoefs = jaiWarp.getYCoeffs();
            final int size = jaiXCoefs.length;
            xCoef = new double[size];
            yCoef = new double[size];
            for(int i=0; i < size; ++i) {
                xCoef[i] = jaiXCoefs[i];
                yCoef[i] = jaiYCoefs[i];
            }
        }
    }

    private void announceGCPWarning() {
        String msg = "";
        for(Band srcBand : sourceProduct.getBands()) {
            final WarpData warpData = warpDataMap.get(srcBand);
            if(warpData != null && warpData.notEnoughGCPs) {
                msg += srcBand.getName() +" does not have enough valid GCPs for the warp\n";
                openResidualsFile = true;
            }
        }
        if(!msg.isEmpty()) {
            System.out.println(msg);
            if(VisatApp.getApp() != null) {
                AutoCloseOptionPane.showWarningDialog("Some bands did not coregister", msg);
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(WarpOp.class);
            super.setOperatorUI(WarpOpUI.class);
        }
    }

    private static class InSARInterpolationKernels {

        // instances
        public double[] x;
        public double[] y;

        InSARInterpolationKernels() {
        }

        // set kernelAxis : no additional checks here!
        public void setXAxis(double[] doubles) {
            this.x = doubles;
        }

        // construct kernel axis for numberOfKernelPoints
        // eg. kernelLength = 4  --->  xKernelAxis = [-1 0 1 2]
        public static double[] defineXAxis(final int kernelLength) {
            final double[] xKernelAxis = new double[kernelLength];
            for (int i = 0; i < kernelLength; ++i) {
                xKernelAxis[i] = 1.0d - (kernelLength / 2) + i;
            }
            return xKernelAxis;
        }

        private enum ResampleKernels {
            RECT, TRI,
            TS6P, TS8P, TS16P,
            CC4P, CC6P,
            RS_KNAB4P, RS_KNAB6P, RS_KNAB8P, RS_KNAB10P, RS_KNAB16P,
            RS_RC6P, RS_RC12P
        }

        public static double[] constructKernel(double[] kernelAxis, final String resampleMethod) {

            final int kernelPoints = kernelAxis.length;

            final int INTERVAL = 127;            // precision: 1./INTERVAL [pixel]
            final int Ninterval = INTERVAL + 1;   // size of lookup table
            final double dx = 1.0 / INTERVAL;       // interval look up table

            DoubleMatrix kernel;
            final DoubleMatrix pntKernelAz = new DoubleMatrix(Ninterval,kernelPoints);

            for (int i = 0; i < Ninterval; i++) {
            //                switch (ResampleKernels.valueOf(resampleMethod.toUpperCase())) {
            //                    case RECT:
            //                        kernel = new DoubleMatrix(rect(kernelAxis));
            //                        pntKernelAz.putRow(i,kernel);
            //                        break;
            //                    case TRI:
            //            ......
            if (resampleMethod.equals(RECT)) {
                    kernel = new DoubleMatrix(rect(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(TRI)) {
                    kernel = new DoubleMatrix(tri(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(TS6P)) {
                    kernel = new DoubleMatrix(ts6(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(TS8P)) {
                    kernel = new DoubleMatrix(ts8(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(TS16P)) {
                    kernel = new DoubleMatrix(ts16(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(CC4P)) {
                    kernel = new DoubleMatrix(cc4(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                } else if (resampleMethod.equals(CC6P)) {
                    kernel = new DoubleMatrix(cc6(kernelAxis));
                    pntKernelAz.putRow(i, kernel);
                }
                kernelAxis = new DoubleMatrix(kernelAxis).sub(dx).toArray();
            }

            // normalization
            pntKernelAz.divColumnVector(pntKernelAz.rowSums());

            return pntKernelAz.transpose().toArray();
        }

        // methods for interpolators
        // -------------------------------------------
        // cc4: cubic convolution 4 points
        // input:
        //   - x-axis
        // output:
        //  - y=f(x); function evaluated at x
        public static double[] cc4(final double[] x) {

            final double alpha = -1.0;
            final double[] y = new double[x.length];

            for (int i = 0; i < y.length; i++) {
                final double xx2 = Math.sqrt(x[i]);
                final double xx = Math.sqrt(xx2);
                if (xx < 1)
                    y[i] = (alpha + 2) * xx2 * xx - (alpha + 3) * xx2 + 1;
                else if (xx < 2)
                    y[i] = alpha * xx2 * xx - 5 * alpha * xx2 + 8 * alpha * xx - 4 * alpha;
                else
                    y[i] = 0;
            }

            return y;

        } // END cc4


        // cc6: cubic convolution 4 points
        // input:
        //   - x-axis
        // output:
        //  - y=f(x); function evaluated at x

        private static double[] cc6(final double[] x) {

            final double alpha = -0.5;
            final double beta = 0.5;
            final double[] y = new double[x.length];

            for (int i = 0; i < y.length; i++) {
                final double xx2 = Math.pow(x[i], 2);
                final double xx = Math.sqrt(xx2);
                if (xx < 1)
                    y[i] = (alpha - beta + 2) * xx2 * xx - (alpha - beta + 3) * xx2 + 1;
                    //y[i] = (alpha+beta+2)*xx2*xx - (alpha+beta+3)*xx2 + 1; // wrong in reference paper??
                else if (xx < 2)
                    y[i] = alpha * xx2 * xx - (5 * alpha - beta) * xx2
                            + (8 * alpha - 3 * beta) * xx - (4 * alpha - 2 * beta);
                else if (xx < 3)
                    y[i] = beta * xx2 * xx - 8 * beta * xx2 + 21 * beta * xx - 18 * beta;
                else
                    y[i] = 0.0;
            }

            return y;

        } // END cc6

        // ts6: truncated sinc 6 points
        // input:
        //   - x-axis
        // output:
        //  - y=f(x); function evaluated at x

        private static double[] ts6(final double[] x) {

            final double[] y = new double[x.length];

            for (int i = 0; i < y.length; i++)
                y[i] = sinc(x[i]) * rect(x[i] / 6.0);

            return y;

        } // END ts6

        // ts8: truncated sinc 8 points
        // input:
        //   - x-axis
        // output:
        //  - y=f(x); function evaluated at x

        private static double[] ts8(final double[] x) {

            final double[] y = new double[x.length];

            for (int i = 0; i < y.length; i++)
                y[i] = sinc(x[i]) * rect(x[i] / 8.0);

            return y;

        } // END ts8

        // ts16: truncated sinc 6 points
        // input:
        //   - x-axis
        // output:
        //  - y=f(x); function evaluated at x

        private static double[] ts16(final double[] x) {

            double[] y = new double[x.length];

            for (int i = 0; i < y.length; i++)
                y[i] = sinc(x[i]) * rect(x[i] / 16.0);

            return y;

        } // END cc6

        // rect :: rect function for matrix (stepping function?)
        // input:
        //    - x-axis
        // output:
        //    - y=f(x); function evaluated at x

        private static double[] rect(final double[] x) {
            final double[] y = new double[x.length];
            for (int i = 0; i < y.length; i++) {
                y[i] = rect(x[i]);
            }
            return y;
        } // END rect

        // tri ::  tri function for matrix (piecewize linear?, triangle)
        // input:
        //    - x-axis
        // output:
        //    - y=f(x); function evaluated at x

        private static double[] tri(final double[] x) {
            final double[] y = new double[x.length];
            for (int i = 0; i < y.length; i++) {
                y[i] = tri(x[i]);
            }
            return y;
        } // END tri

        /*
              knab :: KNAB window of N points, oversampling factor CHI
                   defined by: Migliaccio IEEE letters vol41,no5, pp1105,1110, 2003
                   k = sinc(x).*(cosh((pi*v*L/2)*sqrt(1-(2.*x./L).^2))/cosh(pi*v*L/2));
                    input:
                       - x-axis
                       - oversampling factor of bandlimited sigal CHI
                       - N points of kernel size
                   output:
                       - y=f(x); function evaluated at x
        */

        private static double[] knab(final double[] x, final double CHI, final int N) {
            final double[] y = new double[x.length];
            final double v = 1.0 - 1.0 / CHI;
            final double vv = Math.PI * v * N / 2.0;
            final double coshvv = Math.cosh(vv);

            for (int i = 0; i < y.length; i++) {
                y[i] = sinc(x[i]) * Math.cosh(vv * Math.sqrt(1.0 - Math.pow(2.0 * x[i] / (double) N, 2))) / coshvv;
            }
            return y;

        } // END knab

        /*
       rc_kernel: Raised Cosine window of N points, oversampling factor CHI
                  defined by: Cho, Kong and Kim, J.Elektromagn.Waves and appl
                                    vol19, no.1, pp, 129-135, 2005;
       claimed to be best, 0.9999 for 6 points kernel.
       k(x) = sinc(x).*[cos(v*pi*x)/(1-4*v^2*x^2)]*rect(x/L)
             where v = 1-B/fs = 1-1/Chi (roll-off factor; ERS: 15.55/18.96)
             L = 6 (window size)
        input:
                - x-axis
                - oversampling factor of bandlimited sigal CHI
                - N points of kernel size
       output:
                - y=f(x); function evaluated at x
        */

        private static double[] rc_kernel(final double[] x, final double CHI, final int N) {

            final double[] y = new double[x.length];
            final double v = 1.0 - 1.0 / CHI;// alpha in paper cho05
            final double v2 = 2.0*v;
            final double vPI = v * Math.PI;

            for (int i = 0; i < y.length; i++) {
                y[i] = sinc(x[i]) * rect(x[i] / N) *
                        Math.cos(vPI * x[i]) / (1.0 - Math.pow(v2 * x[i], 2));
            }
            return y;
        } // END rc_kernel

        private static double sinc(final double x) {
            return ((x == 0) ? 1 : Math.sin(Math.PI * x) / (Math.PI * x));
        }

        private static double rect(final double x) {
            double ans = 0.0;
            if (x < 0.5 && x > -0.5) {
                ans = 1;
            } else if (x == 0.5 || x == -0.5) {
                ans = 0.5;
            }
            return ans;
        }

        private static double tri(final double x) {
            double ans = 0.0;
            if (x < 1.0 && x > -1.0) {
                ans = (x < 0) ? 1 + x : 1 - x;
            }
            return ans;
        }

    }

}
