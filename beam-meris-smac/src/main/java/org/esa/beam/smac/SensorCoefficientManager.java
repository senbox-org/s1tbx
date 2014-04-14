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
package org.esa.beam.smac;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the mapping between the satellite sensor bands and the appropriate SMAC coefficient sets.
 * <p/>
 * This mapping is configurable from the outside. The class <code>SensorCoefficientManager</code> expects an URL
 * pointing to a directoy where the mapping file "SensorMap.txt" and the appropriate coefficient files are located.
 * <p/>
 * The syntax for the map file is very simple, it is a separated ascii (separation character is '|') table with the
 * following entries:
 * <p/>
 * SENSOR_NAME | BAND_NAME | ATMOSPHERE_TYPE | COEFFICIENT_FILE
 * <p/>
 * SENSOR_NAME - is defined by the public fields xxx_NAME where xxx denotes the satellite sensor BAND_NAME - must match
 * the band names defined in <code>BeamConstants</code> ATMOSPHERE_TYPE - dependent on the coefficient set - this
 * string will be seen in the SMAC UI COEFFICIENT_FILE - name of the coefficient file
 *
 */
public class SensorCoefficientManager {

    public static final String AER_DES_NAME = "DES";
    public static final String AER_CONT_NAME = "CONT";
    public static final String MERIS_NAME = "MERIS";
    public static final String AATSR_NAME = "AATSR";

    private static final char[] fieldSeparators = {'|'};
    private static final String mapFileName = "SensorMap.txt";

    private List<SensorDb> sensors;
    private String locationPath;
    private Map<AEROSOL_TYPE, String> aerosolTypeMap;

    /**
     * Constructs the object with default parameters.
     */
    public SensorCoefficientManager() {
        init();
    }

    /**
     * Constructs the object with a given location for the sensor coefficient files. Scans this URL for valid files.
     *
     * @param location the URL where the sensor coefficient files reside
     * @throws java.io.IOException when unable to access <code>URL</code>
     */
    public SensorCoefficientManager(URL location) throws IOException {
        Guardian.assertNotNull("location", location);

        init();
        setURL(location);
    }

    /**
     * Sets the URL where the sensor coefficient files reside. Scans this URL for valid files.
     *
     * @param location the URL where the coefficients reside
     * @throws java.io.IOException when unable to access <code>URL</code>
     */
    public void setURL(URL location) throws IOException {
        Guardian.assertNotNull("location", location);
        URL mapFileURL;

        String file = null;
        try {
            file = SystemUtils.convertToLocalPath(location.getPath() + "/" + mapFileName);
            mapFileURL = new URL(location.getProtocol(), location.getHost(), file);
        } catch (MalformedURLException e) {
            throw new IOException("Unable to open coefficient map file from URL '" + file + "'", e);
        }

        setLocationPath(location);

        try (InputStream in = mapFileURL.openStream()) {
            final CsvReader csvReader = new CsvReader(new InputStreamReader(in),
                                                      fieldSeparators);

            // read sensor map file completely and assemble the database
            // ---------------------------------------------------------
            String[] record;
            SensorDb sensorDb;
            BandDb bandDb;
            while ((record = csvReader.readRecord()) != null) {
                // retrieve the sensor db entry
                sensorDb = getSensorDb(record[0]);
                if (sensorDb == null) {
                    // is not in database yet, create and add to database
                    sensorDb = new SensorDb(record[0]);
                    sensors.add(sensorDb);
                }

                // add the band entry to sensorDb
                bandDb = new BandDb(record[1], record[2], record[3]);
                sensorDb.addBand(bandDb);
            }
        }
    }

    /**
     * Returns the sensor coefficient file URL for a given sensor and bandName name. Or <code>null</code> when the sensor or
     * bandName are not in the database.
     *  @param sensor      the sensor name
     * @param bandName        the bandName name
     * @param aerosolType the aerosol type
     */
    public URL getCoefficientFile(String sensor, String bandName, AEROSOL_TYPE aerosolType) {
        URL url = null;
        SensorDb sensorDb;

        sensorDb = getSensorDb(sensor);
        if (sensorDb != null) {
            BandDb bandDb;

            bandDb = sensorDb.getBand(bandName, aerosolTypeMap.get(aerosolType));
            if (bandDb == null) {
                return null;
            }

            try {
                url = new URL(bandDb.getCoefficientFileName());
            } catch (MalformedURLException e) {
                try {
                    url = new URL(locationPath + bandDb.getCoefficientFileName());
                } catch (MalformedURLException ignored) {
                }
            }
        }

        return url;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the object.
     */
    private void init() {
        sensors = new ArrayList<>();
        aerosolTypeMap = new HashMap<>();
        aerosolTypeMap.put(AEROSOL_TYPE.CONTINENTAL, AER_CONT_NAME);
        aerosolTypeMap.put(AEROSOL_TYPE.DESERT, AER_DES_NAME);
    }

    /**
     * Retrieves the sensorDb class for the given sensor name or <code>null</code> when no sensor of the name is in the
     * database.
     *
     * @param name the sensor name
     */
    private SensorDb getSensorDb(String name) {
        SensorDb sensorDb = null;
        SensorDb current;

        for (SensorDb sensor : sensors) {
            current = sensor;
            if (ObjectUtils.equalObjects(name, current.getName())) {
                sensorDb = current;
            }
        }

        return sensorDb;
    }

    /**
     * Sets the location path for the current URL
     *
     * @param location the URL
     */
    private void setLocationPath(URL location) {
        locationPath = location.toExternalForm();
    }

    /**
     * Class holding the coefficient file information for one satellite sensor.
     */
    private class SensorDb {

        private String name;
        private List<BandDb> bands;

        /**
         * Constructs the class with given sensor name
         *
         * @param name the sensor name
         */
        SensorDb(String name) {
            init();
            setName(name);
        }

        /**
         * Retrieves the name of the sensor.
         */
        String getName() {
            return name;
        }

        /**
         * Sets the name of the sensor.
         *
         * @param name the sensor name
         */
        void setName(String name) {
            Guardian.assertNotNull("name", name);
            this.name = name;
        }

        /**
         * Adds an band to the sensor.
         *
         * @param band the BandDb entry to be added
         */
        void addBand(BandDb band) {
            Guardian.assertNotNull("bandDb", band);
            bands.add(band);
        }

        /**
         * Retrieves the dabase entry for given band name and aerosol type.
         *
         * @param name        the band name
         * @param aerosolType the aerosol type
         */
        public BandDb getBand(String name, String aerosolType) {
            BandDb ret = null;
            BandDb current;

            for (BandDb band : bands) {
                current = band;

                if (ObjectUtils.equalObjects(name, current.getBandName())
                    && ObjectUtils.equalObjects(aerosolType, current.getAerosolType())) {
                    ret = current;
                    break;
                }
            }

            return ret;
        }

        ///////////////////////////////////////////////////////////////////////
        /////// END OF PUBLIC
        ///////////////////////////////////////////////////////////////////////

        /**
         * Initializes the object.
         */
        private void init() {
            bands = new ArrayList<>();
        }
    }

    /**
     * Class holding the band information of a specific sensor coefficient set.
     */
    private class BandDb {

        String bandName;
        String aerosolType;
        String coeffFileName;

        /**
         * Creates the object with given band name, aerosol tape and coefficient file name
         */
        BandDb(String bandName, String aerosolType, String coefficientFileName) {
            setBandName(bandName);
            setAerosolType(aerosolType);
            setCoefficientFileName(coefficientFileName);
        }

        /**
         * Sets the band name for this object.
         *
         * @param bandName the band name
         */
        void setBandName(String bandName) {
            Guardian.assertNotNull("bandName", bandName);
            this.bandName = bandName;
        }

        /**
         * Retrieves the band name of the object.
         */
        String getBandName() {
            return bandName;
        }

        /**
         * Sets the aerosol type for this object.
         *
         * @param aerosolType the aerosol type
         */
        void setAerosolType(String aerosolType) {
            Guardian.assertNotNull("aerosolType", aerosolType);
            this.aerosolType = aerosolType;
        }

        /**
         * Retrieves the aerosol type for this object.
         */
        String getAerosolType() {
            return aerosolType;
        }

        /**
         * Sets the coefficient file name for thic object.
         *
         * @param coeffFile the coefficient file name
         */
        void setCoefficientFileName(String coeffFile) {
            Guardian.assertNotNull("coeffFile", coeffFile);
            coeffFileName = coeffFile;
        }

        /**
         * Retrieves the sensor coefficient file name set in this object.
         */
        String getCoefficientFileName() {
            return coeffFileName;
        }
    }
}
