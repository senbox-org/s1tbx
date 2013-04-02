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
package org.esa.beam.processor.smac;

import org.esa.beam.util.Guardian;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;

/**
 * Implements the <code>SmacSensorCoefficients</code> interface for file access.
 * <p/>
 * The files must conform the format specified by the original source code by H.Rahman and G.Dedieu.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
class SensorCoefficientFile implements SmacSensorCoefficients {

    private double _ah2o;
    private double _nh2o;
    private double _ao3;
    private double _no3;
    private double _ao2;
    private double _no2;
    private double _po2;
    private double _aco2;
    private double _nco2;
    private double _pco2;
    private double _ach4;
    private double _nch4;
    private double _pch4;
    private double _ano2;
    private double _nno2;
    private double _pno2;
    private double _aco;
    private double _nco;
    private double _pco;
    private double _a0s;
    private double _a1s;
    private double _a2s;
    private double _a3s;
    private double _a0T;
    private double _a1T;
    private double _a2T;
    private double _a3T;
    private double _taur;
    private double _sr;
    private double _a0taup;
    private double _a1taup;
    private double _wo;
    private double _gc;
    private double _a0P;
    private double _a1P;
    private double _a2P;
    private double _a3P;
    private double _a4P;
    private double _rest1;
    private double _rest2;
    private double _rest3;
    private double _rest4;
    private double _resr1;
    private double _resr2;
    private double _resr3;
    private double _resa1;
    private double _resa2;
    private double _resa3;
    private double _resa4;

    /**
     * Creates the object with default parameters.
     */
    public SensorCoefficientFile() {
    }

    /**
     * Sets the sensor coefficients file name for this class and reads the file.
     *
     * @param fileName the name of the sensor coefficients file
     *
     * @throws java.lang.IllegalArgumentException
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void readFile(String fileName) throws IllegalArgumentException,
                                                 FileNotFoundException,
                                                 IOException {
        Guardian.assertNotNull("fileName", fileName);
        File coeffFile = new File(fileName);

        // check if file is available
        if (!coeffFile.exists() || !coeffFile.isFile()) {
            throw new FileNotFoundException();
        }

        scan(coeffFile);
    }

    /**
     * Retrieves the gaseous absorption coefficient for h20.
     */
    public double getAh2o() {
        return _ah2o;
    }

    /**
     * Retrieves the gaseous absorption exponent for h2o.
     */
    public double getNh2o() {
        return _nh2o;
    }

    /**
     * Retrieves the gaseous absorption coefficient for o3.
     */
    public double getAo3() {
        return _ao3;
    }

    /**
     * Retrieves the gaseous absorption exponent for o3.
     */
    public double getNo3() {
        return _no3;
    }

    /**
     * Retrieves the gaseous absorption coefficient for o2.
     */
    public double getAo2() {
        return _ao2;
    }

    /**
     * Retrieves the gaseous absorption exponent for o2.
     */
    public double getNo2() {
        return _no2;
    }

    /**
     * Retrieves the gaseous transmission coefficient for o2.
     */
    public double getPo2() {
        return _po2;
    }

    /**
     * Retrieves the gaseous absorption coefficient for co2.
     */
    public double getAco2() {
        return _aco2;
    }

    /**
     * Retrieves the gaseous absorption exponent for co2.
     */
    public double getNco2() {
        return _nco2;
    }

    /**
     * Retrieves the gaseous transmission coefficient for co2.
     */
    public double getPco2() {
        return _pco2;
    }

    /**
     * Retrieves the gaseous absorption coefficient for ch4.
     */
    public double getAch4() {
        return _ach4;
    }

    /**
     * Retrieves the gaseous absorption exponent for ch4.
     */
    public double getNch4() {
        return _nch4;
    }

    /**
     * Retrieves the gaseous transmission coefficient for ch4.
     */
    public double getPch4() {
        return _pch4;
    }

    /**
     * Retrieves the gaseous absorption coefficient for no2.
     */
    public double getAno2() {
        return _ano2;
    }

    /**
     * Retrieves the gaseous absorption exponent for no2.
     */
    public double getNno2() {
        return _nno2;
    }

    /**
     * Retrieves the gaseous transmission coefficient for no2.
     */
    public double getPno2() {
        return _pno2;
    }

    /**
     * Retrieves the gaseous absorption coefficient for co2.
     */
    public double getAco() {
        return _aco;
    }

    /**
     * Retrieves the gaseous absorption exponent for co2.
     */
    public double getNco() {
        return _nco;
    }

    /**
     * Retrieves the gaseous transmission coefficient for co.
     */
    public double getPco() {
        return _pco;
    }

    /**
     * Retrieves the spherical albedo coefficient 0.
     */
    public double getA0s() {
        return _a0s;
    }

    /**
     * Retrieves the spherical albedo coefficient 1.
     */
    public double getA1s() {
        return _a1s;
    }

    /**
     * Retrieves the spherical albedo coefficient 2.
     */
    public double getA2s() {
        return _a2s;
    }

    /**
     * Retrieves the spherical albedo coefficient 3.
     */
    public double getA3s() {
        return _a3s;
    }

    /**
     * Retrieves the scattering transmission coefficient 0.
     */
    public double getA0T() {
        return _a0T;
    }

    /**
     * Retrieves the scattering transmission coefficient 1.
     */
    public double getA1T() {
        return _a1T;
    }

    /**
     * Retrieves the scattering transmission coefficient 2.
     */
    public double getA2T() {
        return _a2T;
    }

    /**
     * Retrieves the scattering transmission coefficient 3.
     */
    public double getA3T() {
        return _a3T;
    }

    /**
     * Retrieves the molecular optical depth.
     */
    public double getTaur() {
        return _taur;
    }

    public double getSr() {
        return _sr;
    }

    /**
     * Retrieves aerosol optical depth coefficient 0.
     */
    public double getA0taup() {
        return _a0taup;
    }

    /**
     * Retrieves aerosol optical depth coefficient 1.
     */
    public double getA1taup() {
        return _a1taup;
    }

    public double getWo() {
        return _wo;
    }

    public double getGc() {
        return _gc;
    }

    /**
     * Retrieves aerosol reflectance coefficient 0.
     */
    public double getA0P() {
        return _a0P;
    }

    /**
     * Retrieves aerosol reflectance coefficient 1.
     */
    public double getA1P() {
        return _a1P;
    }

    /**
     * Retrieves aerosol reflectance coefficient 2.
     */
    public double getA2P() {
        return _a2P;
    }

    /**
     * Retrieves aerosol reflectance coefficient 3.
     */
    public double getA3P() {
        return _a3P;
    }

    /**
     * Retrieves aerosol reflectance coefficient 4.
     */
    public double getA4P() {
        return _a4P;
    }

    /**
     * Retrieves the residual transmission coefficient 1.
     */
    public double getRest1() {
        return _rest1;
    }

    /**
     * Retrieves the residual transmission coefficient 2.
     */
    public double getRest2() {
        return _rest2;
    }

    /**
     * Retrieves the residual transmission coefficient3.
     */
    public double getRest3() {
        return _rest3;
    }

    /**
     * Retrieves the residual transmission coefficient 4.
     */
    public double getRest4() {
        return _rest4;
    }

    /**
     * Retrieves the residual rayleigh coefficient 1.
     */
    public double getResr1() {
        return _resr1;
    }

    /**
     * Retrieves the residual rayleigh coefficient 2.
     */
    public double getResr2() {
        return _resr2;
    }

    /**
     * Retrieves the residual rayleigh coefficient 3.
     */
    public double getResr3() {
        return _resr3;
    }

    /**
     * Retrieves the residual aerosol coefficient 1.
     */
    public double getResa1() {
        return _resa1;
    }

    /**
     * Retrieves the residual aerosol coefficient 2.
     */
    public double getResa2() {
        return _resa2;
    }

    /**
     * Retrieves the residual aerosol coefficient 3.
     */
    public double getResa3() {
        return _resa3;
    }

    /**
     * Retrieves the residual aerosol coefficient 4.
     */
    public double getResa4() {
        return _resa4;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Scans the file for coefficients
     */
    private void scan(File coeffFile) throws FileNotFoundException,
                                             IOException {
        FileReader reader = new FileReader(coeffFile);
        StreamTokenizer tokenizer = new StreamTokenizer(reader);

        tokenizer.resetSyntax();
        tokenizer.whitespaceChars(0, 32);
        tokenizer.eolIsSignificant(false);
        tokenizer.wordChars(33, 255);

        _ah2o = getNextCoefficient(tokenizer);
        _nh2o = getNextCoefficient(tokenizer);
        _ao3 = getNextCoefficient(tokenizer);
        _no3 = getNextCoefficient(tokenizer);
        _ao2 = getNextCoefficient(tokenizer);
        _no2 = getNextCoefficient(tokenizer);
        _po2 = getNextCoefficient(tokenizer);
        _aco2 = getNextCoefficient(tokenizer);
        _nco2 = getNextCoefficient(tokenizer);
        _pco2 = getNextCoefficient(tokenizer);
        _ach4 = getNextCoefficient(tokenizer);
        _nch4 = getNextCoefficient(tokenizer);
        _pch4 = getNextCoefficient(tokenizer);
        _ano2 = getNextCoefficient(tokenizer);
        _nno2 = getNextCoefficient(tokenizer);
        _pno2 = getNextCoefficient(tokenizer);
        _aco = getNextCoefficient(tokenizer);
        _nco = getNextCoefficient(tokenizer);
        _pco = getNextCoefficient(tokenizer);
        _a0s = getNextCoefficient(tokenizer);
        _a1s = getNextCoefficient(tokenizer);
        _a2s = getNextCoefficient(tokenizer);
        _a3s = getNextCoefficient(tokenizer);
        _a0T = getNextCoefficient(tokenizer);
        _a1T = getNextCoefficient(tokenizer);
        _a2T = getNextCoefficient(tokenizer);
        _a3T = getNextCoefficient(tokenizer);
        _taur = getNextCoefficient(tokenizer);
        _sr = getNextCoefficient(tokenizer);
        _a0taup = getNextCoefficient(tokenizer);
        _a1taup = getNextCoefficient(tokenizer);
        _wo = getNextCoefficient(tokenizer);
        _gc = getNextCoefficient(tokenizer);
        _a0P = getNextCoefficient(tokenizer);
        _a1P = getNextCoefficient(tokenizer);
        _a2P = getNextCoefficient(tokenizer);
        _a3P = getNextCoefficient(tokenizer);
        _a4P = getNextCoefficient(tokenizer);
        _rest1 = getNextCoefficient(tokenizer);
        _rest2 = getNextCoefficient(tokenizer);
        _rest3 = getNextCoefficient(tokenizer);
        _rest4 = getNextCoefficient(tokenizer);
        _resr1 = getNextCoefficient(tokenizer);
        _resr2 = getNextCoefficient(tokenizer);
        _resr3 = getNextCoefficient(tokenizer);
        _resa1 = getNextCoefficient(tokenizer);
        _resa2 = getNextCoefficient(tokenizer);
        _resa3 = getNextCoefficient(tokenizer);
        _resa4 = getNextCoefficient(tokenizer);
    }

    /**
     * Retrieves the next double precision value from the coeficient file
     */
    private static double getNextCoefficient(StreamTokenizer tokenizer) throws IOException {
        double ret;

        tokenizer.nextToken();

        if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
            ret = tokenizer.nval;
        } else {
            ret = Double.valueOf(tokenizer.sval).doubleValue();
        }

        return ret;
    }
}