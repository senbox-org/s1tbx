/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Band;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Thomas Storm
 */
class BandSorter {

    static Comparator<Band> createComparator() {
        return new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                String band1Name = removeDigits(band1);
                String band2Name = removeDigits(band2);
                float wavelength1 = band1.getSpectralWavelength();
                float wavelength2 = band2.getSpectralWavelength();

                if (band1Name.equals(band2Name)) {
                    // names without digits are equal: decide by wavelength
                    if (wavelength1 > 0 && wavelength2 > 0 && wavelength1 != wavelength2) {
                        return (int) (wavelength1 - wavelength2);
                    } else {
                        // wavelengths are equal: decide by original name
                        return band1.getName().compareTo(band2.getName());
                    }
                } else {
                    // names are not equal -> try to sort by wavelength
                    if (wavelength1 > 0.0 && wavelength2 > 0.0) {
                        return (int) (wavelength1 - wavelength2);
                    } else {
                        // names are not equal and there are no wavelengths: sort by name
                        return band1.getName().compareTo(band2.getName());
                    }
                }
            }
        };
    }

    static void sort(Band[] allBands) {
        Arrays.sort(allBands, createComparator());
    }

    private static String removeDigits(Band band) {
        return band.getName().replaceAll("\\d", "");
    }

}
