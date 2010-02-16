/*
 * $Id: ModisTiePointDescription.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
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
package org.esa.beam.dataio.modis.productdb;

public class ModisTiePointDescription {

    private final String name;
    private final String scale;
    private final String offset;
    private final String unit;

    /**
     * Creates the object with given parameters.
     *
     * @param name            the name of the tie point grid
     * @param scaleAttribute  the name of the attribute containing the scale factor
     * @param offsetAttribute the name of the attribute containing the scaling offset
     * @param unitAttribute   the name of the attribute containi8ng the unit name
     */
    public ModisTiePointDescription(final String name, final String scaleAttribute,
                                    final String offsetAttribute, final String unitAttribute) {
        this.name = name;
        this.scale = scaleAttribute;
        this.offset = offsetAttribute;
        this.unit = unitAttribute;
    }

    /**
     * Retrievs the name of the rtie point grid
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the name of the scaling factor attribute
     *
     * @return the name
     */
    public String getScaleAttribName() {
        return scale;
    }

    /**
     * Retrieves the name of the scaling offset attribute
     *
     * @return the name
     */
    public String getOffsetAttribName() {
        return offset;
    }

    /**
     * Retrieves the name of the attribute for the physical unit
     *
     * @return the name
     */
    public String getUnitAttribName() {
        return unit;
    }
}
