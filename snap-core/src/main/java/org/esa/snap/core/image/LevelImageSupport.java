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

package org.esa.snap.core.image;


/**
 * Supports the development of images, which are returned by implementations of the
 * {@link com.bc.ceres.glevel.MultiLevelSource MultiLevelSource} interface.
 */
public final class LevelImageSupport {

    private final int sourceWidth;
    private final int sourceHeight;
    private final int level;
    private final double scale;

    public LevelImageSupport(int sourceWidth, int sourceHeight, ResolutionLevel level) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.level = level.getIndex();
        this.scale = level.getScale();
    }

    public int getSourceWidth() {
        return sourceWidth;
    }

    public int getSourceHeight() {
        return sourceHeight;
    }

    public int getLevel() {
        return level;
    }

    public double getScale() {
        return scale;
    }

    public final int getSourceX(int tx) {
        return getSourceCoord(tx, 0, getSourceWidth() - 1);
    }

    public final int getSourceY(int ty) {
        return getSourceCoord(ty, 0, getSourceHeight() - 1);
    }

    public final int getSourceWidth(int destWidth) {
        return double2int(getScale() * destWidth, 1, getSourceWidth());
    }

    public final int getSourceHeight(int destHeight) {
        return double2int(getScale() * destHeight, 1, getSourceHeight());
    }

    public final int getSourceCoord(double destCoord, int min, int max) {
        return double2int(getScale() * destCoord, min, max);
    }

    private static int double2int(double v, int min, int max) {
        int sc = (int) Math.floor(v);
        if (sc < min) {
            sc = min;
        }
        if (sc > max) {
            sc = max;
        }
        return sc;
    }
}
