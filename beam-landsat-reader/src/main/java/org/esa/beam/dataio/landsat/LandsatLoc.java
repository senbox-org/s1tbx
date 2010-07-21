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

package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.fast.Landsat5FASTConstants;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The class <code>LandsatLoc</code> is used to store the data of the row/path location of a Landsat TM product
 * Landsat Location Dataset
 * FAST REV.B = ppp/rrrff
 * <p/>
 * p = path
 * r = row
 * f = fraction
 * s = subscene
 * b = blank
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */

public final class LandsatLoc {

    private String path = null;
    private String row = null;
    private String fraction = null;
    private String subscene = null;

    /**
     * @param locationString
     * @param format
     */
    public LandsatLoc(final String locationString, final int format) {

        if (isValideLocationString(locationString, format)) {
            parseLocationString(locationString, format);
        }
    }

    /**
     * @param path
     * @param row
     */
    public LandsatLoc(final String path, final String row) {
        this.path = path;
        this.row = row;
    }

    /**
     * TODO
     *
     * @param locationString
     * @param format
     */

    private void parseLocationString(final String locationString, final int format) {
        if (format == LandsatConstants.FAST_L5) {
            setPath(locationString.substring(0, 3));
            setRow(locationString.substring(4, 7));
            setFraction(locationString.substring(7));
        }
    }

    /**
     * TODO
     *
     * @param locationString
     * @param format
     *
     * @return <code>true</code> if the location string is valide <code>false</code> if not
     */


    private static boolean isValideLocationString(final String locationString, final int format) {
        Guardian.assertNotNullOrEmpty("locationString", locationString);
        String trimmedLocationString = locationString.trim();

        if (format == LandsatConstants.FAST_L5) {
            if (trimmedLocationString.length() == Landsat5FASTConstants.LENGTH_OF_FASTB_LOCATION_STRING) {
                if (trimmedLocationString.charAt(LandsatConstants.POSITION_OF_SEPERATOR) == '/') {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param path
     *
     * @return <code>true</code> if the value saved in the string is a valide path value <code>false</code> if the value is not valide
     */


    private static boolean checkPath(final String path) {

        int pathNumber = Integer.parseInt(path);
        return pathNumber > 0 && pathNumber < 234;
    }

    /**
     * @param row
     *
     * @return @return <code>true</code> if the value saved in the string is a valide row value <code>false</code> if the value is not valide
     */

    private static boolean checkRow(final String row) {
        int rowNumber = Integer.parseInt(row);
        return rowNumber > 0 && rowNumber < 249;
    }

    /**
     * @return fraction value of the LANDSAT location
     */

    public final String getFraction() {
        return fraction;
    }


    /**
     * @param _fraction
     */

    private void setFraction(final String _fraction) {
        this.fraction = _fraction;
    }

    /**
     * @return the path of LANDSAT image
     */
    public final String getPath() {
        return path;
    }

    /**
     * @param _path
     */
    private void setPath(final String _path) {

        if (checkPath(_path)) {
            this.path = _path;
        } else {
            Debug.trace("wrong path format");
        }
    }

    /**
     * @return the row of a LANDSAT image
     */
    public final String getRow() {
        return row;
    }

    /**
     * @param _row
     */
    private void setRow(final String _row) {
        if (checkRow(_row)) {
            this.row = _row;
        } else {
            Debug.trace("wrong row format");
        }
    }

    /**
     * @return subscene of LANDSAT location
     */
    public final String getSubscene() {
        return subscene;
    }

    /**
     * @param _subscene
     */
    public final void setSubscene(final String _subscene) {
        this.subscene = _subscene;
    }


    /**
     * @return array with the location Data (path, row, fraction, subscene)
     */

    public final String [] locationRecord() {
        List<String> locRec = new ArrayList<String>();
        String [] locArray = {path, row, fraction, subscene};

        for (int i = 0; i < locArray.length; i++) {
            if (locArray[i] != null && !locArray[i].equals("")) {

                locRec.add(locArray[i]);
            }
        }
        String [] tempArray = new String [locRec.size()];
        int i = 0;
        for (Iterator<String> iter = locRec.iterator(); iter.hasNext();) {
            tempArray[i++] = iter.next();
        }

        return tempArray;
    }

    /**
     * @return array with the descriptions of the location data ("Path","Row","Fraction","Subscene"}
     */

    public final String [] locationRecordDescription() {

        List<String> locRec = new ArrayList<String>();
        String [] locArray = {path, row, fraction, subscene};

        for (int i = 0; i < locArray.length; i++) {
            if (locArray[i] != null && !locArray[i].equals("")) {
                locRec.add(LandsatConstants.LOC_DESC[i]);
            }
        }
        String [] tempArray = new String [locRec.size()];
        int i = 0;
        for (Iterator<String> iter = locRec.iterator(); iter.hasNext();) {
            tempArray[i++] = iter.next();
        }
        return tempArray;
    }

}
