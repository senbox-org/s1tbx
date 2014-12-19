/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.*;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.ProductInformation;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.nest.gpf.geometric.SARGeocoding;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.StackUtils;
import org.esa.snap.gpf.TileIndex;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;


import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * Compute deramp and demodulation phases.
 */
@OperatorMetadata(alias = "Compute-Deramp-Demod-Phase",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Compute Deramp and Demodulation Phases")
public final class ComputeDerampDemodPhaseOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;

	private int subSwathIndex = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ComputeDerampDemodPhaseOp() {
    }

    /**
     * Compute range dependent Doppler rate Kt(r) for given burst.
     * @return The Doppler rate array.
     */
    public float[] computeDopplerRate(final int burstIndex) throws Exception {
        float[] kt = new float[sourceImageWidth];
        for (int x = 0; x < sourceImageWidth; x++) {
            kt[x] = (float)subSwath[0].dopplerRate[burstIndex][x];
        }
        return kt;
    }

    /**
     * Compute range dependent Doppler centroid fDC(r) for given burst.
     * @return The Doppler centroid array.
     */
    public float[] computeDopplerCentroid(final int burstIndex) throws Exception {
        float[] fdc = new float[sourceImageWidth];
        for (int x = 0; x < sourceImageWidth; x++) {
            fdc[x] = (float)subSwath[0].dopplerCentroid[burstIndex][x];
        }
        return fdc;
    }

    /**
     * Compute slant range.
     * @return The slant range array.
     */
    public float[] computeSlantRange() throws Exception {
        float[] slr = new float[sourceImageWidth];
        for (int x = 0; x < sourceImageWidth; x++) {
            slr[x] = (float)(subSwath[0].slrTimeToFirstPixel * Constants.lightSpeed +
                    x * subSwath[subSwathIndex - 1].rangePixelSpacing);

        }
        return slr;
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
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            checkSourceProductValidity();

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            su.computeReferenceTime();
            subSwath = su.getSubSwath();

			final String[] subSwathNames = su.getSubSwathNames();
			if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            }

			subSwathIndex = 1; // subSwathIndex is always 1 because of split product

            final String[] polarizations = su.getPolarizations();
			if (polarizations.length != 1) {
                throw new OperatorException("Split product with one polarization is expected.");
            }

            createTargetProduct();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() {

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException("Source product has invalid mission for Sentinel1 product");
        }

        final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!productType.equals("SLC")) {
            throw new OperatorException("Source product should be SLC product");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(
                sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        final Band derampPhaseBand = new Band(
                "derampPhase",
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        derampPhaseBand.setUnit("radian");
        targetProduct.addBand(derampPhaseBand);

        final Band demodPhaseBand = new Band(
                "demodPhase",
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        demodPhaseBand.setUnit("radian");
        targetProduct.addBand(demodPhaseBand);
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
    @Override
     public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
             throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final int tyMax = ty0 + th;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            for (int burstIndex = 0; burstIndex < subSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + subSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (tyMax <= firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

				final int ntx0 = tx0;
				final int ntw = tw;
                final int nty0 = Math.max(ty0, firstLineIdx);
                final int ntyMax = Math.min(tyMax, lastLineIdx + 1);
                final int nth = ntyMax - nty0;
                System.out.println("burstIndex = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                computeDerampDemodPhase(
                        subSwathIndex, burstIndex, ntx0, nty0, ntw, nth, targetTileMap, pm);
            }

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void computeDerampDemodPhase(final int subSwathIndex, final int burstIndex,
                                    final int x0, final int y0, final int w, final int h,
                                    final Map<Band, Tile> targetTileMap,
                                    ProgressMonitor pm)
            throws Exception {

        try {
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            final int s = subSwathIndex - 1;

            final Band derampPhaseBand = targetProduct.getBand("derampPhase");
            final Band demodPhaseBand = targetProduct.getBand("demodPhase");
            final Tile tgtTileDerampPhase = targetTileMap.get(derampPhaseBand);
            final Tile tgtTileDemodPhase = targetTileMap.get(demodPhaseBand);
            final ProductData tgtBufferDerampPhase = tgtTileDerampPhase.getDataBuffer();
            final ProductData tgtBufferDemodPhase = tgtTileDemodPhase.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileDerampPhase);

            final int firstLineInBurst = burstIndex*subSwath[s].linesPerBurst;
            for (int y = y0; y < yMax; y++) {
                tgtIndex.calculateStride(y);
                final double ta = (y - firstLineInBurst)*subSwath[s].azimuthTimeInterval;
                for (int x = x0; x < xMax; x++) {
                    final int idx = tgtIndex.getIndex(x);
                    final double kt = subSwath[s].dopplerRate[burstIndex][x]; // DLR: 1780.8765
                    final double deramp = -Math.PI * kt * Math.pow(ta - subSwath[s].referenceTime[burstIndex][x], 2);
                    final double demod = -2 * Math.PI * subSwath[s].dopplerCentroid[burstIndex][x] * ta;
                    tgtBufferDerampPhase.setElemFloatAt(idx, (float)deramp);
                    tgtBufferDemodPhase.setElemFloatAt(idx, (float)demod);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeDerampDemodPhase", e);
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
            super(ComputeDerampDemodPhaseOp.class);
        }
    }
}