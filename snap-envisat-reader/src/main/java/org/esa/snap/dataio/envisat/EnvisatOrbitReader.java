/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

public class EnvisatOrbitReader extends EnvisatAuxReader {

    private OrbitVector[] dataRecords = null;
    private double[] recordTimes = null;

    public EnvisatOrbitReader() {
        super();
    }

    public void readOrbitData() throws IOException {

        if (_productFile instanceof DorisOrbitProductFile) {

            final DorisOrbitProductFile dorisProdFile = (DorisOrbitProductFile) _productFile;
            final Record orbitRecord = dorisProdFile.readOrbitData();

            OrbitVector orb = null;
            final ArrayList<OrbitVector> orbitVectorList = new ArrayList<>();

            final int numFields = orbitRecord.getNumFields();
            for (int i = 0; i < numFields; ++i) {

                final Field f = orbitRecord.getFieldAt(i);

                final String fieldName = f.getName();
                if (fieldName.contains("blank")) {
                    continue;
                } else if (fieldName.contains("utc_time")) {
                    orb = new OrbitVector();
                    try {
                        orb.utcTime = ProductData.UTC.parse(f.getData().getElemString()).getMJD();
                    } catch (ParseException e) {
                        throw new IllegalFileFormatException("Failed to parse UTC time " + e.getMessage());
                    }
                } else if (fieldName.contains("delta_ut1")) {
                    if (orb != null)
                        orb.delta_ut1 = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("abs_orbit")) {
                    if (orb != null)
                        orb.absOrbit = Integer.parseInt(f.getData().getElemString().replace("+",""));
                } else if (fieldName.contains("x_pos")) {
                    if (orb != null)
                        orb.xPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("y_pos")) {
                    if (orb != null)
                        orb.yPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("z_pos")) {
                    if (orb != null)
                        orb.zPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("x_vel")) {
                    if (orb != null)
                        orb.xVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("y_vel")) {
                    if (orb != null)
                        orb.yVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("z_vel")) {
                    if (orb != null)
                        orb.zVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("qual_flags")) {
                    if (orb != null)
                        orb.qualFlags = f.getData().getElemString();
                    orbitVectorList.add(orb);
                }
            }

            dataRecords = orbitVectorList.toArray(new OrbitVector[orbitVectorList.size()]);

            recordTimes = new double[dataRecords.length];
            for (int i = 0; i < dataRecords.length; i++) {
                recordTimes[i] = dataRecords[i].utcTime;
            }
        }
    }

    public OrbitVector getOrbitVector(int n) {
        return dataRecords[n];
    }

    public int getNumRecords() {
        return dataRecords.length;
    }

    /**
     * Get orbit vector for given UTC time.
     * @param utc The UTC time
     * @throws Exception for incorrect time
     * @return The orbit vector
     */
    public OrbitVector getOrbitVector(double utc) throws Exception {

        final int interpOrder = 8;
        final int nRecords = recordTimes.length;
        double t0 = recordTimes[0];
        double tN = recordTimes[nRecords - 1];

        // final int n = Arrays.binarySearch(recordTimes, utc);
        // binary search not needed, we can pre-compute index for given utc wrt delta, and start/end time
        final double tRel = (utc - t0) / (tN - t0) * (nRecords - 1); // data index from 0
        final int itRel = (int) Math.max(1, Math.min(Math.round(tRel) - (interpOrder / 2), (nRecords - 1) - interpOrder));

        // indices for populating arrays - Working with 8th Order => 9 points
        final int n0 = itRel;
        final int n1 = itRel + 1;
        final int n2 = itRel + 2;
        final int n3 = itRel + 3;
        final int n4 = itRel + 4; // <- closest to interpolation point
        final int n5 = itRel + 5;
        final int n6 = itRel + 6;
        final int n7 = itRel + 7;
        final int n8 = itRel + 8;

        // TODO: Verify validity of check for incorrect UTC time in parsing orbit
        // ....I'm not sure that this check is very smart, and that it would pick up not fully overlapping arc
        if (n0 < 0 || n1 < 0 || n2 < 0 || n3 < 0 || n4 < 0 || n5 > nRecords || n6 > nRecords || n7 > nRecords || n8 > nRecords) {
            throw new Exception("Incorrect UTC time");
        }

        final double[] delta_ut1Array = {dataRecords[n0].delta_ut1, dataRecords[n1].delta_ut1, dataRecords[n2].delta_ut1, dataRecords[n3].delta_ut1,
                dataRecords[n4].delta_ut1,
                dataRecords[n5].delta_ut1, dataRecords[n6].delta_ut1, dataRecords[n7].delta_ut1, dataRecords[n8].delta_ut1};

        final double[] xPosArray = {dataRecords[n0].xPos, dataRecords[n1].xPos, dataRecords[n2].xPos, dataRecords[n3].xPos,
                dataRecords[n4].xPos,
                dataRecords[n5].xPos, dataRecords[n6].xPos, dataRecords[n7].xPos, dataRecords[n8].xPos};

        final double[] yPosArray = {dataRecords[n0].yPos, dataRecords[n1].yPos, dataRecords[n2].yPos, dataRecords[n3].yPos,
                dataRecords[n4].yPos,
                dataRecords[n5].yPos, dataRecords[n6].yPos, dataRecords[n7].yPos, dataRecords[n8].yPos};

        final double[] zPosArray = {dataRecords[n0].zPos, dataRecords[n1].zPos, dataRecords[n2].zPos, dataRecords[n3].zPos,
                dataRecords[n4].zPos,
                dataRecords[n5].zPos, dataRecords[n6].zPos, dataRecords[n7].zPos, dataRecords[n8].zPos};

        final double[] xVelArray = {dataRecords[n0].xVel, dataRecords[n1].xVel, dataRecords[n2].xVel, dataRecords[n3].xVel,
                dataRecords[n4].xVel,
                dataRecords[n5].xVel, dataRecords[n6].xVel, dataRecords[n7].xVel, dataRecords[n8].xVel};

        final double[] yVelArray = {dataRecords[n0].yVel, dataRecords[n1].yVel, dataRecords[n2].yVel, dataRecords[n3].yVel,
                dataRecords[n4].yVel,
                dataRecords[n5].yVel, dataRecords[n6].yVel, dataRecords[n7].yVel, dataRecords[n8].yVel};

        final double[] zVelArray = {dataRecords[n0].zVel, dataRecords[n1].zVel, dataRecords[n2].zVel, dataRecords[n3].zVel,
                dataRecords[n4].zVel,
                dataRecords[n5].zVel, dataRecords[n6].zVel, dataRecords[n7].zVel, dataRecords[n8].zVel};


        final OrbitVector orb = new OrbitVector();

        double ref = tRel - itRel;

        orb.utcTime = utc;
        orb.absOrbit = dataRecords[n1].absOrbit;
        orb.qualFlags = dataRecords[n1].qualFlags;
        orb.delta_ut1 = lagrangeEightOrderInterpolation(delta_ut1Array, ref);
        orb.xPos = lagrangeEightOrderInterpolation(xPosArray, ref);
        orb.yPos = lagrangeEightOrderInterpolation(yPosArray, ref);
        orb.zPos = lagrangeEightOrderInterpolation(zPosArray, ref);
        orb.xVel = lagrangeEightOrderInterpolation(xVelArray, ref);
        orb.yVel = lagrangeEightOrderInterpolation(yVelArray, ref);
        orb.zVel = lagrangeEightOrderInterpolation(zVelArray, ref);
        /*
        final double dt = (utc - recordTimes[n1]) / (recordTimes[n2] - recordTimes[n1]);
        final double w0 = w(dt + 1.0);
        final double w1 = w(dt);
        final double w2 = w(1.0 - dt);
        final double w3 = w(2.0 - dt);

        final OrbitVector orb = new OrbitVector();
        orb.utcTime = utc;
        orb.absOrbit = dataRecords[n1].absOrbit;
        orb.qualFlags = dataRecords[n1].qualFlags;

        orb.delta_ut1 = w0*dataRecords[n0].delta_ut1 +
                        w1*dataRecords[n1].delta_ut1 +
                        w2*dataRecords[n2].delta_ut1 +
                        w3*dataRecords[n3].delta_ut1;

        orb.xPos = w0*dataRecords[n0].xPos +
                   w1*dataRecords[n1].xPos +
                   w2*dataRecords[n2].xPos +
                   w3*dataRecords[n3].xPos;

        orb.yPos = w0*dataRecords[n0].yPos +
                   w1*dataRecords[n1].yPos +
                   w2*dataRecords[n2].yPos +
                   w3*dataRecords[n3].yPos;

        orb.zPos = w0*dataRecords[n0].zPos +
                   w1*dataRecords[n1].zPos +
                   w2*dataRecords[n2].zPos +
                   w3*dataRecords[n3].zPos;

        orb.xVel = w0*dataRecords[n0].xVel +
                   w1*dataRecords[n1].xVel +
                   w2*dataRecords[n2].xVel +
                   w3*dataRecords[n3].xVel;

        orb.yVel = w0*dataRecords[n0].yVel +
                   w1*dataRecords[n1].yVel +
                   w2*dataRecords[n2].yVel +
                   w3*dataRecords[n3].yVel;

        orb.zVel = w0*dataRecords[n0].zVel +
                   w1*dataRecords[n1].zVel +
                   w2*dataRecords[n2].zVel +
                   w3*dataRecords[n3].zVel;
        */
        return orb;
    }

    /**
     * Perform Lagrange polynomial based interpolation.
     * @param pos Position array.
     * @param val Sample value array.
     * @param desiredPos Desired position.
     * @return The interpolated sample value.
     */
    public static double lagrangeInterpolatingPolynomial (final double pos[], final double val[], final double desiredPos)
            throws Exception {

        if (pos.length != val.length) {
            throw new Exception("Incorrect array length");
        }

        double retVal = 0;
        for (int i = 0; i < pos.length; ++i) {
            double weight = 1;
            for (int j = 0; j < pos.length; ++j) {
                if (j != i) {
                    weight *= (desiredPos - pos[j]) / (pos[i] - pos[j]);
                }
            }
            retVal += weight * val[i];
        }
        return retVal;
    }

    /**
     * Interpolate vector using 8th order Legendre interpolation.
     *
     * <p>The method interpolates a n-dimensional vector, at desired point given as input an equidistant
     * n-dimensional vectors.
     *
     * <p><b>Notes:</b> Coefficients for 8th order interpolation are pre-computed. Method is primarily designed for
     * interpolating orbits, and it should be used with care in other applications, although it should work anywhere.
     *
     * <p><b>Implementation details:</b> Adapted from 'getorb' package.
     *
     * @param samples Sample value array.
     * @param x Desired position.
     * @return The interpolated sample value.
     *
     * @author Petar Marinkovic, PPO.labs
     */
    public static double lagrangeEightOrderInterpolation(double[] samples, double x) {

        double out = 0.0d;
        final double[] denominators = {40320, -5040, 1440, -720, 576, -720, 1440, -5040, 40320};
        final double numerator = x * (x - 1) * (x - 2) * (x - 3) * (x - 4) * (x - 5) * (x - 6) * (x - 7) * (x - 8);

        if (numerator == 0) {
            return samples[(int) Math.round(x)];
        }

        double coeff;
        for (int i = 0; i < samples.length; i++) {
            coeff = numerator / denominators[i] / (x - i);
            out += coeff * samples[i];
        }
        return out;
    }

    public final static class OrbitVector {
        public double utcTime = 0;
        public double delta_ut1 = 0;
        public int absOrbit = 0;
        public double xPos = 0;
        public double yPos = 0;
        public double zPos = 0;
        public double xVel = 0;
        public double yVel = 0;
        public double zVel = 0;
        public String qualFlags = null;
    }

    /**
     * Gets the singleton instance of this class.
     * @return the singlton instance
     */
    public static EnvisatOrbitReader getInstance() {
        return Holder.instance;
    }

    /** Initialization on demand holder idiom
     */
    private static class Holder {
        private static final EnvisatOrbitReader instance = new EnvisatOrbitReader();
    }
}
