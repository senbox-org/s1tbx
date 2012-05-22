/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning;

import org.esa.beam.binning.support.ObservationImpl;

import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A "slice" of observations. A slice is a spatially contiguous area of observations.
 *
 * @author Norman Fomferra
 */
public final class ObservationSlice implements Iterable<Observation> {
    private final Raster[] sourceTiles;
    private final ArrayList<Observation> observations;

    public ObservationSlice(Raster[] sourceTiles, int observationCapacity) {
        this.sourceTiles = sourceTiles;
        this.observations = new ArrayList<Observation>(observationCapacity);
    }

    public float[] createObservationSamples(int x, int y) {
        final float[] samples = new float[sourceTiles.length];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = sourceTiles[i].getSampleFloat(x, y, 0);
        }
        return samples;
    }

    public void addObservation(double lat, double lon, float[] samples) {
        observations.add(new ObservationImpl(lat, lon, samples));
    }

    public int getSize() {
        return observations.size();
    }

    @Override
    public Iterator<Observation> iterator() {
        return observations.iterator();
    }
}
