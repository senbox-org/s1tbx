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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;

public class PlacemarkNameFactory {

    private PlacemarkNameFactory() {
    }

    public static String[] createUniqueNameAndLabel(PlacemarkDescriptor placemarkDescriptor, Product product) {
        ProductNodeGroup<Placemark> placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
        int pinNumber = placemarkGroup.getNodeCount() + 1;
        String name = createName(placemarkDescriptor, pinNumber);
        while (placemarkGroup.get(name) != null) {
            name = createName(placemarkDescriptor, ++pinNumber);
        }
        final String label = createLabel(placemarkDescriptor, pinNumber, true);
        return new String[]{name, label};
    }

    public static String createLabel(PlacemarkDescriptor placemarkDescriptor, int pinNumber, boolean firstLetterIsUpperCase) {
        Assert.argument(placemarkDescriptor.getRoleLabel().length() > 0, "placemarkDescriptor.getRoleLabel()");
        String name = placemarkDescriptor.getRoleLabel();
        if (firstLetterIsUpperCase) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name + " " + pinNumber;
    }

    public static String createName(PlacemarkDescriptor placemarkDescriptor, int pinNumber) {
        return placemarkDescriptor.getRoleName() + "_" + pinNumber;
    }

}
