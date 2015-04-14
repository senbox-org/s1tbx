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
 * @author Norman Fomferra
 */
public class TemporalDataPeriod implements DataPeriod {

    private static final double EPS = 1.0 / (60.0 * 60.0 * 1000); // 1 ms

    private final double startTime;
    private final double duration;

    /**
     * @param startTime Binning period's start time in MJD.
     * @param duration  The binning period's duration in days.
     */
    public TemporalDataPeriod(double startTime, double duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    @Override
    public Membership getObservationMembership(double lon, double time) {

        final double h = 24.0 * (time - startTime);

        if (h - EPS < 0) {
            // pixel is attached to data-periods (p-n)
            return Membership.PREVIOUS_PERIODS;
        } else if (h + EPS > 24.0 * duration) {
            // pixel is attached to data-periods (p+n)
            return Membership.SUBSEQUENT_PERIODS;
        } else {
            // pixel is attached to data-period (p)
            return Membership.CURRENT_PERIOD;
        }
    }
}
