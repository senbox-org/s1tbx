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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * The class <code>LandsatConstants</code> is used to store
 * all constant information for all Landsat TM products
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public class LandsatConstants {

    public static final Class[] INPUT_TYPES = new Class[]{
            String.class,
            File.class
    };

    public static final int FAST_L5 = 0;
    public static final int CEOS = 3;
    public static final int ZIPED_UNKNOWN_FORMAT = 1;
    public static final int INVALID_FORMAT = -1;

    public static final String [] LANDSAT_EXTENSIONS = {".dat", ".zip"};
    public static final String DESCRIPTION = "Landsat 5 TM Product Reader";
    public static final String [] FILE_NAMES = {"FAST L5", "LANDSAT ZIP"};

    public static final String [] SATELLITE_NAMES = {"L5"};
    public static final String TM_INSTRUMENT = "TM";
    public static final int LANDSAT_5 = 0;

    /**
     * holds all constant information for a Landsat 5 TM Image
     */
    public static final class ConstBand {

        private final String bandName;
        private final int bandIndex;
        private final float wavelength;
        private final float bandwidth;
        private final int resolution;
        private final String description;
        private final float solarFluxValue;
        private static final List<ConstBand> allBands = new Vector<ConstBand>();

        static {
            allBands.add(new ConstBand("Band1", 1, 490, 66, 30, "Blue", 1957));
            allBands.add(new ConstBand("Band2", 2, 560, 82, 30, "Green", 1826));
            allBands.add(new ConstBand("Band3", 3, 660, 67, 30, "Red", 1554));
            allBands.add(new ConstBand("Band4", 4, 830, 128, 30, "NearIR", 1036));
            allBands.add(new ConstBand("Band5", 5, 1670, 217, 30, "MidIR", 215));
            allBands.add(new ConstBand("Band6", 6, 11500, 1000, 120, "ThermalIR", 0));
            allBands.add(new ConstBand("Band7", 7, 2240, 252, 30, "MidIR", 80.67f));
        }


        private ConstBand(final String bandName, final int index, final float wavelength, final float bandwidth,
                          final int resolution, final String description,
                          final float solarFlux) {
            this.bandName = bandName;
            this.bandIndex = index;
            this.wavelength = wavelength;
            this.bandwidth = bandwidth;
            this.resolution = resolution;
            this.description = description;
            this.solarFluxValue = solarFlux;
        }

        @Override
        public String toString() {
            return this.bandName;
        }

        /**
         * @return ConstantBand by comparing the band description with the constant bands
         */
        static public ConstBand getConstBand(final Band band) {

            for (Iterator<ConstBand> iter = allBands.iterator(); iter.hasNext();) {
                ConstBand element = iter.next();

                // check if a band can be identified as a const landsat band

                if (band.getSpectralBandIndex() == element.getBandIndex()) {
                    return element;
                }
            }

            return null;
        }

        /**
         * @return the bandwith of the image band
         */
        public float getBandwidth() {
            return bandwidth;
        }

        /**
         * expresses the physical preference of this band
         *
         * @return a short description expression
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return resolution of the band in meter
         */
        public int getResolution() {
            return resolution;
        }

        /**
         * @return wavelength of the band in nm
         */
        public float getWavelength() {
            return wavelength;
        }

        /**
         * @return the index of the band in the LANDSAT TM product
         */
        public int getBandIndex() {
            return bandIndex;
        }

        /**
         * @return the solar flux of the band (TM Solar Exoatmosheric spectral irradiances)
         */
        public float getSolarFlux() {
            return this.solarFluxValue;
        }

        /**
         * @param index of the band in the LANDSAT TM product
         *
         * @return the band object at a given index. Is <code>null</code> if there could not be found an element at the given index
         */
        public static ConstBand getConstantBandAt(final int index) {
            for (Iterator<ConstBand> iter = allBands.iterator(); iter.hasNext();) {
                ConstBand element = iter.next();
                if (index == element.getBandIndex()) {
                    return element;
                }
            }
            Debug.trace("no band found at index: " + index);
            return null;
        }

    }

    public static final class NomRadiance {


        public final static int AFTER_5_MAY_2003 = 0;
        public final static int BEFORE_5_MAY_2003 = 1;

        private final double lmin;
        private final double lmax;
        private double _gain;
        private final int _bandNumber;

        static private final List<NomRadiance> after2003Radiances = new Vector<NomRadiance>();

        static {
            after2003Radiances.add(new NomRadiance(1, -1.5, 193, 0.762824));
            after2003Radiances.add(new NomRadiance(2, -2.84, 365, 1.442510));
            after2003Radiances.add(new NomRadiance(3, -1.17, 264, 1.039880));
            after2003Radiances.add(new NomRadiance(4, -1.51, 221, 0.872510));
            after2003Radiances.add(new NomRadiance(5, -0.37, 30.2, 0.119882));
            after2003Radiances.add(new NomRadiance(6, 1.2378, 15.303, 0.055158));
            after2003Radiances.add(new NomRadiance(7, -0.15, 16.5, 0.065294));
        }

        static private final List<NomRadiance> before2003Radiances = new Vector<NomRadiance>();

        static {
            before2003Radiances.add(new NomRadiance(1, -1.5, 152.10, 0.602431));
            before2003Radiances.add(new NomRadiance(2, -2.84, 296.81, 1.175100));
            before2003Radiances.add(new NomRadiance(3, -1.17, 204.30, 0.805765));
            before2003Radiances.add(new NomRadiance(4, -1.51, 206.20, 0.814549));
            before2003Radiances.add(new NomRadiance(5, -0.37, 27.19, 0.108078));
            before2003Radiances.add(new NomRadiance(6, 1.2378, 15.303, 0.055158));
            before2003Radiances.add(new NomRadiance(7, -0.15, 14.38, 0.056980));
        }


        private NomRadiance(final int index, final double lmin, final double lmax,
                            final double gain) {
            this.lmin = lmin;
            this.lmax = lmax;
            this._gain = gain;
            this._bandNumber = index;
        }

        /**
         * @return a List of all nominal radiances normally used after 05.05.2003
         */
        public static List<NomRadiance> getAfter2003Radiances() {
            return after2003Radiances;
        }

        /**
         * @return the nominal gain value
         */
        public double getGain() {
            return _gain;
        }

        /**
         * @return the number of the band
         */
        public int getBandNumber() {
            return _bandNumber;
        }

        /**
         * @return the maximal nominal radiance value of the band
         */
        public double getLmax() {
            return lmax;
        }

        /**
         * @return the minimal nominal radiance value of the band
         */
        public double getLmin() {
            return lmin;
        }

        /**
         * @param index
         * @param date
         *
         * @return a collection of nominal radiance data
         */
        public static NomRadiance getConstantRadAt(final int index, final int date) {
            Guardian.assertTrue("date == AFTER_5_MAY_2003 || date == BEFORE_5_MAY_2003",
                                date == AFTER_5_MAY_2003 || date == BEFORE_5_MAY_2003);
            List<NomRadiance> radianceConstants;

            if (date == AFTER_5_MAY_2003) {
                radianceConstants = after2003Radiances;
            } else {
                radianceConstants = before2003Radiances;
            }

            for (Iterator<NomRadiance> iter = radianceConstants.iterator(); iter.hasNext();) {
                NomRadiance element = iter.next();
                if (index == element.getBandNumber()) {
                    return element;
                }
            }
            Debug.trace("no Radiance data found at index: " + index);
            return null;
        }

    }


    /**
     * @author cberwang
     */
    public static final class Thermal {

        private final double constant;
        private final String unit;
        private final String description;

        private Thermal(final double constant, final String unit, final String description) {
            this.unit = unit;
            this.constant = constant;
            this.description = description;
        }

        public static final Thermal K2 = new Thermal(1260.56, "Kelvin", "calibration constant");
        public static final Thermal K1 = new Thermal(607.76, "W/(m*m*sr*microm)", "calibration constant");

        /**
         * @return the thermal constant
         */
        public double getConstant() {
            return constant;
        }

        /**
         * @return the description string for the constant
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the unit string
         */
        public String getUnit() {
            return unit;
        }
    }

    /**
     * Geometric image point Constants
     * Indices for geometric point array
     */
    public static final class Points {

        private final String id;

        private Points(String anId) {
            this.id = anId;
        }

        @Override
        public String toString() {
            return this.id;
        }

        public static final Points UPPER_LEFT = new Points("Upper left");
        public static final Points LOWER_RIGHT = new Points("Lower right");
        public static final Points UPPER_RIGHT = new Points("Upper right");
        public static final Points LOWER_LEFT = new Points("Lower left");
        public static final Points CENTER = new Points("Center");
    }

    public static final class EarthSunDistance {

        private final int julianDay;
        private final double distance;
        final static private String UNIT = "Astronomical";
        private static final List<EarthSunDistance> distanceCollection = new Vector<EarthSunDistance>();

        static {
            distanceCollection.add(new EarthSunDistance(15, 0.9836));
            distanceCollection.add(new EarthSunDistance(32, 0.9853));
            distanceCollection.add(new EarthSunDistance(46, 0.9878));
            distanceCollection.add(new EarthSunDistance(60, 0.9909));
            distanceCollection.add(new EarthSunDistance(74, 0.9945));
            distanceCollection.add(new EarthSunDistance(91, 0.9993));
            distanceCollection.add(new EarthSunDistance(106, 1.0033));
            distanceCollection.add(new EarthSunDistance(121, 1.0076));
            distanceCollection.add(new EarthSunDistance(135, 1.0109));
            distanceCollection.add(new EarthSunDistance(152, 1.0140));
            distanceCollection.add(new EarthSunDistance(166, 1.0158));
            distanceCollection.add(new EarthSunDistance(182, 1.0167));
            distanceCollection.add(new EarthSunDistance(196, 1.0165));
            distanceCollection.add(new EarthSunDistance(213, 1.0149));
            distanceCollection.add(new EarthSunDistance(227, 1.0128));
            distanceCollection.add(new EarthSunDistance(242, 1.0092));
            distanceCollection.add(new EarthSunDistance(258, 1.0057));
            distanceCollection.add(new EarthSunDistance(274, 1.0011));
            distanceCollection.add(new EarthSunDistance(288, 0.9972));
            distanceCollection.add(new EarthSunDistance(305, 0.9925));
            distanceCollection.add(new EarthSunDistance(319, 0.9892));
            distanceCollection.add(new EarthSunDistance(335, 0.9860));
            distanceCollection.add(new EarthSunDistance(349, 0.9843));
            distanceCollection.add(new EarthSunDistance(365, 0.9833));
        }

        private EarthSunDistance(final int julianDay, final double distance) {
            this.julianDay = julianDay;
            this.distance = distance;
        }

        /**
         * @return the Unit of the earth distance value
         */
        public static String getUnit() {
            return UNIT;
        }

        public static double getEarthSunDistance(final int julianDay) {
            Guardian.assertGreaterThan("julianDay <= 0", julianDay, 0);
            Guardian.assertTrue("julianDay < 367", julianDay < 367);
            final EarthSunDistance firstElement = distanceCollection.get(0);
            double distance = firstElement.getDistance();
            for (Iterator<EarthSunDistance> iter = distanceCollection.iterator(); iter.hasNext();) {
                EarthSunDistance element = iter.next();
                if (element.getJulianDay() > julianDay) {
                    return distance;
                }
                distance = element.getDistance();
            }
            final EarthSunDistance lastElement = distanceCollection.get(
                    distanceCollection.size() - 1);
            return lastElement.getDistance();
        }

        private int getJulianDay() {
            return julianDay;
        }

        private double getDistance() {
            return distance;
        }
    }

    /**
     * Location Constants
     */
    public static final int POSITION_OF_SEPERATOR = 3;
    public static final String [] LOC_DESC = {"Path", "Row", "Fraction", "Subscence"};

    /**
     * Radiance constants
     */
    public static final int NOM_FORMER_MIN_RADIANCE = 4;
    public static final int NOM_FORMER_MAX_RADIANCE = 5;
    public static final int NOM_FORMER_GAIN = 6;
    public static final int NOM_NEWER_MIN_RADIANCE = 7;
    public static final int NOM_NEWER_MAX_RADIANCE = 8;
    public static final int NOM_NEWER_GAIN = 9;
    public static final int MIN_RADIANCE = 0;
    public static final int MAX_RADIANCE = 1;
    public static final int GAIN = 2;
    public static final int BIAS = 3;

    public static final String [] RADIANCE_DESCRIPTION_SHORT = {
            "MinRad",
            "MaxRad",
            "Gain",
            "Bias",
            "old NomMinRad",
            "old NomMaxRad",
            "old NomGain",
            "new NomMinRad",
            "new NomMaxRad",
            "new NomGain"
    };
    public static final String [] RADIANCE_DESCRIPTION = {
            "minimum radiance value",
            "maximum radiance value",
            "gain of the band",
            "bias of the band",
            "nominal minimum radiance value before 5 may 2003",
            "nominal maximal radiance value before 5 may 2003",
            "nominal gain radiance value before 5 may 2003",
            "nominal minimum radiance value after 5 may 2003",
            "nominal maximum radiance value after 5 may 2003",
            "nominal gain radiance value after 5 may 2003"
    };

    public static final String DATUM_SHORT = "Datum";
    public static final String DATUM_DESCRIPTION = "Ellipsoid used";

    public static final String SEMI_MAJ_SHORT = "Semi-Major-Axis";
    public static final String SEMI_MAJ_DESCRIPTION = "Semi-Major-Axis of earth ellipsoid";

    public static final String SEMI_MIN_SHORT = "Semi-Minor-Axis";
    public static final String SEMI_MIN_DESCRIPTION = "Semi-Major-Axis of earth ellipsoid";

    public static final String OFFSET_SHORT = "Horizontal offset ";
    public static final String OFFSET_DESCRIPTION = "Horizontal offset of the true";

    public static final String PROJECTION_SHORT = "Projection ";
    public static final String PROJECTION_DESCRIPTION = "Map projection Name";

    public static final String PROJECTION_ID_SHORT = "USG Projection";
    public static final String PROJECTION_ID_DESCRIPTION = "USG projection number";

    public static final String MAP_ZONE_SHORT = "Map zone";
    public static final String MAP_ZONE_DESCRIPTION = "USG map zone number";
    public static final String SUN_ELEV_SHORT = "Sun elevation";
    public static final String SUN_ELEV_DESCRIPTION = "Sun elevation angle at scene center";
    public static final String SUN_AZIMUTH_SHORT = "Sun azimuth";
    public static final String SUN_AZIMUTH_DESCRIPTION = "Sun azimuth at scene center";
    public static final String PARAMETER_SHORT = ". USG Projections parameter";
    public static final String PARAMETER_DESCRIPTION = "The USG projections parameters in standard USGS order. The meaning of these values depends on the projection used";
    public static final String PRODUCT_ID = "Product";
    public static final String PRODUCT_ID_DESCRIPTION = "Product ordernummer yydddnnn-cc";
    public static final String ACQUISITION_DATE = "Aquisition date";
    public static final String INSTRUMENT_MODE = "Instrument mode";
    public static final String INSTRUMENT_MODE_DESCRIPTION = "mode number";
    public static final String PRODUCT_TYPE = "Type of product";
    public static final String INSTRUMENT = "Instrument";
    public static final String PRODUCT_SIZE = "Product Size";
    public static final String RESAMPLING = "Resampling";
    public static final String TAPE_SPANNING = "Tape Spanning";
    public static final String TAPE_SPANNING_DESCRIPTION = "Tape volume number of volumes in tape set in 'n/m' format";
    public static final String START_LINE = "Startline";
    public static final String START_LINE_DESCRIPTION = "First image line number on this volume";
    public static final String LINES_PER_VOL = "Lines per Volumes";
    public static final String LINES_PER_VOL_DESCRIPTION = "Number of image lines on this volume ";
    public static final String ORIENTATION = "orientation angle";
    public static final String PIXEL_SIZE = "Pixel size";
    public static final String PIXEL_PER_LINES = "Number of pixels";
    public static final String PIXEL_PER_LINES_DESCRIPTION = "Number of pixels per image line";
    public static final String LINES_PER_IMAGE = "Number of lines";
    public static final String LINES_PER_IMAGE_DESCRIPTION = "Number of lines in the output image";
    public static final String BANDS_PRESENT = "Bands";
    public static final String BANDS_PRESENT_DESCRIPTION = "Bands present on the volume";
    public static final String BLOCKING_FACTOR = "Blocking factor";
    public static final String BLOCKING_FACTOR_DESCRIPTION = "Tape blocking factor";
    public static final String RECORD_LENGTH = "Record length";
    public static final String RECORD_LENGTH_DESCRIPTION = "Length of physical tape record";
    public static final String VERSION = "Version";
    public static final String VERSION_DESCRIPTION = "Format version code(A-Z)";


    public static final class geoPointsAttributes {

        private final String id;

        private geoPointsAttributes(String anId) {
            this.id = anId;
        }

        @Override
        public String toString() {
            return this.id;
        }

        public static final geoPointsAttributes LONGITUDE = new geoPointsAttributes("longitude");
        public static final geoPointsAttributes LATITUDE = new geoPointsAttributes("latitude");
        public static final geoPointsAttributes EASTING = new geoPointsAttributes("easting");
        public static final geoPointsAttributes NORTHING = new geoPointsAttributes("northing");
        public static final geoPointsAttributes CENTER_LINE = new geoPointsAttributes("center line");
        public static final geoPointsAttributes CENTER_PIXEL = new geoPointsAttributes("center pixel");
    }

    /**
     * mathematical constants
     */
    public static final int CONVERT_MINUTE_DEGREE = 60;
    public static final int CONVERT_SECOND_DEGREE = 3600;
    public static final double NULL_DATA_VALUE = 0.0;

    /**
     * unit constants
     */
    public static final class Unit {

        private final String unit;

        private Unit(final String unit) {
            this.unit = unit;
        }

        @Override
        public String toString() {
            return unit;
        }

        public static final Unit RADIANCE = new Unit("mW/(cm^2*sr*nm)");
        public static final Unit ANGLE = new Unit("deg.");
        public static final Unit METER = new Unit("m");
        public static final Unit PIXEL = new Unit("pixel");
        public static final Unit REFLECTANCE = new Unit("1000*Reflectance");
        public static final Unit KELVIN = new Unit("Kelvin");
    }

    public static final int FAST_FORMAT_HEADER_SIZE = 1536;
}
