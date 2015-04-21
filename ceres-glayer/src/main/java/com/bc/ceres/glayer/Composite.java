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

package com.bc.ceres.glayer;

import java.awt.AlphaComposite;

/**
 * An enumeration used by {@link Layer}.
 *
 * @author Norman Fomferra
 */
public enum Composite {
    CLEAR(AlphaComposite.CLEAR),
    SRC(AlphaComposite.SRC),
    DST(AlphaComposite.DST),
    SRC_OVER(AlphaComposite.SRC_OVER),
    DST_OVER(AlphaComposite.DST_OVER),
    SRC_IN(AlphaComposite.SRC_IN),
    DST_IN(AlphaComposite.DST_IN),
    SRC_OUT(AlphaComposite.SRC_OUT),
    DST_OUT(AlphaComposite.DST_OUT),
    SRC_ATOP(AlphaComposite.SRC_ATOP),
    DST_ATOP(AlphaComposite.DST_ATOP),
    XOR(AlphaComposite.XOR);

    final int value;

    Composite(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public AlphaComposite getAlphaComposite(float alpha) {
        return AlphaComposite.getInstance(getValue(), alpha);
    }
}
