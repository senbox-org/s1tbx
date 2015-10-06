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

package org.esa.snap.unmixing;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.gpf.annotations.Parameter;

public class Endmember {
    @Parameter(pattern = "[a-zA-Z_0-9]*")
    private String name;
    @Parameter
    private double[] wavelengths;
    @Parameter
    private double[] radiations;

    public Endmember() {
    }

    public Endmember(String name, double[] wavelengths, double[] radiations) {
        Assert.notNull(name, "name");
        if (wavelengths.length != radiations.length) {
            throw new IllegalArgumentException("wavelengths.length != radiations.length");
        }
        this.name = name;
        this.wavelengths = wavelengths;
        this.radiations = radiations;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return wavelengths.length;
    }

    public double getWavelength(int i) {
        return wavelengths[i];
    }

    public double[] getWavelengths() {
        return wavelengths.clone();
    }

    public double getRadiation(int i) {
        return radiations[i];
    }

    public double[] getRadiations() {
        return radiations.clone();
    }

    @Override
    public String toString() {
        return getName();
    }
}
