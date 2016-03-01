/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.graphics;

import java.awt.*;

/**
 * Create color maps
 */
public class Palette {

    // predefined color maps
    public final static Palette[] STANDARD_MAPS =
            {
                    new Palette("Rainbow", new Color[]
                            {Color.blue, Color.cyan, Color.green, Color.yellow, Color.orange, Color.red}),
                    new Palette("Cool", new Color[]
                            {Color.green, Color.blue, new Color(255, 0, 255)}),
                    new Palette("Warm", new Color[]
                            {Color.red, Color.orange, Color.yellow}),
                    new Palette("Thermal", new Color[]
                            {Color.black, Color.red, Color.orange, Color.yellow, Color.green,
                                    Color.blue, new Color(255, 0, 255), Color.white})
            };

    private Color[] colors;
    private final String name;

    public Palette(String name, Color[] colors) {
        this.colors = colors;
        this.name = name;
    }

    public Color[] getColors() {
        return colors;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the color corresponding to the specified value. The
     * argument is assumed to fall between 0 and 1 for interpolation. If
     * <code>val</code> is less than 0 or greater than 1, the start or
     * end colors will be returned, respectively.
     *
     * @param percent the value
     * @return the corresponding color
     */
    public Color lookupColor(final float percent) {
        int length = colors.length - 1;

        if (percent < 0.f)
            return colors[0];
        if (percent >= 1.f)
            return colors[length];

        final int pos = (int) (percent * length);
        final Color s = colors[pos];
        final Color e = colors[pos + 1];
        final float rem = percent * length - pos;
        return new Color(
                s.getRed() + (int) (rem * (e.getRed() - s.getRed())),
                s.getGreen() + (int) (rem * (e.getGreen() - s.getGreen())),
                s.getBlue() + (int) (rem * (e.getBlue() - s.getBlue())));
    }
}
