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

package org.esa.beam.framework.gpf.internal;

/**
 * A simple tile cache metric which holds the average time required to process a pixel of an image.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PerformanceMetric {
    private final MeanCalc sourceNanosPerPixel;
    private final MeanCalc targetNanosPerPixel;

    public PerformanceMetric() {
        sourceNanosPerPixel = new MeanCalc();
        targetNanosPerPixel = new MeanCalc();
    }

    /**
     * @return The accumulated number of nanos per source pixel.
     */
    public double getSourceNanosPerPixel() {
        return sourceNanosPerPixel.getMean();
    }

    /**
     * @return The accumulated number of nanos per target pixel.
     */
    public double getTargetNanosPerPixel() {
        return targetNanosPerPixel.getMean();
    }

    /**
     * @return The net number of nanos per pixel (target NPP - source NPP).
     */
    public double getNanosPerPixel() {
        return getTargetNanosPerPixel() - getSourceNanosPerPixel();
    }

    public void updateSource(double value) {
        sourceNanosPerPixel.update(value);
    }

    public void updateTarget(double value) {
        targetNanosPerPixel.update(value);
    }

    private static class MeanCalc {
        private double mean;
        private long count;

        public final synchronized double getMean() {
            return mean;
        }

        public final synchronized long getCount() {
            return count;
        }

        public final synchronized void update(double value) {
            mean = (mean * count + value) / (count + 1);
            count++;
        }
    }
}
