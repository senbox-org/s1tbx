package org.jlinda.core.geom;

import org.jlinda.core.*;

public class DemTile {

    //// logger
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DemTile.class.getName());

    //// topoPhase global params
    double lat0;
    double lon0;
    long nLatPixels;
    long nLonPixels;

    double lat0_ABS;
    double lon0_ABS;
    long nLatPixels_ABS;
    long nLonPixels_ABS;

    double latitudeDelta;
    double longitudeDelta;

    // no data value
    double noDataValue;

    //// actual demTileData
    double[][] data;

    /// extent : coordinates in radians
    double lambdaExtra;
    double phiExtra;

    /// tile coordinates in phi,lam
    boolean cornersComputed = false;

    public double phiMin;
    public double phiMax;
    public double lambdaMin;
    public double lambdaMax;

    /// tile index
    int indexPhi0DEM;
    int indexPhiNDEM;
    int indexLambda0DEM;
    int indexLambdaNDEM;

    //// tile stats
    boolean statsComputed = false;
    long totalNumPoints;
    long numNoData;
    long numValid;
    double meanValue;
    double minValue;
    double maxValue;


    public DemTile() {
    }

    public DemTile(double lat0, double lon0, long nLatPixels, long nLonPixels,
                   double latitudeDelta, double longitudeDelta, long noDataValue) {
        this.lat0 = lat0;
        this.lon0 = lon0;
        this.nLatPixels = nLatPixels;
        this.nLonPixels = nLonPixels;
        this.latitudeDelta = latitudeDelta;
        this.longitudeDelta = longitudeDelta;
        this.noDataValue = noDataValue;
    }

    public DemTile(double lat0_ABS, double lon0_ABS, long nLatPixels_ABS, long nLonPixels_ABS,
                   double delta, long noDataValue) {

        this.lat0_ABS = lat0_ABS;
        this.lon0_ABS = lon0_ABS;
        this.nLatPixels_ABS = nLatPixels_ABS;
        this.nLonPixels_ABS = nLonPixels_ABS;
        this.latitudeDelta = delta;
        this.longitudeDelta = delta;
        this.noDataValue = noDataValue;
    }

    public void setLatitudeDelta(double latitudeDelta) {
        this.latitudeDelta = latitudeDelta;
    }

    public void setLongitudeDelta(double longitudeDelta) {
        this.longitudeDelta = longitudeDelta;
    }

    public void setNoDataValue(double noDataValue) {
        this.noDataValue = noDataValue;
    }

    public double[][] getData() {
        return data;
    }

    public void setData(double[][] data) {
        this.data = data;
    }

    // ----- Loop over DEM for stats ------------------------
    public void stats() throws Exception {

        // inital values
//        double min_dem_buffer = 100000.0;
//        double max_dem_buffer = -100000.0;
        double min_dem_buffer = data[0][0];
        double max_dem_buffer = data[0][0];

        if (max_dem_buffer == noDataValue) {
            max_dem_buffer = 0;
        }
        if (min_dem_buffer == noDataValue) {
            min_dem_buffer = 0;
        }

        try {
            totalNumPoints = data.length * data[0].length;
            for (double[] aData : data) {
                for (int j = 0; j < data[0].length; j++) {
                    if (aData[j] != noDataValue) {
                        numValid++;
                        meanValue += aData[j];           // divide by numValid later
                        if (aData[j] < min_dem_buffer)
                            min_dem_buffer = aData[j];
                        if (aData[j] > max_dem_buffer)
                            max_dem_buffer = aData[j];
                    } else {
                        numNoData++;
//                        System.out.println("dataValue = " + aData[j]);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Something went wrong when computing DEM tile stats");
            logger.error("Is DEM tile declared?");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        //global stats
        minValue = min_dem_buffer;
        maxValue = max_dem_buffer;
        meanValue /= numValid;

        statsComputed = true;
        showStats();

    }

    private void showStats() {

        if (statsComputed) {
            System.out.println("DEM Tile Stats");
            System.out.println("------------------------------------------------");
            System.out.println("Total number of points: " + totalNumPoints);
            System.out.println("Number of valid points: " + numValid);
            System.out.println("Number of NODATA points: " + numNoData);
            System.out.println("Max height in meters at valid points: " + maxValue);
            System.out.println("Min height in meters at valid points: " + minValue);
            System.out.println("Mean height in meters at valid points: " + meanValue);
        } else {
            System.out.println("DEM Tile Stats");
            System.out.println("------------------------------------------------");
            System.out.println("DemTile.stats() method not invoked!");
        }

    }


    @Deprecated
    public void computeGeoCorners(final SLCImage meta, final Orbit orbit, final Window tile) throws Exception {

        double[] phiAndLambda;

        final double l0 = tile.linelo;
        final double lN = tile.linehi;
        final double p0 = tile.pixlo;
        final double pN = tile.pixhi;

        // compute Phi, Lambda for Tile corners
        phiAndLambda = orbit.lp2ell(new Point(p0, l0), meta);
        final double phi_l0p0 = phiAndLambda[0];
        final double lambda_l0p0 = phiAndLambda[1];

        phiAndLambda = orbit.lp2ell(new Point(p0, lN), meta);
        final double phi_lNp0 = phiAndLambda[0];
        final double lambda_lNp0 = phiAndLambda[1];

        phiAndLambda = orbit.lp2ell(new Point(pN, lN), meta);
        final double phi_lNpN = phiAndLambda[0];
        final double lambda_lNpN = phiAndLambda[1];

        phiAndLambda = orbit.lp2ell(new Point(pN, l0), meta);
        final double phi_l0pN = phiAndLambda[0];
        final double lambda_l0pN = phiAndLambda[1];

        //// Select DEM values based on rectangle outside l,p border ////
        // phi
        phiMin = Math.min(Math.min(Math.min(phi_l0p0, phi_lNp0), phi_lNpN), phi_l0pN);
        phiMax = Math.max(Math.max(Math.max(phi_l0p0, phi_lNp0), phi_lNpN), phi_l0pN);
        // lambda
        lambdaMin = Math.min(Math.min(Math.min(lambda_l0p0, lambda_lNp0), lambda_lNpN), lambda_l0pN);
        lambdaMax = Math.max(Math.max(Math.max(lambda_l0p0, lambda_lNp0), lambda_lNpN), lambda_l0pN);

        // a little bit extra at edges to be sure

        // redefine it: no checks whether there are previous declarations
        defineExtraPhiLam();

        // phi
        phiMin -= phiExtra;
        phiMax += phiExtra;
        // lambda
        lambdaMax += lambdaExtra;
        lambdaMin -= lambdaExtra;

//        computeIndexCornersNest();
//        computeDemTileSize();

        cornersComputed = true;
    }

    @Deprecated
    private void computeDemTileSize() {
        nLatPixels = indexLambdaNDEM - indexLambda0DEM;
        nLonPixels = indexPhiNDEM - indexPhi0DEM;
    }

    @Deprecated
    private void computeIndexCorners() {

        indexPhi0DEM = (int) (Math.floor((lat0 - phiMax) / latitudeDelta));
        indexPhiNDEM = (int) (Math.ceil((lat0 - phiMin) / latitudeDelta));
        indexLambda0DEM = (int) (Math.floor((lambdaMin - lon0) / longitudeDelta));
        indexLambdaNDEM = (int) (Math.ceil((lambdaMax - lon0) / longitudeDelta));

        //// sanity checks ////
        if (indexPhi0DEM < 0) {
            logger.warn("indexPhi0DEM: " + indexPhi0DEM);
            indexPhi0DEM = 0;   // reset to default start at first
            logger.warn("DEM does not cover entire interferogram/tile.");
            logger.warn("input DEM should be extended to the North.");
        }

        if (indexPhiNDEM > nLatPixels - 1) {
            logger.warn("indexPhiNDEM: " + indexPhi0DEM);
            indexPhiNDEM = (int) (nLatPixels - 1);
            logger.warn("DEM does not cover entire interferogram/tile.");
            logger.warn("input DEM should be extended to the South.");
        }

        if (indexLambda0DEM < 0) {
            logger.warn("indexLambda0DEM: " + indexLambda0DEM);
            indexLambda0DEM = 0;    // default start at first
            logger.warn("DEM does not cover entire interferogram/tile.");
            logger.warn("input DEM should be extended to the West.");
        }

        if (indexLambdaNDEM > nLonPixels - 1) {
            logger.warn("indexLambdaNDEM: " + indexLambdaNDEM);
            indexLambdaNDEM = (int) (nLonPixels - 1);
            logger.warn("DEM does not cover entire interferogram/tile.");
            logger.warn("input DEM should be extended to the East.");
        }

    }

    @Deprecated
    private void computeIndexCornersNest() {
        indexPhi0DEM = (int) (Math.floor(nLatPixels_ABS - (lat0_ABS + phiMax) / latitudeDelta));
        indexPhiNDEM = (int) (Math.ceil(nLatPixels_ABS - (lat0_ABS + phiMin) / latitudeDelta));
        indexLambda0DEM = (int) (Math.floor((lambdaMin + lon0_ABS) / longitudeDelta));
        indexLambdaNDEM = (int) (Math.ceil((lambdaMax - lon0_ABS) / longitudeDelta));
    }

    // get corners of tile (approx) to select DEM
    //	in radians (if height were zero)
    @Deprecated
    private void defineExtraPhiLam() {
        // TODO: introduce methods for dynamic scaling of extra lambda/phi depending on average tile Height!
//        lambdaExtra = (1.5 * latitudeDelta + (4.0 / 25.0) * Constants.DTOR); // for himalayas!
//        phiExtra = (1.5 * longitudeDelta + (4.0 / 25.0) * Constants.DTOR);
//        lambdaExtra = (1.5 * latitudeDelta + (0.75 / 25.0) * Constants.DTOR); // for Etna
//        phiExtra = (1.5 * longitudeDelta + (0.05 / 25.0) * Constants.DTOR);
        lambdaExtra = (1.5 * latitudeDelta + (0.75 / 25.0) * Constants.DTOR); // for Etna
        phiExtra = (1.5 * longitudeDelta + (0.1 / 25.0) * Constants.DTOR);
    }


}
