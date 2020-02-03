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
package org.esa.snap.core.gpf.monitor;

import org.esa.snap.core.gpf.internal.OperatorImage;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * A printer for tile computation events.
 * It prints out immediately when a tile computation has happened.
 * Re-computations are indicated.
 *
 * May be used as a value for the 'snap.config' variable 'snap.gpf.tileComputationObserver'.
 *
 * @author marco Zuehlke
 */
public class TileComputationEventLogger extends TileComputationObserver {

    private static class TileEvent {
        private final OperatorImage image;
        private final int tileX;
        private final int tileY;
        private final double duration;

        TileEvent(TileComputationEvent event) {
            this.image = event.getImage();
            this.tileX = event.getTileX();
            this.tileY = event.getTileY();
            this.duration = nanosToRoundedSecs((event.getEndNanos() - event.getStartNanos()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TileEvent that = (TileEvent) o;

            if (tileX != that.tileX) return false;
            if (tileY != that.tileY) return false;
            if (image != that.image) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = image.hashCode();
            result = 31 * result + tileX;
            result = 31 * result + tileY;
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s, tileX=%d, tileY=%d, tileWidth=%d, tileHeight=%d, time=%f",
                                 image, tileX, tileY, image.getTileWidth(), image.getTileHeight(), duration);
        }

        private static double nanosToRoundedSecs(long nanos) {
            double secs = nanos * 1.0E-9;
            return Math.round(1000.0 * secs) / 1000.0;
        }
    }

    private final Set<TileEvent> recordedEventSet = new HashSet<TileEvent>();

    @Override
    public void start() {
        getLogger().log(Level.INFO, "Starting TileComputationPrinter");
    }

    @Override
    public void tileComputed(TileComputationEvent event) {
        TileEvent tileEvent = new TileEvent(event);
        String message = tileEvent.toString();
        boolean newEvent = false;
        synchronized (recordedEventSet) {
            if (!recordedEventSet.contains(tileEvent)) {
                recordedEventSet.add(tileEvent);
                newEvent = true;
            }
        }
        if (newEvent) {
            getLogger().log(Level.INFO, "Tile computed: " + message);
        } else {
            getLogger().log(Level.WARNING, "Tile re-computed: " + message);
        }
    }

    @Override
    public void stop() {
        recordedEventSet.clear();
        getLogger().log(Level.INFO, "Stopping TileComputationPrinter");
    }
}
