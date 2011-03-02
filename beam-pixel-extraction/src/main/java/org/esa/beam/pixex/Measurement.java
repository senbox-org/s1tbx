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

package org.esa.beam.pixex;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.Arrays;

/**
 * A container class which holds all information of an measurement.
 */
public class Measurement {

    private final ProductData.UTC time;
    private final GeoPos geoPos;
    private final Number[] values;
    private final int coordinateID;
    private final int productId;
    private final float pixelX;
    private final float pixelY;
    private final String coordinateName;
    private final boolean isValid;

    Measurement(int coordinateID, String name, int productId, float pixelX, float pixelY, ProductData.UTC time,
                GeoPos geoPos, Number[] values, boolean isValid) {
        this.coordinateID = coordinateID;
        this.productId = productId;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        coordinateName = name;
        this.time = time;
        this.geoPos = geoPos;
        this.values = new Number[values.length];
        this.isValid = isValid;
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    ProductData.UTC getTime() {
        return time;
    }

    Number[] getValues() {
        return values;
    }

    float getLat() {
        return geoPos.lat;
    }

    float getLon() {
        return geoPos.lon;
    }

    int getCoordinateID() {
        return coordinateID;
    }

    String getCoordinateName() {
        return coordinateName;
    }

    boolean isValid() {
        return isValid;
    }

    float getPixelX() {
        return pixelX;
    }

    float getPixelY() {
        return pixelY;
    }

    int getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Measurement that = (Measurement) o;

        if (coordinateID != that.coordinateID) {
            return false;
        }
        if (isValid != that.isValid) {
            return false;
        }
        if (Float.compare(that.pixelX, pixelX) != 0) {
            return false;
        }
        if (Float.compare(that.pixelY, pixelY) != 0) {
            return false;
        }
        if (productId != that.productId) {
            return false;
        }
        if (!coordinateName.equals(that.coordinateName)) {
            return false;
        }
        if (!geoPos.equals(that.geoPos)) {
            return false;
        }
        if (!time.equalElems(that.time)) {
            return false;
        }
        if (!Arrays.equals(values, that.values)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = time.hashCode();
        result = 31 * result + geoPos.hashCode();
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + coordinateID;
        result = 31 * result + productId;
        result = 31 * result + (pixelX == +0.0f ? 0 : Float.floatToIntBits(pixelX));
        result = 31 * result + (pixelY == +0.0f ? 0 : Float.floatToIntBits(pixelY));
        result = 31 * result + coordinateName.hashCode();
        result = 31 * result + (isValid ? 1 : 0);
        return result;
    }
}