/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

/**
 * Represents the period in which pixel data can contribute to a bin.
 *
 * @author Norman Fomferra
 */
public interface DataPeriod {

    enum Membership {
        PREVIOUS_PERIODS(-1),
        CURRENT_PERIOD(0),
        SUBSEQUENT_PERIODS(+1);

        private final int value;

        Membership(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Compute the membership of a given longitude-time pair to this spatial data-period.
     * The result may be one of
     * <ul>
     * <li><code>0</code> - the longitude-time pair belongs to the current period (given by time and time+duration)</li>
     * <li><code>-1</code> - the longitude-time pair belongs to a previous period</li>
     * <li><code>+1</code> - the longitude-time pair belongs to a following period</li>
     * </ul>
     *
     * @param lon  The longitude in range -180 to 180 degrees.
     * @param time The time in days using Modified Julian Day units
     *
     * @return The membership.
     */
    Membership getObservationMembership(double lon, double time);
}
