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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This operator computes velocities for master-slave GCP pairs. Than velocities for all pixels are computed
 * through interpolation.
 */

@OperatorMetadata(alias = "Show-Movement",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Create Warp Function And Get Co-registrated Images")
public class ShowMovementOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "5.0",
            label = "Max Velocity (m/day)")
    private float maxVelocity = 5.0f;

    private Band masterBand = null;
    private boolean GCPVelocityAvailable = false;
    private MetadataElement mstAbsRoot = null;
    private MetadataElement slvAbsRoot = null;
    private double mstFirstLineTime = 0.0;
    private double slvFirstLineTime = 0.0;
    private double acquisitionTimeInterval = 0.0;
    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double rgAzRatio = 0.0;

    private String processedSlaveBand;
    private String[] masterBandNames = null;
    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, FastDelaunayTriangulator> triangulatorMap = new HashMap<>(10);
    private final double invalidIndex = -9999.0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ShowMovementOp() {
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
            validator.checkIfCoregisteredStack();

            getMasterMetadata();

            getSlaveMetadata();

            acquisitionTimeInterval = slvFirstLineTime - mstFirstLineTime; // in days

            rgAzRatio = rangeSpacing / azimuthSpacing;

            getMasterBands();

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterMetadata() throws Exception {

        mstAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mstFirstLineTime = AbstractMetadata.parseUTC(
                mstAbsRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        processedSlaveBand = mstAbsRoot.getAttributeString("processed_slave");

        rangeSpacing = AbstractMetadata.getAttributeDouble(mstAbsRoot, AbstractMetadata.range_spacing);

        azimuthSpacing = AbstractMetadata.getAttributeDouble(mstAbsRoot, AbstractMetadata.azimuth_spacing);
    }

    private void getSlaveMetadata() {

        slvAbsRoot = AbstractMetadata.getSlaveMetadata(sourceProduct.getMetadataRoot()).getElementAt(0);

        slvFirstLineTime = AbstractMetadata.parseUTC(
                slvAbsRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days
    }

    private void getMasterBands() {

        String mstBandName = sourceProduct.getBandAt(0).getName();

        // find co-pol bands
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for(String bandName : masterBandNames) {
            final String mstPol = OperatorUtils.getPolarizationFromBandName(bandName);
            if(mstPol != null && (mstPol.equals("hh") || mstPol.equals("vv"))) {
                mstBandName = bandName;
                break;
            }
        }
        masterBand = sourceProduct.getBand(mstBandName);
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
        Band targetBand;
        for (int i = 0; i < numSrcBands; i++) {
            final Band srcBand = sourceProduct.getBandAt(i);
            if (srcBand == masterBand || StringUtils.contains(masterBandNames, srcBand.getName())) {
                targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                targetBand = targetProduct.addBand(srcBand.getName(), ProductData.TYPE_FLOAT32);
                ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            }
            sourceRasterMap.put(targetBand, srcBand);
        }

        // co-registered image should have the same geo-coding as the master image
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        //System.out.println("ShowMovementOp: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!GCPVelocityAvailable) {
                getGCPVelocity(targetRectangle);
            }

            final Band srcBand = sourceRasterMap.get(targetBand);
            if (srcBand == null)
                return;

            if (pm.isCanceled())
                return;

            final FastDelaunayTriangulator FDT = triangulatorMap.get(srcBand);
            if (FDT == null)
                return;

            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, yMax - 1, x0, xMax - 1);
            final double[][] velocityArray = new double[h][w];

            TriangleUtils.interpolate(rgAzRatio, tileWindow, 1, 1, 0, invalidIndex, FDT, velocityArray);

            final ProductData targetBuffer = targetTile.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(targetTile);
            for (int y = y0; y < yMax; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; x++) {
                    targetBuffer.setElemFloatAt(tgtIndex.getIndex(x), (float)velocityArray[yy][x - x0]);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void getGCPVelocity(final Rectangle targetRectangle) throws Exception {

        if (GCPVelocityAvailable) {
            return;
        }

        final Band targetBand = targetProduct.getBand(processedSlaveBand);
        if(targetBand == null) {
            throw new OperatorException(processedSlaveBand + " band not found");
        }
        // force getSourceTile to computeTiles on GCPSelection
        final Tile sourceRaster = getSourceTile(sourceRasterMap.get(targetBand), targetRectangle);

        final ProductNodeGroup<Placemark> masterGCPGroup = GCPManager.instance().getGcpGroup(masterBand);

        final int numSrcBands = sourceProduct.getNumBands();
        for (int i = 0; i < numSrcBands; i++) {
            final Band srcBand = sourceProduct.getBandAt(i);
            if (srcBand == masterBand || StringUtils.contains(masterBandNames, srcBand.getName()))
                continue;

            ProductNodeGroup<Placemark> slaveGCPGroup = GCPManager.instance().getGcpGroup(srcBand);

            VelocityData[] velocityList = computeGCPVelocity(masterGCPGroup, slaveGCPGroup);

            FastDelaunayTriangulator FDT = TriangleUtils.triangulate(velocityList, rgAzRatio, invalidIndex);

            triangulatorMap.put(srcBand, FDT);
        }

        GCPManager.instance().removeAllGcpGroups();

        GCPVelocityAvailable = true;
    }

    private VelocityData[] computeGCPVelocity(final ProductNodeGroup<Placemark> masterGCPGroup,
                                    final ProductNodeGroup<Placemark> slaveGCPGroup) {
        final int numGCPs = slaveGCPGroup.getNodeCount();
        final List<VelocityData> velocityList = new ArrayList<>(numGCPs);
        for (int i = 0; i < numGCPs; i++) {
            final Placemark sPin = slaveGCPGroup.get(i);
            final PixelPos sGCPPos = sPin.getPixelPos();

            final Placemark mPin = masterGCPGroup.get(masterGCPGroup.indexOf(sPin.getName()));
            final PixelPos mGCPPos = mPin.getPixelPos();

            final double rangeShift = (mGCPPos.x - sGCPPos.x) * rangeSpacing;
            final double azimuthShift = (mGCPPos.y - sGCPPos.y) * azimuthSpacing;
            final double v = Math.sqrt(rangeShift*rangeShift + azimuthShift*azimuthShift) / acquisitionTimeInterval;

            // eliminate outliers
            if (v < maxVelocity) {
                final VelocityData data = new VelocityData(mGCPPos.x, mGCPPos.y, sGCPPos.x, sGCPPos.y, v);
                velocityList.add(data);
            }
        }

        return velocityList.toArray(new VelocityData[velocityList.size()]);
    }

    private static class TriangleUtils {

        public static FastDelaunayTriangulator triangulate(
                final VelocityData[] velocityList, final double xyRatio, final double invalidIndex)
                throws Exception {

            java.util.List<Geometry> list = new ArrayList<>();
            GeometryFactory gf = new GeometryFactory();

            for (VelocityData data : velocityList) {
                if (data.mstGCPy == invalidIndex || data.mstGCPx == invalidIndex) {
                    continue;
                }
                list.add(gf.createPoint(new Coordinate(data.mstGCPy, data.mstGCPx * xyRatio, data.velocity)));
            }

            if (list.size() < 3) {
                return null;
            }

            FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
            try {
                FDT.triangulate(list.iterator());
            } catch (TriangulationException te) {
                te.printStackTrace();
            }

            return FDT;
        }

        public static void interpolate(final double xyRatio, final org.jlinda.core.Window tileWindow,
                                       final double xScale, final double yScale, final double offset,
                                       final double invalidIndex, FastDelaunayTriangulator FDT,
                                       final double[][] z_out) {

            final double x_min = tileWindow.linelo;
            final double y_min = tileWindow.pixlo;

            int i, j; // counters
            long i_min, i_max, j_min, j_max; // minimas/maximas
            double xp, yp;
            double xkj, ykj, xlj, ylj;
            double f; // function
            double zj, zk, zl, zkj, zlj;
            double a, b, c;

            // containers for xy coordinates of Triangles: p1-p2-p3-p1
            double[] vx = new double[4];
            double[] vy = new double[4];

            // declare demRadarCode_phase
            final int nx = (int) tileWindow.lines();
            final int ny = (int) tileWindow.pixels();

            // interpolate: loop over triangles
            for (Triangle triangle : FDT.triangles) {

                // store triangle coordinates in local variables
                vx[0] = vx[3] = triangle.getA().x;
                vy[0] = vy[3] = triangle.getA().y / xyRatio;

                vx[1] = triangle.getB().x;
                vy[1] = triangle.getB().y / xyRatio;

                vx[2] = triangle.getC().x;
                vy[2] = triangle.getC().y / xyRatio;

                // skip invalid indices
                if (vx[0] == invalidIndex || vx[1] == invalidIndex || vx[2] == invalidIndex ||
                        vy[0] == invalidIndex || vy[1] == invalidIndex || vy[2] == invalidIndex) {
                    continue;
                }

                // Compute grid indices the current triangle may cover
                xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
                i_min = coordToIndex(xp, x_min, xScale, offset);

                xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
                i_max = coordToIndex(xp, x_min, xScale, offset);

                yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
                j_min = coordToIndex(yp, y_min, yScale, offset);

                yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
                j_max = coordToIndex(yp, y_min, yScale, offset);

                // skip triangle that is above or below the region
                if ((i_max < 0) || (i_min >= nx)) {
                    continue;
                }

                // skip triangle that is on the left or right of the region
                if ((j_max < 0) || (j_min >= ny)) {
                    continue;
                }

                // triangle covers the upper or lower boundary
                if (i_min < 0) {
                    i_min = 0;
                }

                if (i_max >= nx) {
                    i_max = nx - 1;
                }

                // triangle covers left or right boundary
                if (j_min < 0) {
                    j_min = 0;
                }

                if (j_max >= ny) {
                    j_max = ny - 1;
                }

                // compute plane defined by the three vertices of the triangle: z = ax + by + c
                xkj = vx[1] - vx[0];
                ykj = vy[1] - vy[0];
                xlj = vx[2] - vx[0];
                ylj = vy[2] - vy[0];

                f = 1.0 / (xkj * ylj - ykj * xlj);

                zj = triangle.getA().z;
                zk = triangle.getB().z;
                zl = triangle.getC().z;
                zkj = zk - zj;
                zlj = zl - zj;
                a = -f * (ykj * zlj - zkj * ylj);
                b = -f * (zkj * xlj - xkj * zlj);
                c = -a * vx[1] - b * vy[1] + zk;

                for (i = (int) i_min; i <= i_max; i++) {
                    xp = indexToCoord(i, x_min, xScale, offset);
                    for (j = (int) j_min; j <= j_max; j++) {
                        yp = indexToCoord(j, y_min, yScale, offset);

                        if (!pointInTriangle(vx, vy, xp, yp)) {
                            continue;
                        }

                        z_out[i][j] = a * xp + b * yp + c;
                    }
                }
            }
        }

        private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
            int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
            int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
            int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }

        private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
            return (long)Math.floor((((coord - coord0) / (deltaCoord)) - offset) + 0.5);
        }

        private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
            return (coord0 + idx * deltaCoord + offset);
        }
    }

    public static class VelocityData {

        public double mstGCPx;
        public double mstGCPy;
        public double slvGCPx;
        public double slvGCPy;
        public double velocity;

        public VelocityData(final double mstGCPx, final double mstGCPy, final double slvGCPx, final double slvGCPy,
                            final double velocity) {

            this.mstGCPx = mstGCPx;
            this.mstGCPy = mstGCPy;
            this.slvGCPx = slvGCPx;
            this.slvGCPy = slvGCPy;
            this.velocity = velocity;
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ShowMovementOp.class);
        }
    }

}
