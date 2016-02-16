package org.esa.s1tbx.insar.gpf.coregistration;

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.OperatorException;
import org.jlinda.core.coregistration.PolynomialModel;

import javax.media.jai.WarpPolynomial;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 15/02/2016.
 */
public class WarpData implements PolynomialModel {
    public final List<Placemark> slaveGCPList = new ArrayList<>();
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

    public boolean isValid() {
        return !notEnoughGCPs;
    }

    public WarpPolynomial getJAIWarp() {
        return jaiWarp;
    }

    public int getNumObservations() {
        return numValidGCPs;
    }

    public double getRMS(int index) {
        return rms[index];
    }

    public double getXMasterCoord(int index) {
        return masterGCPCoords[2 * index];
    }

    public double getYMasterCoord(int index) {
        return masterGCPCoords[2 * index + 1];
    }

    public double getXSlaveCoord(int index) {
        return slaveGCPCoords[2 * index];
    }

    public double getYSlaveCoord(int index) {
        return slaveGCPCoords[2 * index + 1];
    }

    public List<Placemark> getSlaveGCPList() {
        return slaveGCPList;
    }

    /**
     * Compute WARP function using master and slave GCPs.
     *
     * @param warpPolynomialOrder The WARP polynimal order.
     */
    public void computeWARP(final int warpPolynomialOrder) {

        // check if master and slave GCP coordinates are identical, if yes set the warp polynomial coefficients
        // directly, no need to compute them using JAI function because JAI produces incorrect result due to ill
        // conditioned matrix.
        float sum = 0.0f;
        for (int i = 0; i < slaveGCPCoords.length; i++) {
            sum += Math.abs(slaveGCPCoords[i] - masterGCPCoords[i]);
        }
        if (sum < 0.01) {
            if (warpPolynomialOrder < 1) {
                throw new OperatorException("Incorrect WARP degree");
            }
            // coefLen = 3, 6, 10, 15, ...
            final int coefLen = (warpPolynomialOrder + 1) * (warpPolynomialOrder + 2) / 2;
            xCoef = new double[coefLen];
            yCoef = new double[coefLen];
            for (int i = 0; i < coefLen; i++) {
                xCoef[i] = 0;
                yCoef[i] = 0;
            }
            xCoef[1] = 1;
            yCoef[2] = 1;

            jaiWarp = null;

            return;
        }

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
        for (int i = 0; i < size; ++i) {
            xCoef[i] = jaiXCoefs[i];
            yCoef[i] = jaiYCoefs[i];
        }
    }
}
