/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Stx;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class HistogramPanelModel {

    Map<HistogramConfig, Stx> stxMap = new HashMap<>(31);

    public boolean hasStx(HistogramConfig config) {
        return stxMap.containsKey(config);
    }

    public Stx getStx(HistogramConfig config) {
        if (!stxMap.containsKey(config)) {
            throw new IllegalArgumentException("No such key: " + config);
        }
        return stxMap.get(config);
    }

    public void setStx(HistogramConfig config, Stx stx) {
        if (hasStx(config)) {
            throw new IllegalArgumentException("Trying to overwrite valid stx for config: " + config);
        }
        stxMap.put(config, stx);
    }

    static interface HistogramConfig {
    }

    static class NullConfig implements HistogramConfig {
    }

    static class HistogramConfigImpl implements HistogramConfig {

        String bandName;
        String roiMask;
        int numBins;
        boolean logScaledBins;

        HistogramConfigImpl(String bandName, String roiMask, int numBins, boolean logScaledBins) {
            this.bandName = bandName;
            this.roiMask = roiMask;
            this.numBins = numBins;
            this.logScaledBins = logScaledBins;
        }

        @Override
        public String toString() {
            return "HistogramConfig{" +
                    "bandName='" + bandName + '\'' +
                    "roiMask='" + roiMask + '\'' +
                    ", numBins=" + numBins +
                    ", logScaledBins=" + logScaledBins +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HistogramConfigImpl that = (HistogramConfigImpl) o;

            return logScaledBins == that.logScaledBins &&
                    numBins == that.numBins &&
                    !(roiMask != null ? !roiMask.equals(that.roiMask) : that.roiMask != null) &&
                    bandName.equals(that.bandName);

        }

        @Override
        public int hashCode() {
            int result = roiMask != null ? roiMask.hashCode() : 0;
            result = 31 * result + numBins;
            result = 31 * result + (logScaledBins ? 1 : 0);
            return result;
        }
    }
}
