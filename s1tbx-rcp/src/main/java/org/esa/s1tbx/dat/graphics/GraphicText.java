/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Java 2D graphic utils
 */
public class GraphicText {

    public static void setHighQuality(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void shadowText(final Graphics2D g, final Color col, final String text, final int x, final int y) {
        g.setColor(Color.black);
        g.drawString(text, x + 2, y + 2);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static void outlineText(final Graphics2D g, final Color col, final String text, final int x, final int y) {
        g.setColor(Color.black);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static void highlightText(final Graphics2D g, final Color col, final String text, final int x, final int y,
                                     final Color highlightColor) {
        g.setColor(highlightColor);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static String padString(final String text, final int width) {
        if (text.length() >= width || text.equalsIgnoreCase("NaN"))
            return text;
        final StringBuilder newText = new StringBuilder(width);
        final int firstPos = width - text.length();
        int i = 0;
        while (i <= firstPos) {
            newText.append(' ');
            ++i;
        }
        newText.append(text);
        return newText.toString();
    }
}
