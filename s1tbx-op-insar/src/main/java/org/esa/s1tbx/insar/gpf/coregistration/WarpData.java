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

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.jlinda.core.coregistration.PolynomialModel;

import javax.media.jai.WarpPolynomial;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 15/02/2016.
 */
public class WarpData implements PolynomialModel {
    public final List<Placemark> slaveGCPList = new ArrayList<>();
    private WarpPolynomial jaiWarp = null;
    private double[] xCoef = null;
    private double[] yCoef = null;

    private int numValidGCPs = 0;
    private boolean notEnoughGCPs = false;
    private float[] rms = null;
    private float[] rowResiduals = null;
    private float[] colResiduals = null;
    private float[] masterGCPCoords = null;
    private float[] slaveGCPCoords = null;

    private double rmsStd = 0;
    private double rmsMean = 0;
    private double rowResidualStd = 0;
    private double rowResidualMean = 0;
    private double colResidualStd = 0;
    private double colResidualMean = 0;

    public WarpData(ProductNodeGroup<Placemark> slaveGCPGroup) {
        for (int i = 0; i < slaveGCPGroup.getNodeCount(); ++i) {
            slaveGCPList.add(slaveGCPGroup.get(i));
        }
    }

    public double getRMSStd() {
        return rmsStd;
    }

    public double getRMSMean() {
        return rmsMean;
    }

    public double getRowResidualStd() {
        return rowResidualStd;
    }

    public double getRowResidualMean() {
        return rowResidualMean;
    }

    public double getColResidualStd() {
        return colResidualStd;
    }

    public double getColResidualMean() {
        return colResidualMean;
    }

    public boolean isValid() {
        return !notEnoughGCPs;
    }

    public void setInValid() {
        notEnoughGCPs = true;
    }

    public int getNumValidGCPs() {
        return numValidGCPs;
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

    public void computeWARPPolynomialFromGCPs(
            final Product sourceProduct, final Band srcBand, final int warpPolynomialOrder,
            final ProductNodeGroup<Placemark> masterGCPGroup, final int maxIterations, final float rmsThreshold,
            final boolean appendFlag) {

        boolean append;
        float threshold = 0.0f;
        for (int iter = 0; iter < maxIterations; iter++) {

            if (iter == 0) {
                append = appendFlag;
            } else {
                append = true;

                if (iter < maxIterations - 1 && rmsMean > rmsThreshold) {
                    threshold = (float) (rmsMean + rmsStd);
                } else {
                    threshold = rmsThreshold;
                }
            }

            if (threshold > 0.0f) {
                eliminateGCPsBasedOnRMS(threshold);
            }

            computeWARPPolynomial(warpPolynomialOrder, masterGCPGroup);

            outputCoRegistrationInfo(
                    sourceProduct, warpPolynomialOrder, this, append, threshold, iter, srcBand.getName());

            if (notEnoughGCPs || iter > 0 && threshold <= rmsThreshold) {
                break;
            }
        }
    }

    /**
     * Compute WARP polynomial function using master and slave GCP pairs.
     *
     * @param warpPolynomialOrder The WARP polynimal order.
     * @param masterGCPGroup      The master GCPs.
     */
    private void computeWARPPolynomial(final int warpPolynomialOrder, final ProductNodeGroup<Placemark> masterGCPGroup) {

        getNumOfValidGCPs(warpPolynomialOrder);

        getMasterAndSlaveGCPCoordinates(masterGCPGroup);
        if (notEnoughGCPs) return;

        computeWARP(warpPolynomialOrder);

        computeRMS(warpPolynomialOrder);
    }

    /**
     * Get the number of valid GCPs.
     *
     * @param warpPolynomialOrder The WARP polynimal order.
     * @throws OperatorException The exceptions.
     */
    private void getNumOfValidGCPs(final int warpPolynomialOrder) throws OperatorException {

        numValidGCPs = slaveGCPList.size();
        final int requiredGCPs = (warpPolynomialOrder + 2) * (warpPolynomialOrder + 1) / 2;
        if (numValidGCPs < requiredGCPs) {
            notEnoughGCPs = true;
            //throw new OperatorException("Order " + warpPolynomialOrder + " requires " + requiredGCPs +
            //        " GCPs, valid GCPs are " + numValidGCPs + ", try a larger RMS threshold.");
        }
    }

    /**
     * Get GCP coordinates for master and slave bands.
     *
     * @param masterGCPGroup The master GCPs.
     */
    private void getMasterAndSlaveGCPCoordinates(final ProductNodeGroup<Placemark> masterGCPGroup) {

        masterGCPCoords = new float[2 * numValidGCPs];
        slaveGCPCoords = new float[2 * numValidGCPs];

        for (int i = 0; i < numValidGCPs; ++i) {

            final Placemark sPin = slaveGCPList.get(i);
            final PixelPos sGCPPos = sPin.getPixelPos();
            //System.out.println("WARP: slave gcp[" + i + "] = " + "(" + sGCPPos.x + "," + sGCPPos.y + ")");

            final Placemark mPin = masterGCPGroup.get(sPin.getName());
            final PixelPos mGCPPos = mPin.getPixelPos();
            //System.out.println("WARP: master gcp[" + i + "] = " + "(" + mGCPPos.x + "," + mGCPPos.y + ")");

            final int j = 2 * i;
            masterGCPCoords[j] = (float) mGCPPos.x;
            masterGCPCoords[j + 1] = (float) mGCPPos.y;
            slaveGCPCoords[j] = (float) sGCPPos.x;
            slaveGCPCoords[j + 1] = (float) sGCPPos.y;
        }
    }

    /**
     * Compute root mean square error of the warped GCPs for given WARP function and given GCPs.
     *
     * @param warpPolynomialOrder The WARP polynimal order.
     */
    private void computeRMS(final int warpPolynomialOrder) {

        // compute RMS for all valid GCPs
        rms = new float[numValidGCPs];
        colResiduals = new float[numValidGCPs];
        rowResiduals = new float[numValidGCPs];
        final PixelPos slavePos = new PixelPos(0.0f, 0.0f);
        for (int i = 0; i < rms.length; i++) {
            final int i2 = 2 * i;
            getWarpedCoords(warpPolynomialOrder,
                            masterGCPCoords[i2],
                            masterGCPCoords[i2 + 1],
                            slavePos);
            final double dX = slavePos.x - slaveGCPCoords[i2];
            final double dY = slavePos.y - slaveGCPCoords[i2 + 1];
            colResiduals[i] = (float) dX;
            rowResiduals[i] = (float) dY;
            rms[i] = (float) Math.sqrt(dX * dX + dY * dY);
        }

        // compute some statistics
        rmsMean = 0.0;
        rowResidualMean = 0.0;
        colResidualMean = 0.0;
        double rms2Mean = 0.0;
        double rowResidual2Mean = 0.0;
        double colResidual2Mean = 0.0;

        for (int i = 0; i < rms.length; i++) {
            rmsMean += rms[i];
            rms2Mean += rms[i] * rms[i];
            rowResidualMean += rowResiduals[i];
            rowResidual2Mean += rowResiduals[i] * rowResiduals[i];
            colResidualMean += colResiduals[i];
            colResidual2Mean += colResiduals[i] * colResiduals[i];
        }
        rmsMean /= rms.length;
        rms2Mean /= rms.length;
        rowResidualMean /= rms.length;
        rowResidual2Mean /= rms.length;
        colResidualMean /= rms.length;
        colResidual2Mean /= rms.length;

        rmsStd = Math.sqrt(rms2Mean - rmsMean * rmsMean);
        rowResidualStd = Math.sqrt(rowResidual2Mean - rowResidualMean * rowResidualMean);
        colResidualStd = Math.sqrt(colResidual2Mean - colResidualMean * colResidualMean);
    }

    /**
     * Eliminate master and slave GCP pairs that have root mean square error greater than given threshold.
     *
     * @param threshold Threshold for eliminating GCPs.
     * @return True if some GCPs are eliminated, false otherwise.
     */
    private boolean eliminateGCPsBasedOnRMS(final float threshold) {

        final List<Placemark> pinList = new ArrayList<>();
        if (slaveGCPList.size() < rms.length) {
            notEnoughGCPs = true;
            return true;
        }
        for (int i = 0; i < rms.length; i++) {
            if (rms[i] >= threshold) {
                pinList.add(slaveGCPList.get(i));
                //System.out.println("WARP: slave gcp[" + i + "] is eliminated");
            }
        }

        for (Placemark aPin : pinList) {
            slaveGCPList.remove(aPin);
        }

        return !pinList.isEmpty();
    }

    /**
     * Compute warped GCPs.
     *
     * @param warpPolynomialOrder The WARP polynomial order.
     * @param mX                  The x coordinate of master GCP.
     * @param mY                  The y coordinate of master GCP.
     * @param slavePos            The warped GCP position.
     * @throws OperatorException The exceptions.
     */
    public void getWarpedCoords(final int warpPolynomialOrder,
                                       final double mX, final double mY, final PixelPos slavePos)
            throws OperatorException {

        final double[] xCoeffs = xCoef;
        final double[] yCoeffs = yCoef;
        if (xCoeffs.length != yCoeffs.length) {
            throw new OperatorException("WARP has different number of coefficients for X and Y");
        }

        final int numOfCoeffs = xCoeffs.length;
        switch (warpPolynomialOrder) {
            case 1: {
                if (numOfCoeffs != 3) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY;
                break;
            }
            case 2: {
                if (numOfCoeffs != 6) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY;
                break;
            }
            case 3: {
                if (numOfCoeffs != 10) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY +
                        xCoeffs[6] * mXmX * mX + xCoeffs[7] * mX * mXmY + xCoeffs[8] * mXmY * mY + xCoeffs[9] * mYmY * mY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY +
                        yCoeffs[6] * mXmX * mX + yCoeffs[7] * mX * mXmY + yCoeffs[8] * mXmY * mY + yCoeffs[9] * mYmY * mY;
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
    private static void outputCoRegistrationInfo(final Product sourceProduct, final int warpPolynomialOrder,
                                                final WarpData warpData, final boolean appendFlag,
                                                final float threshold, final int parseIndex, final String bandName)
            throws OperatorException {

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
                for (double xCoeff : warpData.xCoef) {
                    p.print((float) xCoeff + ", ");
                }

                p.println();
                for (double yCoeff : warpData.yCoef) {
                    p.print((float) yCoeff + ", ");
                }
                p.println();
            }

            if (appendFlag) {
                p.println();
                p.format("RMS Threshold: %5.3f", threshold);
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
                p.println("  No.  | Master GCP x | Master GCP y | Slave GCP x  | Slave GCP y  | Row Residual | Col Residual |        RMS        |");
                p.println("----------------------------------------------------------------------------------------------------------------------");
                for (int i = 0; i < warpData.rms.length; i++) {
                    p.format("%6d |%13.3f |%13.3f |%13.3f |%13.3f |%13.8f |%13.8f |%18.12f |",
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
        final String fileName = sourceProduct.getName() + "_residual.txt";
        return new File(ResourceUtils.getReportFolder(), fileName);
    }
}
