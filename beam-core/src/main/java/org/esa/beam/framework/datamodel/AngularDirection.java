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
package org.esa.beam.framework.datamodel;

public class AngularDirection {

    public double zenith;
    public double azimuth;
    private static final double EPS = 1.0e-10;

    public AngularDirection() {
    }

    public AngularDirection(double azimuth, double zenith) {
        this.azimuth = azimuth;
        this.zenith = zenith;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AngularDirection) {
            AngularDirection other = (AngularDirection) obj;
            return Math.abs(other.azimuth - azimuth) < EPS &&
                   Math.abs(other.zenith - zenith) < EPS;
        }
        return false;
    }

    @Override
    public String toString() {
        return "AngularDirection[" + zenith + "," + azimuth + "]";
    }
}
