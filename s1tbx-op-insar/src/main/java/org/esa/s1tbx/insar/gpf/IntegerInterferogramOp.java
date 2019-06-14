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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.*;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * This operator creates an integer interferogram using two interferograms of the same site to increase
 * their clarity or reduce the number of fringes. The combined interferogram can have a better height
 * of ambiguity (HOA) than any of the source interferograms. Here is is assumed that the flat earth phase
 * and topographic phase have been removed from both the input interferograms using the same DEM. And
 * the two interferograms have been co-registered.
 * <p>
 * [1] Reviews of Geophysics, 36, 4 / November 1998, pages 441–500 - Paper number 97RG03139, "RADAR
 *     INTERFEROMETRY AND ITS APPLICATION TO CHANGES IN THE EARTH’S SURFACE" by D. Massonnet, K.L. Feigl.
 */

@OperatorMetadata(alias = "IntegerInterferogram",
        category = "Radar/Interferometric/Products",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Create integer interferogram")
public class IntegerInterferogramOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    Product targetProduct;

    private MetadataElement mstRoot = null;
    private MetadataElement slv1Root = null;
    private MetadataElement slv2Root = null;
    private Band slv1BandI = null;
    private Band slv1BandQ = null;
    private Band slv2BandI = null;
    private Band slv2BandQ = null;
    private Band ifgBandI = null;
    private Band ifgBandQ = null;
    private double hoa1 = 0.0; // height of ambiguity
    private double hoa2 = 0.0; // height of ambiguity
    private int qOpt1 = 0; // integer coefficients
    private int qOpt2 = 0; // integer coefficients


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public IntegerInterferogramOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.core.datamodel.Product}
     * annotated with the {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            checkSourceProductValidity();

            getMetadata();

            getHOA();

            computeOptimalHOA();

            getSourceBands();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkSourceProductValidity() throws IOException {

        final InputProductValidator validator1 = new InputProductValidator(sourceProduct[0]);
        validator1.checkIfSARProduct();
        validator1.checkIfCoregisteredStack();

        if(sourceProduct.length < 2) {
            throw new IOException("Integer Interferogram requires two coregistered stacks as input");
        }

        final InputProductValidator validator2 = new InputProductValidator(sourceProduct[1]);
        validator2.checkIfSARProduct();
        validator2.checkIfCoregisteredStack();
    }

    private void getMetadata() {

        mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);

        MetadataElement slaveElem1 = sourceProduct[0].getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (slaveElem1 == null) {
            throw new OperatorException("Slave_Metadata not found in product " + sourceProduct[0].getName());
        }
        slv1Root = slaveElem1.getElements()[0];

        MetadataElement slaveElem2 = sourceProduct[1].getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (slaveElem2 == null) {
            throw new OperatorException("Slave_Metadata not found in product " + sourceProduct[1].getName());
        }
        slv2Root = slaveElem2.getElements()[0];
    }

    /**
     * Get height of ambiguities
     */
    private void getHOA() throws Exception {

        InSARStackOverview.IfgStack[] stackOverview1 = InSARStackOverview.calculateInSAROverview(
                new MetadataElement[]{mstRoot, slv1Root});

        hoa1 = stackOverview1[0].getMasterSlave()[1].getHeightAmb();
        if (StackUtils.isBiStaticStack(sourceProduct[0])) {
            hoa1 *= 2.0;
        }

        InSARStackOverview.IfgStack[] stackOverview2 = InSARStackOverview.calculateInSAROverview(
                new MetadataElement[]{mstRoot, slv2Root});

        hoa2 = stackOverview2[0].getMasterSlave()[1].getHeightAmb();
        if (StackUtils.isBiStaticStack(sourceProduct[1])) {
            hoa2 *= 2.0;
        }
    }

    private void computeOptimalHOA() {

        double hoaMax = 0.0;
        for (int q1 = -3; q1 <= 3; q1++) {
            for (int q2 = -3; q2 <= 3; q2++) {
                if (q1 == 0 || q2 == 0) {
                    continue;
                }

                final double hoaEq = 1.0 / (q1 / hoa1 + q2 / hoa2);
                if (hoaEq > hoaMax) {
                    hoaMax = hoaEq;
                    qOpt1 = q1;
                    qOpt2 = q2;
                }
            }
        }
    }

    private void getSourceBands() {

        final String mstDate = OperatorUtils.getAcquisitionDate(mstRoot);
        final String slv1Date = OperatorUtils.getAcquisitionDate(slv1Root);
        final String slv2Date = OperatorUtils.getAcquisitionDate(slv2Root);

        final String slv1Tag = mstDate + '_' + slv1Date;
        final String slv2Tag = mstDate + '_' + slv2Date;

        final Band[] srcBands1 = sourceProduct[0].getBands();
        for (Band band : srcBands1) {
            if (band instanceof VirtualBand) {
                continue;
            }

            final String unit = band.getUnit();
            if (band.getName().contains(slv1Tag)) {
                if (unit.equals(Unit.REAL)) {
                    slv1BandI = band;
                } else if (unit.equals(Unit.IMAGINARY)) {
                    slv1BandQ = band;
                }
            }
        }

        final Band[] srcBands2 = sourceProduct[1].getBands();
        for (Band band : srcBands2) {
            if (band instanceof VirtualBand) {
                continue;
            }

            final String unit = band.getUnit();
            if (band.getName().contains(slv2Tag)) {
                if (unit.equals(Unit.REAL)) {
                    slv2BandI = band;
                } else if (unit.equals(Unit.IMAGINARY)) {
                    slv2BandQ = band;
                }
            }
        }

        if (slv1BandI == null || slv1BandQ == null || slv2BandI == null || slv2BandQ == null) {
            throw new OperatorException("Two interferogram products are expected.");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        final int sourceImageWidth = sourceProduct[0].getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct[0].getSceneRasterHeight();

        targetProduct = new Product(
                sourceProduct[0].getName(),
                sourceProduct[0].getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct[0], targetProduct);

        ifgBandI = new Band("i_ifg", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        ifgBandI.setUnit(Unit.REAL);
        targetProduct.addBand(ifgBandI);

        ifgBandQ = new Band("q_ifg", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        ifgBandQ.setUnit(Unit.IMAGINARY);
        targetProduct.addBand(ifgBandQ);

        ReaderUtils.createVirtualIntensityBand(targetProduct, ifgBandI, ifgBandQ, "");
        ReaderUtils.createVirtualPhaseBand(targetProduct, ifgBandI, ifgBandQ, "");
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancellation requests.
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int yMax = y0 + h;
        final int xMax = x0 + w;
        //System.out.println("Do: tx0 = " + tx0 + " ty0 = " + ty0 + " tw = " + tw + " th = " + th);

        try {
            final Tile ifgTileI = targetTileMap.get(ifgBandI);
            final Tile ifgTileQ = targetTileMap.get(ifgBandQ);
            final ProductData ifgDataBufferI = ifgTileI.getDataBuffer();
            final ProductData ifgDataBufferQ = ifgTileQ.getDataBuffer();

            final Tile slv1TileI = getSourceTile(slv1BandI, targetRectangle);
            final Tile slv1TileQ = getSourceTile(slv1BandQ, targetRectangle);
            final ProductData slv1DataBufferI = slv1TileI.getDataBuffer();
            final ProductData slv1DataBufferQ = slv1TileQ.getDataBuffer();

            final Tile slv2TileI = getSourceTile(slv2BandI, targetRectangle);
            final Tile slv2TileQ = getSourceTile(slv2BandQ, targetRectangle);
            final ProductData slv2DataBufferI = slv2TileI.getDataBuffer();
            final ProductData slv2DataBufferQ = slv2TileQ.getDataBuffer();

            final TileIndex tgtIndex = new TileIndex(ifgTileI);
            final TileIndex srcIndex = new TileIndex(slv1TileI);

            for (int y = y0; y < yMax; ++y) {
                tgtIndex.calculateStride(y);
                srcIndex.calculateStride(y);

                for (int x = x0; x < xMax; ++x) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int srcIdx = srcIndex.getIndex(x);

                    final double slv1IfgI = slv1DataBufferI.getElemDoubleAt(srcIdx);
                    final double slv1IfgQ = slv1DataBufferQ.getElemDoubleAt(srcIdx);
                    final double slv2IfgI = slv2DataBufferI.getElemDoubleAt(srcIdx);
                    final double slv2IfgQ = slv2DataBufferQ.getElemDoubleAt(srcIdx);

                    ifgDataBufferI.setElemFloatAt(tgtIdx, (float)(qOpt1*slv1IfgI + qOpt2*slv2IfgI));
                    ifgDataBufferQ.setElemFloatAt(tgtIdx, (float)(qOpt1*slv1IfgQ + qOpt2*slv2IfgQ));
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(IntegerInterferogramOp.class);
        }
    }
}