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

package org.esa.beam.dataio.chris.internal;

/**
 * Class representing the angular position of the Sun.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SunPosition {

    private double aa;
    private double za;

    public SunPosition(final double za, final double aa) {
        if (aa < 0.0) {
            throw new IllegalArgumentException("aa < 0.0");
        }
        if (aa > 360.0) {
            throw new IllegalArgumentException("aa > 360.0");
        }
        if (za < 0.0) {
            throw new IllegalArgumentException("za < 0.0");
        }
        if (za > 180.0) {
            throw new IllegalArgumentException("za > 180.0");
        }

        this.aa = aa;
        this.za = za;
    }

    public final double getAzimuthAngle() {
        return aa;
    }

    public final double getZenithAngle() {
        return za;
    }
}
