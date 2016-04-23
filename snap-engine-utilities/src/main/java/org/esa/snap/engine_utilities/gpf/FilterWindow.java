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
package org.esa.snap.engine_utilities.gpf;

import java.awt.*;

/**
 * Window helper class
 */
public class FilterWindow {

    public static final String SIZE_3x3 = "3x3";
    public static final String SIZE_5x5 = "5x5";
    public static final String SIZE_7x7 = "7x7";
    public static final String SIZE_9x9 = "9x9";
    public static final String SIZE_11x11 = "11x11";
    public static final String SIZE_13x13 = "13x13";
    public static final String SIZE_15x15 = "15x15";
    public static final String SIZE_17x17 = "17x17";

    private final int windowSizeX, windowSizeY;
    private final int halfWindowSizeX, halfWindowSizeY;

    public FilterWindow(final String windowSizeStr) {
        this(parseWindowSize(windowSizeStr));
    }

    public FilterWindow(final int size) {
        this(size, size);
    }

    public FilterWindow(final int sizeX, final int sizeY) {
        this.windowSizeX = sizeX;
        this.windowSizeY = sizeY;
        this.halfWindowSizeX = sizeX / 2;
        this.halfWindowSizeY = sizeY / 2;
    }

    public int getWindowSize() {
        return windowSizeX;
    }

    public int getWindowSizeX() {
        return windowSizeX;
    }

    public int getWindowSizeY() {
        return windowSizeY;
    }

    public int getHalfWindowSize() {
        return halfWindowSizeX;
    }

    public int getHalfWindowSizeX() {
        return halfWindowSizeX;
    }

    public int getHalfWindowSizeY() {
        return halfWindowSizeY;
    }

    public static int parseWindowSize(final String windowSizeStr) {
        switch (windowSizeStr) {
            case SIZE_3x3:
                return 3;
            case SIZE_5x5:
                return 5;
            case SIZE_7x7:
                return 7;
            case SIZE_9x9:
                return 9;
            case SIZE_11x11:
                return 11;
            case SIZE_13x13:
                return 13;
            case SIZE_15x15:
                return 15;
            case SIZE_17x17:
                return 17;
            default:
                return 0;
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of pixel at the upper left corner of the target tile.
     * @param y0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param w  The width of the target tile.
     * @param h  The height of the target tile.
     * @return The source tile rectangle.
     */
    public Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h,
                                             final int sourceImageWidth, final int sourceImageHeight) {

        int sx0 = x0, sy0 = y0;
        int sw = w, sh = h;

        if (x0 >= halfWindowSizeX) {
            sx0 -= halfWindowSizeX;
            sw += halfWindowSizeX;
        }

        if (y0 >= halfWindowSizeY) {
            sy0 -= halfWindowSizeY;
            sh += halfWindowSizeY;
        }

        if (x0 + w + halfWindowSizeX <= sourceImageWidth) {
            sw += halfWindowSizeX;
        }

        if (y0 + h + halfWindowSizeY <= sourceImageHeight) {
            sh += halfWindowSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }
}
