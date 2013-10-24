package org.jlinda.core.geom;

import org.apache.log4j.Logger;
import org.jlinda.core.*;
import org.jlinda.core.utils.TriangleUtils;

public class ThetaTile {

    //// logger
    static Logger logger = Logger.getLogger(ThetaTile.class.getName());

    private Orbit masterOrbit;   // master
    private SLCImage masterMeta; // master

    private Window tileWindow;    // buffer/tile coordinates

    private DemTile dem;           // demTileData

    public double[][] thetaArray;

    private double[][] demRadarCode_x;
    private double[][] demRadarCode_y;
    private double[][] demRadarCode_theta; // interpolated grid

    private int nRows;
    private int nCols;

    private double rngAzRatio = 0;

    public ThetaTile(SLCImage masterMeta, Orbit masterOrbit, Window window, DemTile demTile) throws Exception {
        this.masterOrbit = masterOrbit;
        this.masterMeta = masterMeta;
        this.tileWindow = window;
        this.dem = demTile;

        nRows = dem.data.length;
        nCols = dem.data[0].length;
    }

    public void setMasterOrbit(Orbit masterOrbit) {
        this.masterOrbit = masterOrbit;
    }

    public void setMasterMeta(SLCImage masterMeta) {
        this.masterMeta = masterMeta;
    }

    public void setWindow(Window window) {
        this.tileWindow = window;
    }

    public double getRngAzRatio() {
        return rngAzRatio;
    }

    public void setRngAzRatio(double rngAzRatio) {
        this.rngAzRatio = rngAzRatio;
    }

    public double[][] getDemRadarCode_x() {
        return demRadarCode_x;
    }

    public void setDemRadarCode_x(double[][] demRadarCode_x) {
        this.demRadarCode_x = demRadarCode_x;
    }

    public double[][] getDemRadarCode_y() {
        return demRadarCode_y;
    }

    public void setDemRadarCode_y(double[][] demRadarCode_y) {
        this.demRadarCode_y = demRadarCode_y;
    }

    public double[][] getDemRadarCode_theta() {
        return demRadarCode_theta;
    }

    public void setDemRadarCode_theta(double[][] demRadarCode_theta) {
        this.demRadarCode_theta = demRadarCode_theta;
    }

    public Window getTileWindow() {
        return tileWindow;
    }

    public void setTileWindow(Window tileWindow) {
        this.tileWindow = tileWindow;
    }

    public double[][] getThetaArray() {
        return thetaArray;
    }

    public void setThetaArray(double[][] thetaArray) {
        this.thetaArray = thetaArray;
    }


    public synchronized void radarCode() throws Exception {

        logger.trace("Converting DEM to radar system for this tile.");

        demRadarCode_x = new double[nRows][nCols];
        demRadarCode_y = new double[nRows][nCols];
        demRadarCode_theta = new double[nRows][nCols];

        final int nPoints = nRows * nCols;
//        final boolean onlyTopoRefPhase = true;

        logger.info("Number of points in DEM: " + nPoints);

        double phi, lambda, height, line, pix, theta;

        final double upperLeftPhi = dem.lat0 - dem.indexPhi0DEM * dem.latitudeDelta;
        final double upperLeftLambda = dem.lon0 + dem.indexLambda0DEM * dem.longitudeDelta;

        Point pointOnDem;

        phi = upperLeftPhi;
        for (int i = 0; i < nRows; i++) {

            lambda = upperLeftLambda;
            double[] heightArray = dem.data[i];

            for (int j = 0; j < nCols; j++) {

                height = heightArray[j];

                if (height != dem.noDataValue) {

                    // starts at the upper left corner
                    double[] phi_lam_height = {phi, lambda, height};
                    Point sarPoint = masterOrbit.ell2lp(phi_lam_height, masterMeta);

                    line = sarPoint.y;
                    pix = sarPoint.x;

                    demRadarCode_y[i][j] = line;
                    demRadarCode_x[i][j] = pix;

                    pointOnDem = Ellipsoid.ell2xyz(phi_lam_height);

                    Point sarPointTime = masterOrbit.xyz2t(pointOnDem, masterMeta);
                    Point satellitePosition = masterOrbit.getXYZ(sarPointTime.y);
                    Point rangeDistance = satellitePosition.min(pointOnDem);

                    demRadarCode_theta[i][j] = pointOnDem.angle(rangeDistance);;

                }

                lambda += dem.longitudeDelta;
            }
            phi -= dem.latitudeDelta;
        }
    }


    public void calculateScalingRatio() throws Exception {

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

        logger.debug("Interferogram azimuth spacing: " + aziSpacing);
        logger.debug("Interferogram range spacing: " + rangeSpacing);
        logger.debug("Range-azimuth spacing ratio: " + rngAzRatio);

    }

    public void gridData() throws Exception {
        if (rngAzRatio == 0) {
            calculateScalingRatio();
        }
        int mlAz = masterMeta.getMlAz();
        int mlRg = masterMeta.getMlRg();
        int offset = 0;
        thetaArray = TriangleUtils.gridDataLinear(demRadarCode_y, demRadarCode_x, demRadarCode_theta,
                tileWindow, rngAzRatio, mlAz, mlRg, dem.noDataValue, offset);
    }

}
