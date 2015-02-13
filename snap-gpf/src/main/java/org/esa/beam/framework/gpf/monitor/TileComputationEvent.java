/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.monitor;

import org.esa.beam.framework.gpf.internal.OperatorImage;

/**
 * A event that is generated after a tile has been computed.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public class TileComputationEvent {
    private final int id;
    private final OperatorImage image;
    private final int tileX;
    private final int tileY;
    private final long startNanos;
    private final long endNanos;
    private final String threadName;
    private final long nettoNanos;

    static int ids = 0;

    public TileComputationEvent(OperatorImage image, int tileX, int tileY, long startNanos, long endNanos, long nettoNanos) {
        this.id = ++ids;
        this.image = image;
        this.tileX = tileX;
        this.tileY = tileY;
        this.startNanos = startNanos;
        this.endNanos = endNanos;
        this.threadName = Thread.currentThread().getName();
        this.nettoNanos = nettoNanos;
    }

    public int getId() {
        return id;
    }

    public OperatorImage getImage() {
        return image;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public long getNettoNanos() {
        return nettoNanos;
    }

    public String getThreadName() {
        return threadName;
    }
}
