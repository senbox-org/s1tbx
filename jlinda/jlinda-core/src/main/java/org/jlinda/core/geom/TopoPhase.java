package org.jlinda.core.geom;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.SystemUtils;
import org.jlinda.core.*;
import org.jlinda.core.Point;
import org.jlinda.core.Window;
import org.jlinda.core.delaunay.TriangleInterpolator;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.ProductContainer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public class TopoPhase {

    //// logger
    static Logger logger = SystemUtils.LOG;

    private Orbit masterOrbit;   // master
    private SLCImage masterMeta; // master
    private Orbit slaveOrbit;    // slave
    private SLCImage slaveMeta;  // slave

    private Window tileWindow;    // buffer/tile coordinates

    private DemTile dem;           // demTileData
    public double[][] demPhase;
    public double[][] elevation;
    public double[][] latitude;
    public double[][] longitude;

    private double[][] demRadarCode_x;
    private double[][] demRadarCode_y;
    private double[][] demRadarCode_phase; // interpolated grid
    private double[][] demElevation;
    private double[][] demLatitude;
    private double[][] demLongitude;

    private int nRows;
    private int nCols;

    private double rngAzRatio = 0;
    private boolean isBiStaticStack = false;
    private static final double invalidIndex = -9999.0;

    public TopoPhase(SLCImage masterMeta, Orbit masterOrbit, SLCImage slaveMeta, Orbit slaveOrbit, Window window,
                     DemTile demTile) throws Exception {
        this.masterOrbit = masterOrbit;
        this.masterMeta = masterMeta;
        this.slaveOrbit = slaveOrbit;
        this.slaveMeta = slaveMeta;
        this.tileWindow = window;
        this.dem = demTile;

        nRows = dem.data.length;
        nCols = dem.data[0].length;

        isBiStaticStack = masterMeta.isBiStaticStack;
    }

    public void setMasterOrbit(Orbit masterOrbit) {
        this.masterOrbit = masterOrbit;
    }

    public void setMasterMeta(SLCImage masterMeta) {
        this.masterMeta = masterMeta;
    }

    public void setSlaveOrbit(Orbit slaveOrbit) {
        this.slaveOrbit = slaveOrbit;
    }

    public void setSlaveMeta(SLCImage slaveMeta) {
        this.slaveMeta = slaveMeta;
    }

    public void setWindow(Window window) {
        this.tileWindow = window;
    }

    public double[][] getDemRadarCode_phase() {
        return demRadarCode_phase;
    }

    public double[][] getDemRadarCode_x() {
        return demRadarCode_x;
    }

    public double[][] getDemRadarCode_y() {
        return demRadarCode_y;
    }

    public double getRngAzRatio() {
        return rngAzRatio;
    }

    public void setRngAzRatio(double rngAzRatio) {
        this.rngAzRatio = rngAzRatio;
    }

    public void radarCode(final boolean useInvalidIndex) throws Exception {

        //logger.info("Converting DEM to radar system for this tile.");

        demRadarCode_x = new double[nRows][nCols];
        demRadarCode_y = new double[nRows][nCols];
        demRadarCode_phase = new double[nRows][nCols];
        demElevation = new double[nRows][nCols];
        demLatitude = new double[nRows][nCols];
        demLongitude = new double[nRows][nCols];

        final int nPoints = nRows * nCols;
        final boolean onlyTopoRefPhase = true;

        //logger.info("Number of points in DEM: " + nPoints);

        double masterMin4piCDivLam = (-4 * Math.PI * Constants.SOL) / masterMeta.getRadarWavelength();
        double slaveMin4piCDivLam = (-4 * Math.PI * Constants.SOL) / slaveMeta.getRadarWavelength();

        double phi, lambda, height, line, pix, ref_phase;

        final double upperLeftPhi = dem.lat0;// - dem.indexPhi0DEM * dem.latitudeDelta;
        final double upperLeftLambda = dem.lon0;// + dem.indexLambda0DEM * dem.longitudeDelta;

        Point pointOnDem;
        Point slaveTime;

        phi = upperLeftPhi;
        for (int i = 0; i < nRows; i++) {

//            if ((i % 100) == 0) {
//                logger.info("Radarcoding DEM line: " + i + " (" + Math.floor(.5 + (100. * (double) i / (double) (nRows))) + "%");
//            }

            lambda = upperLeftLambda;
            double[] heightArray = dem.data[i];

            for (int j = 0; j < nCols; j++) {

                height = heightArray[j];
                demElevation[i][j] = height;
                demLatitude[i][j] = phi;
                demLongitude[i][j] = lambda;

                if (height != dem.noDataValue) {

                    double[] phi_lam_height = {phi, lambda, height};
                    Point sarPoint = masterOrbit.ell2lp(phi_lam_height, masterMeta);

                    line = sarPoint.y;
                    pix = sarPoint.x;

                    demRadarCode_y[i][j] = line;
                    demRadarCode_x[i][j] = pix;

                    pointOnDem = Ellipsoid.ell2xyz(phi_lam_height);
                    slaveTime = slaveOrbit.xyz2t(pointOnDem, slaveMeta);
/*
                if (outH2PH == true) {

                    // compute h2ph factor
                    Point masterSatPos = masterOrbit.getXYZ(masterTime.y);
                    Point slaveSatpOS = slaveOrbit.getXYZ(slaveTime.y);
                    double B = masterSatPos.distance(slaveSatpOS);
                    double Bpar = Constants.SOL * (masterTime.x - masterTime.y);
                    Point rangeDistToMaster = masterSatPos.min(pointOnDem);
                    Point rangeDistToSlave = slaveSatpOS.min(pointOnDem);
                    // double thetaMaster = masterSatPos.angle(rangeDistToMaster);  // look angle - not needed
                    double thetaMaster = pointOnDem.angle(rangeDistToMaster); // incidence angle
                    double thetaSlave = pointOnDem.angle(rangeDistToSlave); // incidence angle slave
                    double Bperp = (thetaMaster > thetaSlave) ? // sign ok
                            Math.sqrt(MathUtils.sqr(B) - MathUtils.sqr(Bpar))
                            : -Math.sqrt(MathUtils.sqr(B) - MathUtils.sqr(Bpar));

                    h2phArray[i][j] = Bperp / (masterTime.x * Constants.SOL * Math.sin(thetaMaster));
                }
*/
                    // do not include flat earth phase
                    if (onlyTopoRefPhase) {
                        Point masterXYZPos = masterOrbit.lp2xyz(line, pix, masterMeta);
                        Point flatEarthTime = slaveOrbit.xyz2t(masterXYZPos, slaveMeta);
                        if (isBiStaticStack) {
                            ref_phase = slaveMin4piCDivLam * (flatEarthTime.x - slaveTime.x) * 0.5;
                        } else {
                            ref_phase = slaveMin4piCDivLam * (flatEarthTime.x - slaveTime.x);
                        }
                    } else {
                        // include flatearth, ref.pha = phi_topo+phi_flatearth
                        ref_phase = masterMin4piCDivLam * masterMeta.pix2tr(pix) - slaveMin4piCDivLam * slaveTime.x;
                    }

                    demRadarCode_phase[i][j] = ref_phase;

                } else {

                    double[] phi_lam_height = {phi, lambda, 0};
                    Point sarPoint = masterOrbit.ell2lp(phi_lam_height, masterMeta);

                    line = sarPoint.y;
                    pix = sarPoint.x;

                    if (useInvalidIndex) {
                        demRadarCode_y[i][j] = invalidIndex;//line;
                        demRadarCode_x[i][j] = invalidIndex;//pix;
                    } else {
                        demRadarCode_y[i][j] = line;
                        demRadarCode_x[i][j] = pix;
                    }
                    demRadarCode_phase[i][j] = 0;
                }

                lambda += dem.longitudeDelta;
            }
            phi -= dem.latitudeDelta;
        }
    }


    public void calculateScalingRatio() throws Exception {

/*
        // TTODO: simplify this! Tiles are _simpler_ then doris.core buffers!
        final Window masterWin = masterMeta.getCurrentWindow();
        final Window tileWin = (Window) masterWin.clone();

        final int mlL = masterMeta.getMlAz();
        final int mlP = masterMeta.getMlRg();
        final long nLinesMl = masterWin.lines() / mlL; // ifg lines when mlL = 1 (no multilooking)
        final long nPixelsMl = masterWin.pixels() / mlP;

        double ifgLineLo = tileWin.linelo;
        double ifgLineHi = tileWin.pixlo;

        final double veryFirstLine = ifgLineLo + ((double) mlL - 1.0) / 2.0;
        final double veryLastLine = veryFirstLine + (double)((nLinesMl - 1) * mlL);
        final double firstPixel = ifgLineHi + ((double) mlP - 1.0) / 2.0;
        final double lastPixel = firstPixel + (double)((nPixelsMl - 1) * mlP);

        //Determine range-azimuth spacing ratio, needed for proper triangulation
        Point p1 = masterOrbit.lp2xyz(veryFirstLine, firstPixel, masterMeta);
        Point p2 = masterOrbit.lp2xyz(veryFirstLine, lastPixel, masterMeta);
        Point p3 = masterOrbit.lp2xyz(veryLastLine, firstPixel, masterMeta);
        Point p4 = masterOrbit.lp2xyz(veryLastLine, lastPixel, masterMeta);

        final double rangeSpacing = ((p1.min(p2)).norm() + (p3.min(p4)).norm()) / 2
                / (lastPixel - firstPixel);
        final double aziSpacing = ((p1.min(p3)).norm() + (p2.min(p4)).norm()) / 2
                / (veryLastLine - veryFirstLine);
        final double rangeAzRatio = rangeSpacing / aziSpacing;

*/

        //Determine range-azimuth spacing ratio, needed for proper triangulation
        final long firstLine = tileWindow.linelo;
        final long lastLine = tileWindow.linehi;
        final long firstPixel = tileWindow.pixlo;
        final long lastPixel = tileWindow.pixhi;
        Point p1 = masterOrbit.lp2xyz(firstLine, firstPixel, masterMeta);
        Point p2 = masterOrbit.lp2xyz(firstLine, lastPixel, masterMeta);
        Point p3 = masterOrbit.lp2xyz(lastLine, firstPixel, masterMeta);
        Point p4 = masterOrbit.lp2xyz(lastLine, lastPixel, masterMeta);


        final double rangeSpacing = ((p1.min(p2)).norm() + (p3.min(p4)).norm()) / 2
                / (lastPixel - firstPixel);
        final double aziSpacing = ((p1.min(p3)).norm() + (p2.min(p4)).norm()) / 2
                / (lastLine - firstLine);
        rngAzRatio = rangeSpacing / aziSpacing;

        logger.fine("Interferogram azimuth spacing: " + aziSpacing);
        logger.fine("Interferogram range spacing: " + rangeSpacing);
        logger.fine("Range-azimuth spacing ratio: " + rngAzRatio);

    }

    public void gridData(boolean includeDEM, boolean includeLatLon) throws Exception {
        if (rngAzRatio == 0) {
            calculateScalingRatio();
        }
        int mlAz = masterMeta.getMlAz();
        int mlRg = masterMeta.getMlRg();
        int offset = 0;
        demPhase = new double[(int) tileWindow.lines()][(int) tileWindow.pixels()];

        TriangleInterpolator.ZData[] data;
        if(includeDEM || includeLatLon) {
            final int nLines = (int) tileWindow.lines();
            final int nPixels = (int) tileWindow.pixels();
            if (includeDEM) {
                elevation = new double[nLines][nPixels];
                for (double[] row : elevation) {
                    Arrays.fill(row, dem.noDataValue);
                }
            }
            if (includeLatLon) {
                latitude = new double[nLines][nPixels];
                longitude = new double[nLines][nPixels];
                for (double[] row : latitude) {
                    Arrays.fill(row, Double.NaN);
                }
                for (double[] row : longitude) {
                    Arrays.fill(row, Double.NaN);
                }
            }

            if (includeDEM && includeLatLon) {
                // This should never happen as elevation requires masking of sea pixels and
                // lat/lon do not; but leave it here anyways. See comments in SubtRefDemOp.computeTopoPhase()
                // that calls gridData().
                data = new TriangleInterpolator.ZData[]{
                        new TriangleInterpolator.ZData(demRadarCode_phase, demPhase),
                        new TriangleInterpolator.ZData(demElevation, elevation),
                        new TriangleInterpolator.ZData(demLatitude, latitude),
                        new TriangleInterpolator.ZData(demLongitude, longitude)
                };
            } else if (includeDEM) {
                data = new TriangleInterpolator.ZData[]{
                        new TriangleInterpolator.ZData(demRadarCode_phase, demPhase),
                        new TriangleInterpolator.ZData(demElevation, elevation),
                };
            }  else { // includeLatLon must be true
                data = new TriangleInterpolator.ZData[]{
                        new TriangleInterpolator.ZData(demRadarCode_phase, demPhase),
                        new TriangleInterpolator.ZData(demLatitude, latitude),
                        new TriangleInterpolator.ZData(demLongitude, longitude)
                };
            }
        } else {
            data = new TriangleInterpolator.ZData[] {
                    new TriangleInterpolator.ZData(demRadarCode_phase, demPhase)
            };
        }

        TriangleInterpolator.gridDataLinear(demRadarCode_y, demRadarCode_x, data,
                tileWindow, rngAzRatio, mlAz, mlRg, invalidIndex, offset);
    }

    public static DemTile getDEMTile(final org.jlinda.core.Window tileWindow,
                                     final Map<String, ProductContainer> targetMap,
                                     final ElevationModel dem,
                                     final double demNoDataValue,
                                     final double demSamplingLat,
                                     final double demSamplingLon,
                                     final String tileExtensionPercent) {

        ProductContainer mstContainer = targetMap.values().iterator().next();

        return getDEMTile(tileWindow, mstContainer.sourceMaster.metaData, mstContainer.sourceMaster.orbit,
                dem, demNoDataValue, demSamplingLat, demSamplingLon, tileExtensionPercent);
    }

    public static DemTile getDEMTile(final org.jlinda.core.Window tileWindow,
                                     final SLCImage metaData,
                                     final Orbit orbit,
                                     final ElevationModel dem,
                                     final double demNoDataValue,
                                     final double demSamplingLat,
                                     final double demSamplingLon,
                                     final String tileExtensionPercent) {

        try {
            // compute tile geo-corners ~ work on ellipsoid
            GeoPoint[] geoCorners = org.jlinda.core.utils.GeoUtils.computeCorners(metaData, orbit, tileWindow);

            // get corners as DEM indices
            PixelPos[] pixelCorners = new PixelPos[2];
            pixelCorners[0] = dem.getIndex(new GeoPos(geoCorners[0].lat, geoCorners[0].lon));
            pixelCorners[1] = dem.getIndex(new GeoPos(geoCorners[1].lat, geoCorners[1].lon));

            final int x0DEM = (int)Math.round(pixelCorners[0].x);
            final int y0DEM = (int)Math.round(pixelCorners[0].y);
            final int x1DEM = (int)Math.round(pixelCorners[1].x);
            final int y1DEM = (int)Math.round(pixelCorners[1].y);
            final Rectangle demTileRect = new Rectangle(x0DEM, y0DEM, x1DEM - x0DEM + 1, y1DEM - y0DEM + 1);

            // get max/min height of tile ~ uses 'fast' GCP based interpolation technique
            final double[] tileHeights = computeMaxHeight(
                    pixelCorners, demTileRect, tileExtensionPercent, dem, demNoDataValue);

            // compute extra lat/lon for dem tile
            GeoPoint geoExtent = org.jlinda.core.utils.GeoUtils.defineExtraPhiLam(tileHeights[0], tileHeights[1],
                    tileWindow, metaData, orbit);

            // extend corners
            geoCorners = org.jlinda.core.utils.GeoUtils.extendCorners(geoExtent, geoCorners);

            // update corners
            pixelCorners[0] = dem.getIndex(new GeoPos(geoCorners[0].lat, geoCorners[0].lon));
            pixelCorners[1] = dem.getIndex(new GeoPos(geoCorners[1].lat, geoCorners[1].lon));

            pixelCorners[0] = new PixelPos(Math.floor(pixelCorners[0].x), Math.floor(pixelCorners[0].y));
            pixelCorners[1] = new PixelPos(Math.ceil(pixelCorners[1].x), Math.ceil(pixelCorners[1].y));

            GeoPos upperLeftGeo = dem.getGeoPos(pixelCorners[0]);

            int nLatPixels = (int) Math.abs(pixelCorners[1].y - pixelCorners[0].y);
            int nLonPixels = (int) Math.abs(pixelCorners[1].x - pixelCorners[0].x);

            if(!upperLeftGeo.isValid()) {
                return null;
            }

            DemTile demTile = new DemTile(upperLeftGeo.lat * org.jlinda.core.Constants.DTOR,
                    upperLeftGeo.lon * org.jlinda.core.Constants.DTOR,
                    nLatPixels, nLonPixels, Math.abs(demSamplingLat),
                    Math.abs(demSamplingLon), (long)demNoDataValue);

            int startX = (int) pixelCorners[0].x;
            int endX = startX + nLonPixels;
            int startY = (int) pixelCorners[0].y;
            int endY = startY + nLatPixels;

            double[][] elevation = new double[nLatPixels][nLonPixels];
            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = startX, j = 0; x < endX; x++, j++) {
                    try {
                        double elev = dem.getSample(x, y);
                        if (Double.isNaN(elev)) {
                            elev = demNoDataValue;
                        }
                        elevation[i][j] = elev;
                    } catch (Exception e) {
                        elevation[i][j] = demNoDataValue;
                    }
                }
            }

            demTile.setData(elevation);

            return demTile;
        } catch (Exception e) {
            return null;
        }
    }

    public static TopoPhase computeTopoPhase(
            final ProductContainer product, final Window tileWindow, final DemTile demTile, final boolean outputDEM, final boolean outputLatLon) {

        final SLCImage mstMetaData = product.sourceMaster.metaData;
        final Orbit mstOrbit = product.sourceMaster.orbit;
        final SLCImage slvMetaData = product.sourceSlave.metaData;
        final Orbit slvOrbit = product.sourceSlave.orbit;

        return computeTopoPhase(mstMetaData, mstOrbit, slvMetaData, slvOrbit, tileWindow, demTile, outputDEM, outputLatLon);
    }

    public static TopoPhase computeTopoPhase(
            final ProductContainer product, final Window tileWindow, final DemTile demTile, final boolean outputDEM) {

        final SLCImage mstMetaData = product.sourceMaster.metaData;
        final Orbit mstOrbit = product.sourceMaster.orbit;
        final SLCImage slvMetaData = product.sourceSlave.metaData;
        final Orbit slvOrbit = product.sourceSlave.orbit;

        return computeTopoPhase(mstMetaData, mstOrbit, slvMetaData, slvOrbit, tileWindow, demTile, outputDEM, false);
    }

    public static TopoPhase computeTopoPhase(
            final SLCImage mstMetaData, final Orbit mstOrbit, final SLCImage slvMetaData, final Orbit slvOrbit,
            final Window tileWindow, final DemTile demTile, final boolean outputDEM) {
        return computeTopoPhase(mstMetaData, mstOrbit, slvMetaData, slvOrbit, tileWindow, demTile, outputDEM, false);
    }

    public static TopoPhase computeTopoPhase(
            final SLCImage mstMetaData, final Orbit mstOrbit, final SLCImage slvMetaData, final Orbit slvOrbit,
            final Window tileWindow, final DemTile demTile, final boolean outputDEM, final boolean outputLatLon) {
        // computeTopoPhase() is called separately for outputting lat/lon and outputting elevation because elevation
        // requires sea pixels to be masked out and lat/lon do not; so outputDEM and outputLatLon cannot be true
        // at the same time.
        try {
            final TopoPhase topoPhase = new TopoPhase(mstMetaData, mstOrbit, slvMetaData, slvOrbit, tileWindow, demTile);

            // We do not want to use ivalidIndex if it is outputting lat/lon because we do not want to mask out the sea
            // pixels like we do with elevation.
            topoPhase.radarCode(!outputLatLon);

            topoPhase.gridData(outputDEM, outputLatLon);

            return topoPhase;

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private static double[] computeMaxHeight(
            final PixelPos[] corners, final Rectangle rectangle, final String tileExtensionPercent,
            final ElevationModel dem, final double demNoDataValue) throws Exception {

        /* Notes:
          - The scaling and extensions of extreme values of DEM tiles has to be performed to guarantee the overlap
            between SAR and DEM tiles, and avoid blanks in the simulated Topo phase.

          - More conservative, while also more reliable parameters are introduced that guarantee good results even
            in some extreme cases.

          - Parameters are defined for the reliability, not(!) the performance.
         */

        int tileExtPercent = Integer.parseInt(tileExtensionPercent);
        final float extraTileX = (float) (1 + tileExtPercent / 100.0); // = 1.5f
        final float extraTileY = (float) (1 + tileExtPercent / 100.0); // = 1.5f
        final float scaleMaxHeight = (float) (1 + tileExtPercent/ 100.0); // = 1.25f

        double[] heightArray = new double[2];

        // double square root : scales with the size of tile
        final int numberOfPoints = (int) (10 * Math.sqrt(Math.sqrt(rectangle.width * rectangle.height)));

        // extend tiles for which statistics is computed
        final int offsetX = (int) (extraTileX * rectangle.width);
        final int offsetY = (int) (extraTileY * rectangle.height);

        // define window
        final Window window = new Window((long)(corners[0].y - offsetY),
                (long)(corners[1].y + offsetY),
                (long)(corners[0].x - offsetX),
                (long)(corners[1].x + offsetX));

        // distribute points
        final int[][] points = MathUtils.distributePoints(numberOfPoints, window);
        final ArrayList<Double> heights = new ArrayList();

        // then for number of extra points
        for (int[] point : points) {
            try {
                Double height = dem.getSample(point[1], point[0]);
                if (!Double.isNaN(height) && !height.equals(demNoDataValue)) {
                    heights.add(height);
                }
            } catch (Exception e) {
                // don't add height
            }
        }

        // get max/min and add extras ~ just to be sure
        if (heights.size() > 2) {
            // set minimum to 'zero', eg, what if there's small lake in tile?
            // heightArray[0] = Collections.min(heights);
            heightArray[0] = Collections.min(heights);
            heightArray[1] = Collections.max(heights) * scaleMaxHeight;
        } else { // if nodatavalues return 0s ~ tile in the sea
            heightArray[0] = 0;
            heightArray[1] = 0;
        }

        return heightArray;
    }
}
