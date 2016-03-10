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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
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
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;
import org.jlinda.nest.gpf.coregistration.GCPManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This operator computes velocities for master-slave GCP pairs. Than velocities for all pixels are computed
 * through interpolation.
 */

@OperatorMetadata(alias = "Offset-Tracking",
        category = "Radar/Feature Extraction",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Create velocity vectors from offset tracking")
public class OffsetTrackingOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "5.0",
            label = "Max Velocity (m/day)")
    private float maxVelocity = 5.0f;

    @Parameter(description = "Output range and azimuth shifts", defaultValue = "false",
            label = "Output range and azimuth shifts")
    private boolean outputRangeAzimuthOffset = false;

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
    private final Map<Band, VelocityData[]> velocityMap = new HashMap<>(10);
    private final static double invalidIndex = -9999.0;

    private final static String PRODUCT_SUFFIX = "_Vel";
    private final static String VELOCITY = "Velocity";
    private final static String POINTS = "Points";
    private final static String RANGE_SHIFT = "Range_Shift";
    private final static String AZIMUTH_SHIFT = "Azimuth_Shift";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public OffsetTrackingOp() {
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
        for (String bandName : masterBandNames) {
            final String mstPol = OperatorUtils.getPolarizationFromBandName(bandName);
            if (mstPol != null && (mstPol.equals("hh") || mstPol.equals("vv"))) {
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

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
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
                final String suffix = StackUtils.getBandSuffix(srcBand.getName());
                final String velocityBandName = VELOCITY + suffix;
                if (targetProduct.getBand(velocityBandName) == null) {
                    targetBand = targetProduct.addBand(velocityBandName, ProductData.TYPE_FLOAT32);
                    targetBand.setUnit(Unit.METERS_PER_DAY);
                    targetBand.setDescription("Velocity");
                    sourceRasterMap.put(targetBand, srcBand);

                    targetProduct.setQuicklookBandName(targetBand.getName());
                }

                final String gcpPositionBandName = POINTS + suffix;
                if (targetProduct.getBand(gcpPositionBandName) == null) {
                    targetBand = targetProduct.addBand(gcpPositionBandName, ProductData.TYPE_FLOAT32);
                    targetBand.setUnit(Unit.METERS_PER_DAY);
                    targetBand.setDescription("Velocity Points");
                    sourceRasterMap.put(targetBand, srcBand);
                }

                if (outputRangeAzimuthOffset) {
                    final String rangeShiftBandName = RANGE_SHIFT + suffix;
                    if (targetProduct.getBand(rangeShiftBandName) == null) {
                        targetBand = targetProduct.addBand(rangeShiftBandName, ProductData.TYPE_FLOAT32);
                        targetBand.setUnit(Unit.METERS_PER_DAY);
                        targetBand.setDescription("Range Shift");
                        sourceRasterMap.put(targetBand, srcBand);
                    }

                    final String azimuthShiftBandName = AZIMUTH_SHIFT + suffix;
                    if (targetProduct.getBand(azimuthShiftBandName) == null) {
                        targetBand = targetProduct.addBand(azimuthShiftBandName, ProductData.TYPE_FLOAT32);
                        targetBand.setUnit(Unit.METERS_PER_DAY);
                        targetBand.setDescription("Azimuth Shift");
                        sourceRasterMap.put(targetBand, srcBand);
                    }
                }
            }
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
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        //System.out.println("OffsetTrackingOp: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (pm.isCanceled())
                return;

            Band tgtRangeShiftBand = null;
            Band tgtAzimuthShiftBand = null;
            Band tgtVelocityBand = null;
            Band tgtGCPPositionBand = null;
            ProductData tgtRangeShiftBuffer = null;
            ProductData tgtAzimuthShiftBuffer = null;
            ProductData tgtVelocityBuffer = null;
            ProductData tgtGCPPositionBuffer = null;
            final Band[] targetBands = targetProduct.getBands();
            for (Band tgtBand:targetBands) {
                final String tgtBandName = tgtBand.getName();
                if (tgtBandName.contains(RANGE_SHIFT)) {
                    tgtRangeShiftBand = tgtBand;
                    tgtRangeShiftBuffer = targetTileMap.get(tgtRangeShiftBand).getDataBuffer();
                } else if (tgtBandName.contains(AZIMUTH_SHIFT)) {
                    tgtAzimuthShiftBand = tgtBand;
                    tgtAzimuthShiftBuffer = targetTileMap.get(tgtAzimuthShiftBand).getDataBuffer();
                } else if (tgtBandName.contains(VELOCITY)) {
                    tgtVelocityBand = tgtBand;
                    tgtVelocityBuffer = targetTileMap.get(tgtVelocityBand).getDataBuffer();
                } else if (tgtBandName.contains(POINTS)) {
                    tgtGCPPositionBand = tgtBand;
                    tgtGCPPositionBuffer = targetTileMap.get(tgtGCPPositionBand).getDataBuffer();
                }
            }

            final Band srcBand = sourceRasterMap.get(tgtVelocityBand);
            if (!GCPVelocityAvailable) {
                getGCPVelocity(srcBand, targetRectangle);
            }

            final VelocityData[] velocityList = velocityMap.get(srcBand);
            final FastDelaunayTriangulator FDT = triangulatorMap.get(srcBand);
            if (FDT == null)
                return;

            // output velocity, range shift and azimuth shift
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, yMax - 1, x0, xMax - 1);
            final double[][] rangeShiftArray = new double[h][w];
            final double[][] azimuthShiftArray = new double[h][w];
            final double[][] velocityArray = new double[h][w];

            TriangleUtils.interpolate(rgAzRatio, tileWindow, 1, 1, 0, invalidIndex, FDT,
                    velocityList, rangeShiftArray, azimuthShiftArray, velocityArray);

            final Tile tgtTile = targetTileMap.get(tgtVelocityBand);
            final TileIndex tgtIndex = new TileIndex(tgtTile);
            if (tgtVelocityBuffer != null) {
                for (int y = y0; y < yMax; y++) {
                    tgtIndex.calculateStride(y);
                    final int yy = y - y0;
                    for (int x = x0; x < xMax; x++) {
                        tgtVelocityBuffer.setElemFloatAt(tgtIndex.getIndex(x), (float) velocityArray[yy][x - x0]);
                    }
                }
            }

            if (outputRangeAzimuthOffset && tgtRangeShiftBuffer!= null && tgtAzimuthShiftBuffer != null) {
                for (int y = y0; y < yMax; y++) {
                    tgtIndex.calculateStride(y);
                    final int yy = y - y0;
                    for (int x = x0; x < xMax; x++) {
                        tgtRangeShiftBuffer.setElemFloatAt(tgtIndex.getIndex(x), (float) rangeShiftArray[yy][x - x0]);
                    }
                }

                for (int y = y0; y < yMax; y++) {
                    tgtIndex.calculateStride(y);
                    final int yy = y - y0;
                    for (int x = x0; x < xMax; x++) {
                        tgtAzimuthShiftBuffer.setElemFloatAt(tgtIndex.getIndex(x), (float) azimuthShiftArray[yy][x - x0]);
                    }
                }
            }

            // output GCP positions
            if (tgtGCPPositionBuffer != null) {
                for (VelocityData data : velocityList) {
                    final int x = (int) data.mstGCPx;
                    final int y = (int) data.mstGCPy;
                    if (x >= x0 && x < xMax && y >= y0 && y < yMax) {
                        tgtGCPPositionBuffer.setElemFloatAt(tgtTile.getDataBufferIndex(x, y), (float) data.velocity);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void getGCPVelocity(final Band sourceBand, final Rectangle targetRectangle) throws Exception {

        if (GCPVelocityAvailable) {
            return;
        }

        // force getSourceTile to computeTiles on GCPSelection
        final Tile sourceRaster = getSourceTile(sourceBand, targetRectangle);

        final ProductNodeGroup<Placemark> masterGCPGroup = GCPManager.instance().getGcpGroup(masterBand);

        final Band[] targetBands = targetProduct.getBands();
        for (Band tgtBand : targetBands) {
            if (!tgtBand.getName().contains(VELOCITY))
                continue;

            final Band srcBand = sourceRasterMap.get(tgtBand);

            ProductNodeGroup<Placemark> slaveGCPGroup = GCPManager.instance().getGcpGroup(srcBand);

            if (slaveGCPGroup.getNodeCount() > 0) {
                VelocityData[] velocityList = computeGCPVelocity(masterGCPGroup, slaveGCPGroup);
                velocityMap.put(srcBand, velocityList);

                FastDelaunayTriangulator FDT = TriangleUtils.triangulate(velocityList, rgAzRatio, invalidIndex);
                triangulatorMap.put(srcBand, FDT);
            }
        }

        if(velocityMap.isEmpty()) {
            throw new OperatorException("No velocity GCPs found");
        }

        GCPManager.instance().removeAllGcpGroups();

        writeGCPsToMetadata();

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
            final double v = Math.sqrt(rangeShift * rangeShift + azimuthShift * azimuthShift) / acquisitionTimeInterval;

            // eliminate outliers
            if (v < maxVelocity) {
                velocityList.add(new VelocityData(mGCPPos.x, mGCPPos.y, sGCPPos.x, sGCPPos.y, rangeShift, azimuthShift, v));
            }
        }

        return velocityList.toArray(new VelocityData[velocityList.size()]);
    }

    private void writeGCPsToMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final Set<Band> bandSet = velocityMap.keySet();

        for (Band srcBand : bandSet) {
            final String suffix = StackUtils.getBandSuffix(srcBand.getName());
            final String velocityBandName = VELOCITY + suffix;

            final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, velocityBandName, true);

            MetadataElement warpDataElem = bandElem.getElement("WarpData");
            if (warpDataElem == null) {
                warpDataElem = new MetadataElement("WarpData");
                bandElem.addElement(warpDataElem);
            } else {
                // empty out element
                final MetadataAttribute[] attribList = warpDataElem.getAttributes();
                for (MetadataAttribute attrib : attribList) {
                    warpDataElem.removeAttribute(attrib);
                }
            }

            final VelocityData[] velocityList = velocityMap.get(srcBand);
            if (velocityList.length > 0) {
                for (int i = 0; i < velocityList.length; i++) {
                    final MetadataElement gcpElem = new MetadataElement("GCP" + i);
                    warpDataElem.addElement(gcpElem);

                    gcpElem.setAttributeDouble("mst_x", velocityList[i].mstGCPx);
                    gcpElem.setAttributeDouble("mst_y", velocityList[i].mstGCPy);
                    gcpElem.setAttributeDouble("slv_x", velocityList[i].slvGCPx);
                    gcpElem.setAttributeDouble("slv_y", velocityList[i].slvGCPy);
                }
            }
        }
    }

    private static class TriangleUtils {

        public static FastDelaunayTriangulator triangulate(
                final VelocityData[] velocityList, final double xyRatio, final double invalidIndex)
                throws Exception {

            java.util.List<Geometry> list = new ArrayList<>();
            GeometryFactory gf = new GeometryFactory();

            for (int i = 0; i < velocityList.length; i++) {
                VelocityData data = velocityList[i];
                if (data.mstGCPy == invalidIndex || data.mstGCPx == invalidIndex) {
                    continue;
                }
                list.add(gf.createPoint(new Coordinate(data.mstGCPy, data.mstGCPx * xyRatio, i)));
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
                                       final VelocityData[] velocityList,
                                       final double[][] z1_out, final double[][] z2_out, final double[][] z3_out) {

            final double x_min = tileWindow.linelo;
            final double y_min = tileWindow.pixlo;

            int i, j; // counters
            long i_min, i_max, j_min, j_max; // minimas/maximas
            double xp, yp;
            double xkj, ykj, xlj, ylj;
            double f; // function

            // containers for xy coordinates of Triangles: p1-p2-p3-p1
            double[] vx = new double[4];
            double[] vy = new double[4];
            double[] vz = new double[3];
            int[] idx = new int[3];

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

                idx[0] = (int)triangle.getA().z;
                idx[1] = (int)triangle.getB().z;
                idx[2] = (int)triangle.getC().z;

                vz[0] = velocityList[idx[0]].rangeShift;
                vz[1] = velocityList[idx[1]].rangeShift;
                vz[2] = velocityList[idx[2]].rangeShift;
                double[] abc1 = getABC(vx, vy, vz, f, xkj, ykj, xlj, ylj);

                vz[0] = velocityList[idx[0]].azimuthShift;
                vz[1] = velocityList[idx[1]].azimuthShift;
                vz[2] = velocityList[idx[2]].azimuthShift;
                double[] abc2 = getABC(vx, vy, vz, f, xkj, ykj, xlj, ylj);

                vz[0] = velocityList[idx[0]].velocity;
                vz[1] = velocityList[idx[1]].velocity;
                vz[2] = velocityList[idx[2]].velocity;
                double[] abc3 = getABC(vx, vy, vz, f, xkj, ykj, xlj, ylj);

                for (i = (int) i_min; i <= i_max; i++) {
                    xp = indexToCoord(i, x_min, xScale, offset);
                    for (j = (int) j_min; j <= j_max; j++) {
                        yp = indexToCoord(j, y_min, yScale, offset);

                        if (!pointInTriangle(vx, vy, xp, yp)) {
                            continue;
                        }

                        z1_out[i][j] = abc1[0] * xp + abc1[1] * yp + abc1[2];
                        z2_out[i][j] = abc2[0] * xp + abc2[1] * yp + abc2[2];
                        z3_out[i][j] = abc3[0] * xp + abc3[1] * yp + abc3[2];
                    }
                }
            }
        }

        private static double[] getABC(
                final double[] vx, final double[] vy, final double[] vz,
                final double f, final double  xkj, final double ykj, final double xlj, final double ylj) {

            final double zj = vz[0];
            final double zk = vz[1];
            final double zl = vz[2];
            final double zkj = zk - zj;
            final double zlj = zl - zj;

            final double[] abc = new double[3];
            abc[0] = -f * (ykj * zlj - zkj * ylj);
            abc[1] = -f * (zkj * xlj - xkj * zlj);
            abc[2] = -abc[0] * vx[1] - abc[1] * vy[1] + zk;

            return abc;
        }

        private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
            int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
            int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
            int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }

        private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
            return irint((((coord - coord0) / (deltaCoord)) - offset));
        }

        private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
            return (coord0 + idx * deltaCoord + offset);
        }

        private static long irint(final double coord) {
            return ((long) rint(coord));
        }

        private static double rint(final double coord) {
            return Math.floor(coord + 0.5);
        }
    }

    public static class VelocityData {

        public double mstGCPx;
        public double mstGCPy;
        public double slvGCPx;
        public double slvGCPy;
        public double velocity;
        public double rangeShift;
        public double azimuthShift;

        public VelocityData(final double mstGCPx, final double mstGCPy, final double slvGCPx, final double slvGCPy,
                            final double rangeShift, final double azimuthShift, final double velocity) {

            this.mstGCPx = mstGCPx;
            this.mstGCPy = mstGCPy;
            this.slvGCPx = slvGCPx;
            this.slvGCPy = slvGCPy;
            this.rangeShift = rangeShift;
            this.azimuthShift = azimuthShift;
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
            super(OffsetTrackingOp.class);
        }
    }
}
