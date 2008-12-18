/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Band info registry.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class BandInfoRegistry {

    private static final BandInfoRegistry uniqueInstance = new BandInfoRegistry();

    private final ConcurrentMap<String, BandInfo> bandInfoMap;

    private BandInfoRegistry() {
        bandInfoMap = new ConcurrentHashMap<String, BandInfo>(17);

        registerBandInfo("Flags", "", 0.0, 1.0, -1, 0, 1 << 16,
                         "L1c flags applicable to the pixel for this " +
                                 "particular integration time.");
        registerBandInfo("BT_Value", "K", 0.0, 1.0, -999.0, 50.0, 350.0,
                         "Brightness temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("BT_Value_Real", "K", 0.0, 1.0, -999.0, 50.0, 350.0,
                         "Real component of HH, HV or VV polarisation brightness " +
                                 "temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("BT_Value_Imag", "K", 0.0, 1.0, -999.0, 50.0, 350.0,
                         "Imaginary component of HH, HV or VV polarisation brightness " +
                                 "temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("Pixel_Radiometric_Accuracy", "K", 0.0, 50.0 / (1 << 16), -999.0, 0.0, 5.0,
                         "Error accuracy measurement in the Brightness " +
                                 "Temperature presented in the previous field, " +
                                 "extracted in the direction of the pixel.");
        registerBandInfo("Incidence_Angle", "deg", 0.0, 90.0 / (1 << 16), -999.0, 0.0, 90.0,
                         "Incidence angle value corresponding to the " +
                                 "measured BT value over current Earth fixed " +
                                 "grid point. Measured as angle from pixel to " +
                                 "S/C with respect to the pixel local normal (0º " +
                                 "if vertical)");
        registerBandInfo("Azimuth_Angle", "deg", 0.0, 360.0 / (1 << 16), -999.0, 0.0, 360.0,
                         "Azimuth angle value corresponding to the " +
                                 "measured BT value over current Earth fixed " +
                                 "grid point. Measured as angle in pixel local " +
                                 "tangent plane from projected pixel to S/C " +
                                 "direction with respect to the local North (0º if" +
                                 "local North)");
        registerBandInfo("Faraday_Rotation_Angle", "deg", 0.0, 360.0 / (1 << 16), -999.0, 0.0, 360.0,
                         "Faraday rotation angle value corresponding " +
                                 "to the measured BT value over current Earth " +
                                 "fixed grid point. It is computed as the rotation " +
                                 "from antenna to surface (i.e. inverse angle)");
        registerBandInfo("Geometric_Rotation_Angle", "deg", 0.0, 360.0 / (1 << 16), -999.0, 0.0, 360.0,
                         "Geometric rotation angle value " +
                                 "corresponding to the measured BT value " +
                                 "over current Earth fixed grid point. It is " +
                                 "computed as the rotation from surface to " +
                                 "antenna (i.e. direct angle).");
        registerBandInfo("Footprint_Axis1", "km", 0.0, 100.0 / (1 << 16), -999.0, 20.0, 30.0,
                         "Elliptical footprint major semi-axis value.");
        registerBandInfo("Footprint_Axis2", "km", 0.0, 100.0 / (1 << 16), -999.0, 20.0, 30.0,
                         "Elliptical footprint minor semi-axis value.");

        // todo - band info for level 2 (rq-20081208)
    }

    static BandInfoRegistry getInstance() {
        return uniqueInstance;
    }

    BandInfo getBandInfo(String name) {
        return bandInfoMap.get(name);
    }

    private void registerBandInfo(String name, String unit,
                                  double scaleOffset,
                                  double scaleFactor,
                                  Number noDataValue,
                                  double min,
                                  double max,
                                  String description) {
        registerBandInfo(name, new BandInfo(name, unit, scaleOffset, scaleFactor, noDataValue, min, max, description));
    }

    private void registerBandInfo(String name, BandInfo bandInfo) {
        bandInfoMap.putIfAbsent(name, bandInfo);
    }
}
