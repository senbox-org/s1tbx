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
package org.esa.snap.core.util.math;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.Guardian;

/**
 * A utility class providing a set of mathematical functions frequently used in the fields of remote sensing.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public class RsMathUtils {

    /**
     * The specific weight of air in kg / m^3 It's value is <code>1.26895</code>.
     */
    public static final double SPEC_WEIGHT_AIR = 1.26895;
    // @todo 1 tb/tb - found : 1.29, 1.2928, 1,293, 1.2 for this constant, someone has to verify!
    // @todo 1 tb/tb - summed all found values and processed arithmetic mean

    /**
     * The earth's gravity acceleration in  m / s^2. It's value is <code>9.80665</code>.
     */
    public static final double GRAVITY_ACC = 9.80665;

    /**
     * A constant used for the barometric formula - normalize to input in hPa. It's value is <code>GRAVITY_ACC *
     * SPEC_WEIGHT_AIR * 1e-2</code>.
     */
    public static final double BAROMETRIC_CONST = GRAVITY_ACC * SPEC_WEIGHT_AIR * 1e-2;

    /**
     * The constant in Koschmieder's Formula. It's value is <code>3.92 * 2</code>.
     */
    public static final float KOSCHMIEDER_CONST = 3.92F * 2;

    /**
     * A constant used for degree/radian conversions. It's value is <code>Math.PI / 180.0</code>.
     */
    public static final double RAD_PER_DEG = Math.PI / 180.0;

    /**
     * A constant used for radian/degree conversions. It's value is <code>180.0 / Math.PI</code>.
     */
    public static final double DEG_PER_RAD = 180.0 / Math.PI;

    /**
     * The Earth's mean radius in meters.
     */
    public final static double MEAN_EARTH_RADIUS = 6370997; // meter

    /**
     * Converts a reflectance value to a radiance value for given solar spectral flux and sun zenith angle.
     * This is the inverse of <code>radianceToReflectance</code>.
     *
     * @param refl the reflectance
     * @param sza the sun zenith angle in decimal degrees
     * @param e0  the solar spectral flux in mW / (m^2 * sr * nm)
     * @return the corresponding radiance in mW /(m^2 * sr * nm)
     */
    public static float reflectanceToRadiance(float refl, float sza, float e0) {
        return (float) (refl * e0 * Math.cos(sza * RAD_PER_DEG) / Math.PI);
    }

    /**
     * Converts a radiance value to a reflectance value for given solar spectral flux and sun zenith angle.
     *
     * @param rad the radiance in mW /(m^2 * sr * nm)
     * @param sza the sun zenith angle in decimal degrees
     * @param e0  the solar spectral flux in mW / (m^2 * sr * nm)
     * @return the corresponding reflectance
     */
    public static float radianceToReflectance(float rad, float sza, float e0) {
        return (float) ((rad * Math.PI) / (e0 * Math.cos(sza * RAD_PER_DEG)));
    }

    /**
     * Converts an array of radiance values to an array of reflectance values for given solar spectral flux and sun
     * zenith angle.
     * <p>
     * If the <code>recycle</code> parameter is of the same size as the radiance array, this array will be filled with
     * the results and returned. The returned result may contain reflectances &lt;0 or &gt;1.
     * <p>
     * The method performs no plausability check on the conversion.
     *
     * @param rad     the radiances in mW /(m^2 * sr * nm)
     * @param sza     the sun zenith angle in decimal degrees
     * @param e0      the solar spectral flux
     * @param recycle optional array which will be filled with the results, can be null.
     * @return the array of corresponding reflectance
     */
    public static float[] radianceToReflectance(float[] rad, float[] sza, float e0, float[] recycle) {
        Guardian.assertNotNull("rad", rad);
        Guardian.assertNotNull("sza", sza);
        float[] fRet = createOrRecycleArray(recycle, Math.min(rad.length, sza.length));
        for (int i = 0; i < rad.length; i++) {
            fRet[i] = radianceToReflectance(rad[i], sza[i], e0);
        }
        return fRet;
    }

    /**
     * Converts a zenith angle to an elevation angle.
     *
     * @param zenith the zenith angle in decimal degrees
     */
    public static float zenithToElevation(float zenith) {
        return 90.0F - zenith;
    }

    /**
     * Converts a vector of zenith angles to a vector of  elevation angles. If recycle is not <code>null</code> and has
     * the same size as the zenith angle vector, recycle will be filled and returned to minimize the memory
     * finmgerprint.
     *
     * @param zenith  the zenith angle vector in decimal degrees
     * @param recycle optional array which will be filled with the results. can be null.
     */
    public static float[] zenithToElevation(float[] zenith, float[] recycle) {
        Guardian.assertNotNull("zenith", zenith);

        // check if we can use the recycle argument to save memory
        // -------------------------------------------------------
        float[] fRet = createOrRecycleArray(recycle, zenith.length);

        // loop over vector
        // ----------------
        for (int n = 0; n < zenith.length; n++) {
            fRet[n] = 90.0F - zenith[n];
        }

        return fRet;
    }

    /**
     * Converts an elevation angle to a zenith angle. Convenience routine. Does the same as zenithToElevation but for
     * code clarity ...
     *
     * @param elevation the lelevation angle in decimal degrees
     */
    public static float elevationToZenith(float elevation) {
        return 90.0F - elevation;
    }

    /**
     * Converts a vector of elevation angles to a vector of zenith angles. If recycle is not <code>null</code> and has
     * the same size as the elevation angle vector, recycle will be filled and returned to minimize the memory
     * fingerprint. Convenience routine. Does the same as zenithToElevation but for code clarity ...
     *
     * @param elevation a vector of elevation angles (in degree)
     * @param recycle   optional array which will be filled with the results. can be null.
     */
    public static float[] elevationToZenith(float[] elevation, float[] recycle) {
        Guardian.assertNotNull("elevation", elevation);

        // check if we can use the recycle argument to save memory
        // -------------------------------------------------------
        float[] fRet = createOrRecycleArray(recycle, elevation.length);

        // loop over vector
        // ----------------
        for (int n = 0; n < elevation.length; n++) {
            fRet[n] = 90.0F - elevation[n];
        }

        return fRet;
    }

    /**
     * Calculates the air pressure in a given height given the sea level air pressure. Simple version with no dependency
     * on temperature etc...
     *
     * @param seaPress the sea level air pressure in hPa
     * @param height   the height above sea level in m
     */
    public static float simpleBarometric(float seaPress, float height) {
        return seaPress * (float) Math.exp(-BAROMETRIC_CONST * (double) height / (double) seaPress);
    }

    /**
     * Calculates the air pressure in a given height given the sea level air pressure. Simple version with no dependency
     * on temperature etc... This method processes on vectors. If recycle is not <code>null</code> and has the same size
     * as the sea pressure vector, recycle will be filled and returned to minimize the memory fingerprint.
     *
     * @param seaPress vector of sea level air pressure in hPa
     * @param height   vector of height above sea level in m
     * @param recycle  optional array which will be filled with the results. can be null.
     */
    public static float[] simpleBarometric(float[] seaPress, float[] height, float[] recycle) {
        Guardian.assertNotNull("seaPress", seaPress);
        Guardian.assertNotNull("height", height);

        // check if we can use the recycle argument to save memory
        // -------------------------------------------------------
        float[] fRet = createOrRecycleArray(recycle, Math.min(seaPress.length, height.length));

        // loop over vector
        // ----------------
        for (int n = 0; n < seaPress.length; n++) {
            fRet[n] = seaPress[n] * (float) Math.exp(-BAROMETRIC_CONST * (double) height[n] / (double) seaPress[n]);
        }

        return fRet;
    }

    /**
     * Transforms horizontal visibility(km) to aerosol optical depth according to Koschmieder's formula.
     *
     * @param visibility horizontal visibility in km
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given parameter is 0.f
     */
    public static float koschmieder(float visibility) {
        return computeKoschmieder(visibility);
    }

    /**
     * Transforms aerosol optical depth to horizontal visibility(km) according to Koschmieder's formula.
     *
     * @param opticalDepth aerosol optical depth
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given parameter is 0.f
     */
    public static float koschmiederInv(float opticalDepth) {
        return computeKoschmieder(opticalDepth);
    }

    private static float computeKoschmieder(float value) {
        if (value == 0.0F) {
            throw new IllegalArgumentException("value is zero");
        }
        return KOSCHMIEDER_CONST / value;
    }

    private static float[] createOrRecycleArray(float[] recycle, int requiredSize) {
        if (recycle != null && recycle.length == requiredSize) {
            return recycle;
        } else {
            return new float[requiredSize];
        }
    }

    /**
     * Applies a geodetic correction to the given geographical coordinate.
     * <p>The implementation assumes that the Earth is flat at the given coordinate point at elevetion zero and at the
     * given elevation. Furthermore the earth is assumed to be a sphere with the fixed radius {@link RsMathUtils#MEAN_EARTH_RADIUS}.
     *
     * @param gp the geographical coordinate to be corrected and which will be corrected
     * @param h elevation above the Earth's sphere or ellipsoid surface in meters
     * @param vz satellite viewing zenith angle in degree
     * @param va satellite viewing azimuth angle in degree
     */
    public static void applyGeodeticCorrection(final GeoPos gp,
                                               final double h,
                                               final double vz,
                                               final double va) {
        final double dx = h * Math.tan(RAD_PER_DEG * vz);
        final double dlat = DEG_PER_RAD * dx * Math.cos(RAD_PER_DEG * va) /
                            (MEAN_EARTH_RADIUS);
        final double dlon = DEG_PER_RAD * dx * Math.sin(RAD_PER_DEG * va) /
                            (MEAN_EARTH_RADIUS * Math.cos(RAD_PER_DEG * gp.lat));
        gp.lat += dlat;
        gp.lon += dlon;
    }
}
