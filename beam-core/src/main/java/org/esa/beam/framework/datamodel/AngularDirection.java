/*
 * $Id: AngularDirection.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
