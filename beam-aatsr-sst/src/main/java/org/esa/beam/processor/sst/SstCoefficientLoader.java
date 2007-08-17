/*
 * $Id: SstCoefficientLoader.java,v 1.3 2006/11/15 09:22:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.sst;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Convenience class for SST coefficient file handling. This class loads and verifies SST coefficient sets containd in
 * files conforming to the specifications denoted in the BEAM documentation.
 * <p/>
 * A short description of the format:<br> <ul> <li>A coefficient file is a standard java properties file consisting of
 * key/value pairs and comments.</li> <li>A comment line begins with a "#" and is ignored.</li> <li>Each coefficient
 * file should contain a short description, denoted as: <code>description = my coefficient description</code>.</li>
 * <li>The coefficient file contains any number of so called map keys. These keys define a pixel range across the
 * scanline where a specific set of coefficients shall be used.</li> <li>For every map range there must be a
 * corresponding set of coefficients. </li> </ul>
 * <p/>
 * Example:
 * <p/>
 * <code> # Testfile for SST coefficient sets<br> #<br> # The syntax is always<br> #   key=value<br> #<br> # Allowed
 * keys are:<br> #   description - a short description of the coefficient set<br> #   map.x        - defines a range of
 * scanline pixels as coefficient set x<br> #   a.x            - coefficient set for map x<br> #   b.x to d.x   - same
 * as a.x<br> <br> description=test coefficient set for nadir SST<br> <br> map.0=0, 234<br> map.1=235, 511<br> <br>
 * a.0=1.0, 2.0, 3.0<br> b.0=1.0, 2.0, 3.0, 4.0, 5.0<br> <br> a.1=1.0, 2.0, 3.0<br> b.1=1.0, 2.0, 3.0, 4.0, 5.0<br> <br>
 * </code>
 */
public class SstCoefficientLoader {

    private static final String _descriptionKey = "description";
    private static final String _mapKeyStub = "map.";
    private static final String _aKeyStub = "a.";
    private static final String _bKeyStub = "b.";
    private static final String _cKeyStub = "c.";
    private static final String _dKeyStub = "d.";
    private static final char[] _separators = new char[]{','};

    private Properties _props;
    private static final int _numACoeffs = 3;
    private static final int _numBCoeffs = 4;
    private static final int _numCCoeffs = 5;
    private static final int _numDCoeffs = 7;

    private Logger _logger;

    /**
     * Constructs the object with default values
     */
    public SstCoefficientLoader() {
        _props = new Properties();
        _logger = Logger.getLogger(SstConstants.LOGGER_NAME);
    }

    /**
     * Loads a coefficient file passed in as URL and verifies the content for consistency
     *
     * @return a validated coeffcient set contained in the file
     */
    public SstCoefficientSet load(URL coeffFile) throws IOException,
                                                        ProcessorException {
        Guardian.assertNotNull("coeffFile", coeffFile);

        _logger.fine("Reading coefficient file: '" + coeffFile.getPath() + "'");

        InputStream inStream = coeffFile.openStream();
        _props.clear();
        try {
            _props.load(inStream);
        } finally {
            inStream.close();
        }

        SstCoefficientSet set = new SstCoefficientSet();

        addDescription(set);
        addCoefficients(set);

        // check that what we return is really valid
        verify(set);

        _logger.fine(SstConstants.LOG_MSG_SUCCESS);

        return set;
    }

    /**
     * Retrieves the description of the coefficient file passed in
     */
    public String getDescription(URL coeffFile) throws IOException {
        Guardian.assertNotNull("coeffFile", coeffFile);
        String desc = "";

        File file = null;
        try {
            file = new File(coeffFile.toURI());
        } catch (URISyntaxException e) {
            // ignore
        }

        if (file != null && file.exists() && file.isFile()) {
            InputStream inStream = new FileInputStream(file);
            _props.load(inStream);
            inStream.close();
            desc = _props.getProperty(_descriptionKey);
        }

        return desc;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a parameter set description to the set passed in. When no description is in the file currently parsed - just
     * does nothing
     */
    private void addDescription(SstCoefficientSet set) {
        String description;

        description = _props.getProperty(_descriptionKey);
        if (description != null) {
            set.setDescription(description);
            _logger.fine("... coefficients description: '" + description + "'");
        } else {
            _logger.fine("... coefficients have no description");
        }
    }

    /**
     * Adds all coefficients with the appropriate ranges to the set passed in
     */
    private void addCoefficients(SstCoefficientSet set) throws ProcessorException {
        int index = 0;
        String mapKey = _mapKeyStub + index;
        String aKey = _aKeyStub + index;
        String bKey = _bKeyStub + index;
        String cKey = _cKeyStub + index;
        String dKey = _dKeyStub + index;
        String value;
        SstCoefficients coeffs;

        // scan for all properties named map.x
        while ((value = _props.getProperty(mapKey)) != null) {
            coeffs = new SstCoefficients();

            loadMapStringToCoefficients(value, coeffs);

            // try to read the associated a - coefficients
            value = _props.getProperty(aKey);
            if (value != null) {
                load_A_ToCoefficients(value, coeffs);
            }

            // try to read the associated b - coefficients
            value = _props.getProperty(bKey);
            if (value != null) {
                load_B_ToCoefficients(value, coeffs);
            }

            // try to read the associated c - coefficients
            value = _props.getProperty(cKey);
            if (value != null) {
                load_C_ToCoefficients(value, coeffs);
            }

            // try to read the associated d - coefficients
            value = _props.getProperty(dKey);
            if (value != null) {
                load_D_ToCoefficients(value, coeffs);
            }

            // add the coefficients to the set
            set.addCoefficients(coeffs);

            index++;
            mapKey = _mapKeyStub + index;
            aKey = _aKeyStub + index;
            bKey = _bKeyStub + index;
            cKey = _cKeyStub + index;
            dKey = _dKeyStub + index;
        }
    }

    /**
     * Scans the map key passed in and loads the ranges read out to the coefficients passed in
     */
    private static void loadMapStringToCoefficients(String map, SstCoefficients coeffs) throws ProcessorException {
        String[] rangeStrings = StringUtils.split(map, _separators, true);

        // check that we have TWO values
        if (rangeStrings.length != 2) {
            throw new ProcessorException("illegal coefficient file format: map.x must have two values");
        }

        coeffs.setRange(Integer.parseInt(rangeStrings[0]), Integer.parseInt(rangeStrings[1]));
    }

    /**
     * Scans the String passed in for the a coefficient set and loads thme into the coefficients passed in
     */
    private static void load_A_ToCoefficients(String a_values, SstCoefficients coeffs) throws ProcessorException {
        String[] aStrings = StringUtils.split(a_values, _separators, true);

        // check that we have THREE values
        if (aStrings.length != _numACoeffs) {
            throw new ProcessorException("illegal coefficient file format: a.x must have " + _numACoeffs + " values");
        }

        // convert the string array to float values
        float[] aCoeffs = new float[aStrings.length];

        for (int n = 0; n < aStrings.length; n++) {
            aCoeffs[n] = Float.parseFloat(aStrings[n]);
        }

        // and set
        coeffs.set_A_Coeffs(aCoeffs);
    }

    /**
     * Scans the String passed in for the b coefficient set and loads them into the coefficients passed in
     */
    private static void load_B_ToCoefficients(String b_values, SstCoefficients coeffs) throws ProcessorException {
        String[] bStrings = StringUtils.split(b_values, _separators, true);

        // check that we have FOUR values
        if (bStrings.length != _numBCoeffs) {
            throw new ProcessorException("illegal coefficient file format: b.x must have " + _numBCoeffs + " values");
        }

        // convert the string array to float values
        float[] bCoeffs = new float[bStrings.length];

        for (int n = 0; n < bStrings.length; n++) {
            bCoeffs[n] = Float.parseFloat(bStrings[n]);
        }

        // and set
        coeffs.set_B_Coeffs(bCoeffs);
    }

    /**
     * Scans the String passed in for the c coefficient set and loads them into the coefficients passed in
     */
    private static void load_C_ToCoefficients(String c_values, SstCoefficients coeffs) throws ProcessorException {
        String[] cStrings = StringUtils.split(c_values, _separators, true);

        // check that we have FIVE values
        if (cStrings.length != _numCCoeffs) {
            throw new ProcessorException("illegal coefficient file format: c.x must have " + _numCCoeffs + " values");
        }

        // convert the string array to float values
        float[] cCoeffs = new float[cStrings.length];

        for (int n = 0; n < cStrings.length; n++) {
            cCoeffs[n] = Float.parseFloat(cStrings[n]);
        }

        // and set
        coeffs.set_C_Coeffs(cCoeffs);
    }

    /**
     * Scans the String passed in for the d coefficient set and loads them into the coefficients passed in
     */
    private static void load_D_ToCoefficients(String d_values, SstCoefficients coeffs) throws ProcessorException {
        String[] dStrings = StringUtils.split(d_values, _separators, true);

        // check that we have SEVEN values
        if (dStrings.length != _numDCoeffs) {
            throw new ProcessorException("illegal coefficient file format: d.x must have " + _numDCoeffs + " values");
        }

        // convert the string array to float values
        float[] dCoeffs = new float[dStrings.length];

        for (int n = 0; n < dStrings.length; n++) {
            dCoeffs[n] = Float.parseFloat(dStrings[n]);
        }

        // and set
        coeffs.set_D_Coeffs(dCoeffs);
    }

    /**
     * Checks that the set passed in is a valid coefficient set for the sst processor
     */
    private void verify(SstCoefficientSet set) throws ProcessorException {
        SstCoefficients coeffs;
        // get the number of coefficients in the set
        int nMaps = set.getNumCoefficients();
        int nStart;
        int nExpStart;
        int nEnd;

        // first, check that the coordinate ranges are not overlapping
        // or having a gap or have negative values ...
        // ------------------------------------------------------------
        nExpStart = 0;
        for (int n = 0; n < nMaps; n++) {
            coeffs = set.getCoefficientsAt(n);
            nStart = coeffs.getStart();
            nEnd = coeffs.getEnd();
            if ((nStart < 0) || (nEnd < 0) || (nEnd < nStart)) {
                throw new ProcessorException(
                        "illegal coefficient file: map." + n + " start: " + nStart + " end: " + nEnd);
            }

            // check that there is no gap
            if (nStart != nExpStart) {
                throw new ProcessorException(
                        "illegal coefficient file: map." + n + " expected start: " + nExpStart + " actual: " + nStart);
            }

            nExpStart = nEnd + 1;
            _logger.finest("... map." + n + " : " + nStart + " - " + nEnd);
        }

        // now check that each coefficient set either has both a and b
        // coefficients or both c and d.
        float[] testOne;
        float[] testTwo;
        boolean bHasCoeffs = false;
        for (int n = 0; n < nMaps; n++) {
            coeffs = set.getCoefficientsAt(n);

            testOne = coeffs.get_A_Coeffs();
            if (testOne != null) {
                bHasCoeffs = true;
                // has a coefficients - must also have b coefficcients
                testTwo = coeffs.get_B_Coeffs();
                if (testTwo == null) {
                    throw new ProcessorException(
                            "illegal coefficient file: map." + n + "has a coefficients but no b coefficient set");
                }

                _logger.finest("... a." + n + " : " + StringUtils.arrayToCsv(testOne));
                _logger.finest("... b." + n + " : " + StringUtils.arrayToCsv(testTwo));
            }

            testOne = coeffs.get_C_Coeffs();
            if (testOne != null) {
                bHasCoeffs = true;
                // has a coefficients - must also have b coefficcients
                testTwo = coeffs.get_D_Coeffs();
                if (testTwo == null) {
                    throw new ProcessorException(
                            "illegal coefficient file: map." + n + "has c coefficients but no d coefficient set");
                }

                _logger.finest("... c." + n + " : " + StringUtils.arrayToCsv(testOne));
                _logger.finest("... d." + n + " : " + StringUtils.arrayToCsv(testTwo));
            }

            if (!bHasCoeffs) {
                throw new ProcessorException(
                        "illegal coefficient file: map." + n + " has neither a not c coefficients");
            }

            bHasCoeffs = false;
        }
    }
}
