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
package org.esa.snap.dataio.geotiff.internal;

import org.esa.snap.core.util.geotiff.GeoTIFFCodes;

public class GeoKeyEntry {

    private final int keyId;
    private final int tiffTagLocation;
    private final int count;
    private final Integer intValue;
    private final String strValue;
    private final double[] dblValue;

    public GeoKeyEntry(int keyId, int tiffTagLocation, int count, Object value) {
        this.keyId = keyId;
        this.tiffTagLocation = tiffTagLocation;
        this.count = count;
        if (value instanceof Integer) {
            intValue = (Integer) value;
            strValue = null;
            dblValue = null;
        } else if (value instanceof String) {
            intValue = null;
            strValue = (String) value;
            dblValue = null;
        } else if (value instanceof double[]) {
            intValue = null;
            strValue = null;
            dblValue = (double[]) value;
        } else {
            throw new IllegalArgumentException(value.getClass() + "not supported");
        }
    }

    public int getKeyId(){
         return keyId;
    }

    public String getName() {
        return GeoTIFFCodes.getInstance().getName(getKeyId());
    }

    boolean hasIntValue() {
        return intValue != null;
    }

    boolean hasStrValue() {
        return strValue != null;
    }

    boolean hasDblValue() {
        return dblValue != null;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public String getStrValue() {
        return strValue;
    }

    public double[] getDblValue() {
        return dblValue;
    }

    @Override
    public String toString() {
        final String s1 = "" +
                          keyId + ", " +
                          tiffTagLocation + ", " +
                          count + ", ";
        final String s2;
        if (hasIntValue()) {
            s2 = String.valueOf(intValue);
        } else if (hasDblValue()) {
            final StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (double v : dblValue) {
                sb.append(v);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("]");
            s2 = sb.toString();
        } else {
            s2 = strValue;
        }
        return s1 + s2;
    }
}
