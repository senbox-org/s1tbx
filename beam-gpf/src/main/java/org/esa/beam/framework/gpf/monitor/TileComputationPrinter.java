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
package org.esa.beam.framework.gpf.monitor;

import org.esa.beam.framework.gpf.internal.OperatorImage;

import java.util.HashSet;
import java.util.Set;

/**
 * A printer for tile computation events.
 * It prints out immediately when a tile computation has happened.
 * Re-computations are indicated.
 *
 * May be used as a value for the 'beam.config' variable 'beam.gpf.tileComputationObserver'.
 *
 * @author marco Zuehlke
 */
public class TileComputationPrinter extends TileComputationObserver {

    private static class TileEvent {
        private final OperatorImage image;
        private final int tileX;
        private final int tileY;

        TileEvent(TileComputationEvent event) {
            this.image = event.getImage();
            this.tileX = event.getTileX();
            this.tileY = event.getTileY();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TileEvent that = (TileEvent) o;

            if (tileX != that.tileX) return false;
            if (tileY != that.tileY) return false;
            if (!image.equals(that.image)) return false;

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
            return String.format("%70s, tileX=%d, tileY=%d, tw=%d, th=%d", image, tileX, tileY, image.getTileWidth(), image.getTileHeight());
        }
    }

    private final Set<TileEvent> recordedEventList = new HashSet<TileEvent>();

    @Override
    public void start() {
        System.out.println("Starting TileComputationPrinter");
    }

    @Override
    public void tileComputed(TileComputationEvent event) {
        TileEvent tileEvent = new TileEvent(event);
        String message = tileEvent.toString();
        boolean newEvent = false;
        synchronized (recordedEventList) {
            if (!recordedEventList.contains(tileEvent)) {
                recordedEventList.add(tileEvent);
                newEvent = true;
            }
        }
        if (newEvent) {
            System.out.println("computed: " + message);
        } else {
            System.out.println("RE-computed: " + message);
        }
    }

    @Override
    public void stop() {
        recordedEventList.clear();
        System.out.println("Stoping TileComputationPrinter");
    }
}
