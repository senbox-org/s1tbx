package org.jlinda.core.geom;

import org.jlinda.core.Constants;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.TriangleUtils;

/**
 * User: pmar@ppolabs.com
 * Date: 5/15/13
 * Time: 5:00 PM
 */
public class SimAmpTile {

    private Orbit masterOrbit;   // master
    private SLCImage masterMeta; // master

    private Window tileWindow;    // buffer/tile coordinates

    private double[][] thetaArray;
    private double[][] demArray;

    private int nRows;
    private int nCols;

    private double slantRange;

    public double[][] getSimAmpArray() {
        return simAmpArray;
    }

    public void setSimAmpArray(double[][] simAmpArray) {
        this.simAmpArray = simAmpArray;
    }

    public double[][] simAmpArray;

    public SimAmpTile(SLCImage masterMeta, Orbit masterOrbit, Window window, ThetaTile thetaTile, DemTile demTile) throws Exception {

        this.masterOrbit = masterOrbit;
        this.masterMeta = masterMeta;
        this.tileWindow = window;
        this.thetaArray = thetaTile.getThetaArray();

        final int offset = 0;
        demArray = TriangleUtils.gridDataLinear(thetaTile.getDemRadarCode_y(), thetaTile.getDemRadarCode_x(),
                demTile.getData(),
                thetaTile.getTileWindow(), thetaTile.getRngAzRatio(),
                masterMeta.getMlAz(), masterMeta.getMlRg(), demTile.noDataValue, offset);

        slantRange = Constants.SOL / (2 * masterMeta.getRsr2x());

        nRows = thetaArray.length;
        nCols = thetaArray[0].length;

        simAmpArray = new double[nRows][nCols];

    }

    public void simulateAmplitude() throws Exception {

        for (int i = 0 ; i < nRows ; i ++) {
            for (int j = 0; j < nCols; j++) {

                if (j < nCols - 1) {
                    double theta = thetaArray[i][j];           // incidence angle
                    double ground_range = slantRange / Math.sin(theta);
                    double grad = demArray[i][j + 1] - demArray[i][j];   // height gradient
                    double alpha = Math.atan(grad / ground_range);                    // slope
                    double localIncAngle = theta - alpha;

                    /* Testing */
                    // simAmpArray[i][j] = (double) alpha;
                    // simAmpArray[i][j] = (double) localIncAngle;
                    // simAmpArray[i][j] = (double) cos(localIncAngle)/sin(localIncAngle); // see eineder2003
                    // simAmpArray[i][j] = (double) sin(-localIncAngle);   // used
                    // simAmpArray[i][j] = (double) sin(-localIncAngle)+1; // used, +1 shift to positive range of values.
                    // simAmpArray[i][j] = (double) pow(sim_amp(i,j),2);   // intensity to be tested

                    if (demArray[i][j] == 0) {
                        simAmpArray[i][j] = 0;
                    } else {
                        simAmpArray[i][j] = Math.sin(-localIncAngle) + 1;   // used, +1 shift to positive range of values.
                        // intensity
                        //simAmpArray[i][j] = Math.pow((Math.sin(-localIncAngle)+1),2);
                    }

                } else {

                    // cp previous coln as last coln
                    simAmpArray[i][j] = simAmpArray[i][j - 1];

                }

            }
        }
    }


}
