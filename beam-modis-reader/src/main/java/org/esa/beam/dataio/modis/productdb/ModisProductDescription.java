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
package org.esa.beam.dataio.modis.productdb;

import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.util.HashMap;
import java.util.Vector;

public class ModisProductDescription {

    private Vector _bandVec;
    private HashMap _bands;
    private HashMap _tiePoints;
    private Vector _tiePointVec;

    private String[] _geolocationDatasetNames;
    private String _externalGeolocationPattern;
    private boolean _flipTopDown;

    /**
     * Constructs the object with default parameters.
     */
    public ModisProductDescription() {
        _bands = new HashMap();
        _bandVec = new Vector();
        _tiePoints = new HashMap();
        _tiePointVec = new Vector();
    }

    /**
     * Adds a new band description to the product description.
     *
     * @param name          the name of the band (without spectral extension)
     * @param isSpectral    whether the badnd is a spectral band or not
     * @param scalingMethod the scaling method to be used for this band (lin, exp ..)
     * @param scaleName     name of the attribute containing the scale factors
     * @param offsetName    name of the attribute containing the scale offsets
     * @param unitName      name off the attribute containing the physical unit
     * @param bandName      name of the attribute containing the spectral extensions (band names)
     * @param descName      name of the attribute containing a description of the band
     */
    public void addBand(String name, String isSpectral, String scalingMethod, String scaleName,
                        String offsetName, String unitName, String bandName, String descName) {
        addBand(new ModisBandDescription(
                    name, isSpectral, scalingMethod, scaleName,
                    offsetName, unitName, bandName, descName));
    }

    /**
     * Adds a new band description to the product description.
     *
     * @param name               the name of the band (without spectral extension)
     * @param isSpectral         whether the badnd is a spectral band or not
     * @param scalingMethod      the scaling method to be used for this band (lin, exp ..)
     * @param scaleName          name of the attribute containing the scale factors
     * @param offsetName         name of the attribute containing the scale offsets
     * @param unitName           name off the attribute containing the physical unit
     * @param bandName           name of the attribute containing the spectral extensions (band names)
     * @param descName           name of the attribute containing a description of the band
     * @param spectralWaveLength the spectral wavelength in nm (nanomater) units
     * @param spectralBandWidth  the spectral bandwidth in nm (nanomater) units
     * @param spectralBandIndex  the (zero-based) spectral band index
     */
    public void addBand(final String name, final String isSpectral, final String scalingMethod,
                        final String scaleName, final String offsetName, final String unitName,
                        final String bandName, final String descName,
                        final String spectralWaveLength, final String spectralBandWidth,
                        final String spectralBandIndex) {
        final ModisBandDescription bandDesc = new ModisBandDescription(
                    name, isSpectral, scalingMethod, scaleName,
                    offsetName, unitName, bandName, descName);
        bandDesc.setSpecInfo(new ModisSpectralInfo(
                    spectralWaveLength, spectralBandWidth, spectralBandIndex));
        addBand(bandDesc);
    }

    private void addBand(final ModisBandDescription bandDesc) {
        _bandVec.add(bandDesc);
        _bands.put(bandDesc.getName(), bandDesc);
    }

    /**
     * Retrieves the names of the scientific datasets of the product
     *
     * @return the names of the scientific datasets.
     */
    public String[] getBandNames() {
        String[] strRet = new String[_bandVec.size()];

        for (int n = 0; n < _bandVec.size(); n++) {
            strRet[n] = ((ModisBandDescription) _bandVec.get(n)).getName();
        }
        return strRet;
    }

    /**
     * Retrieves the description for the band with the given name
     *
     * @param bandName
     *
     * @return the description of the band
     */
    public ModisBandDescription getBandDescription(String bandName) {
        return (ModisBandDescription) _bands.get(bandName);
    }

    /**
     * Sets the geolocation dataset names.
     *
     * @param lat the name of the dataset providing the latitude values, must not be null
     * @param lon the name of the dataset providing the longitude values, must not be null
     */
    public void setGeolocationDatasetNames(String lat, String lon) {
        Guardian.assertNotNullOrEmpty("lat", lat);
        Guardian.assertNotNullOrEmpty("lon", lon);
        setGeolocationDatasetNames(new String[]{lat, lon});
    }

    /**
     * Sets the geolocation dataset names.
     *
     * @param geolocationDatasetNames if not null <code>geolocationDatasetNames[0]</code> is the name of the dataset providing the latitude values
     *                                <code>geolocationDatasetNames[1]</code> is the name of the dataset providing the longitude values
     */
    public void setGeolocationDatasetNames(String[] geolocationDatasetNames) {
        Guardian.assertNotNull("geolocationDatasetNames", geolocationDatasetNames);
        if (geolocationDatasetNames.length != 2) {
            throw new IllegalArgumentException("geolocationDatasetNames.length != 2");
        }
        Guardian.assertNotNull("geolocationDatasetNames[0]", geolocationDatasetNames[0]);
        Guardian.assertNotNull("geolocationDatasetNames[1]", geolocationDatasetNames[1]);
        _geolocationDatasetNames = geolocationDatasetNames;
    }

    /**
     * Gets the geolocation dataset names.
     *
     * @return the geolocation dataset names. Can be null. If not null <code>geolocationDatasetNames[0]</code> is the name of the dataset providing the latitude values
     *         <code>geolocationDatasetNames[1]</code> is the name of the dataset providing the longitude values
     */
    public String[] getGeolocationDatasetNames() {
        return _geolocationDatasetNames;
    }

    /**
     * Sets the pattern string for the external geolocation.
     *
     * @param externalGeolocationPattern the pattern for the external geolocation, can be null
     */
    public void setExternalGeolocationPattern(String externalGeolocationPattern) {
        Debug.trace("ModisProductDescription.externalGeolocationPattern = " + externalGeolocationPattern);
        _externalGeolocationPattern = externalGeolocationPattern;
    }

    /**
     * Gets the pattern string for the external geolocation.
     *
     * @return the pattern for the external geolocation or null
     */
    public String getExternalGeolocationPattern() {
        return _externalGeolocationPattern;
    }

    /**
     * Retrieves whether the geolocation for this product is stored in an external file - or not
     *
     * @return true, if so
     */
    public boolean hasExternalGeolocation() {
        return _externalGeolocationPattern != null /*&& _geolocationDatasetNames == null*/;
    }

    /**
     * Adds a tie point grid to the product description
     *
     * @param desc
     */
    void addTiePointGrid(final ModisTiePointDescription desc) {
        _tiePoints.put(desc.getName(), desc);
        _tiePointVec.add(desc);
    }

    /**
     * Retrieves the names of the tie point grids of the product.
     *
     * @return the names of the tie point grids.
     */
    public String[] getTiePointNames() {
        String[] strRet = new String[_tiePointVec.size()];

        for (int n = 0; n < _tiePointVec.size(); n++) {
            strRet[n] = ((ModisTiePointDescription) _tiePointVec.get(n)).getName();
        }
        return strRet;
    }

    /**
     * Retrieves the tie point description for the given tie point name
     *
     * @param name
     *
     * @return the description of the tie point.
     */
    public ModisTiePointDescription getTiePointDescription(String name) {
        return (ModisTiePointDescription) _tiePoints.get(name);
    }

    /**
     * Sets whether to flip top down the product or not
     *
     * @param bFlip
     */
    void setTopDownFlip(boolean bFlip) {
        _flipTopDown = bFlip;
    }

    /**
     * Retrieves whether the product has to be flipped top down or not
     *
     * @return <code>true</code> if the product has to be flipped top down, otherwise <code>false</code>.
     */
    public boolean mustFlipTopDown() {
        return _flipTopDown;
    }
}
