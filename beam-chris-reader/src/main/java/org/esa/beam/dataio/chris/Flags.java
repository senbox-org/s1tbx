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

package org.esa.beam.dataio.chris;

import java.awt.Color;

/**
 * The enumeration type {@code Flags} is a representation of
 * the flags used by CHRIS images.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public enum Flags {

    /**
     * Dropout flag.
     */
    DROPOUT(0x0001, "Dropout pixel", Color.red),
    /**
     * Saturation flag.
     */
    SATURATED(0x0002, "Saturated pixel", Color.orange),
    /**
     * Dropout correction flag.
     */
    DROPOUT_CORRECTED(0x0100, "Corrected dropout pixel", Color.green);

    private int mask;
    private Color color;
    private float transparency;
    private String description;

    private Flags(final int mask, final String description, final Color color) {
        this(mask, description, color, 0.5f);
    }

    private Flags(final int mask, final String description, final Color color, final float transparency) {
        this.mask = mask;
        this.color = color;
        this.transparency = transparency;
        this.description = description;
    }

    /**
     * Returns the bit mask associated with the flag.
     *
     * @return the bit mask.
     */
    public final int getMask() {
        return mask;
    }

    /**
     * Returns the textual description of the flag.
     *
     * @return the textual description.
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Returns the color associated with this flag (useful for colored bit mask layer).
     *
     * @return the color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the transparency associated with this flag (useful for colored bit mask layer).
     *
     * @return the transparency.
     */
    public final float getTransparency() {
        return transparency;
    }

    /**
     * Tests a bit pattern for the status of the flag.
     *
     * @param value the bit pattern.
     *
     * @return true if the flag is set, false otherwise.
     */
    public final boolean isSet(final int value) {
        return (value & mask) != 0;
    }

    @Override
    public final String toString() {
        return name().toLowerCase();
    }

}
