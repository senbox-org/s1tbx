/*
 * $Id: HdfDimension.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.dataio.modis.hdf;

/**
 * The dimension class holds the name and the value of a HDF-EOS dimension object
 */
public class HdfDimension {

    private String _name;
    private int _value;

    /**
     * Creates the object with default data
     */
    HdfDimension() {
        this("", 0);
    }

    HdfDimension(final String name, final int value) {
        _name = name;
        _value = value;
    }

    /**
     * Sets the dimension name.
     *
     * @param name
     */
    void setName(String name) {
        _name = name;
    }

    /**
     * Retrieves the dimension name
     *
     * @return the name
     */
    String getName() {
        return _name;
    }

    /**
     * Sets the value of the dimension
     *
     * @param val
     */
    void setValue(int val) {
        _value = val;
    }

    /**
     * Retrieves the value of the dimension
     *
     * @return the value
     */
    int getValue() {
        return _value;
    }
}
