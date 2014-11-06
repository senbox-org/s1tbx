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

package com.bc.ceres.swing.update;

class CaselessKey implements Comparable {

    private final String key;
    private final int hashCode;

    public CaselessKey(String key) {
        this.key = key;
        hashCode = key.toLowerCase().hashCode();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return key.equalsIgnoreCase(obj.toString());
    }

    @Override
    public String toString() {
        return key;
    }

    public int compareTo(Object obj) {
        if (obj == null) {
            return 1;
        }
        return key.compareToIgnoreCase(obj.toString());
    }
}
