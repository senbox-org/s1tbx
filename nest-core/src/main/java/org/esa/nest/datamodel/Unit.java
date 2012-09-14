/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.datamodel;

import org.esa.beam.framework.datamodel.Band;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Nov 27, 2008
 * Time: 3:25:19 PM
 * To change this template use File | Settings | File Templates.
 */
public final class Unit {

    public static final String AMPLITUDE = "amplitude";
    public static final String INTENSITY = "intensity";
    public static final String PHASE = "phase";
    public static final String ABS_PHASE = "abs_phase";
    public static final String COHERENCE = "coherence";

    public static final String REAL = "real";
    public static final String IMAGINARY = "imaginary";

    public static final String DB = "db";

    public static final String AMPLITUDE_DB = AMPLITUDE+'_'+DB;
    public static final String INTENSITY_DB = INTENSITY+'_'+DB;

    public static final String METERS = "meters";

    public enum UnitType { AMPLITUDE, INTENSITY, REAL, IMAGINARY, PHASE, ABS_PHASE, COHERENCE, INTENSITY_DB, AMPLITUDE_DB, METERS, UNKNOWN }

    public static UnitType getUnitType(Band sourceBand) {

        if(sourceBand.getUnit() == null)
            return UnitType.UNKNOWN;
        final String  unit =  sourceBand.getUnit().toLowerCase();
        if (unit.contains(AMPLITUDE)) {
            if (unit.contains(DB))
                return UnitType.AMPLITUDE_DB;
            else
                return UnitType.AMPLITUDE;
        } else if (unit.contains(INTENSITY)) {
            if (unit.contains(DB))
                return UnitType.INTENSITY_DB;
            else
                return UnitType.INTENSITY;
        } else if (unit.contains(PHASE)) {
            return UnitType.PHASE;
        } else if (unit.contains(ABS_PHASE)) {
            return UnitType.ABS_PHASE;
        } else if (unit.contains(REAL)) {
             return UnitType.REAL;
        } else if (unit.contains(IMAGINARY)) {
             return UnitType.IMAGINARY;
        } else if (unit.contains(METERS)) {
             return UnitType.METERS;
        } else if (unit.contains(COHERENCE)) {
            return UnitType.COHERENCE;
        } else {
            return UnitType.UNKNOWN;
        }
    }

    // tiepoint grid units

    public static final String DEGREES = "deg";
    public static final String NANOSECONDS = "ns";

    // temporary unit, should be removed later and use bit mask
    public static final String BIT = "bit";
}
