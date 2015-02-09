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

import org.esa.beam.binning.support.VectorImpl;

import java.util.HashMap;


/**
 * A Hadoop-serializable bin.
 *
 * @author Norman Fomferra
 */
public abstract class Bin implements BinContext {

    long index;
    int numObs;
    float[] featureValues;

    // Not serialized for Hadoop
    private transient HashMap<String, Object> contextMap;

    public Bin() {
        this.index = -1;
    }

    public Bin(long index, int numFeatures) {
        this.index = index;
        setNumFeatures(numFeatures);
    }

    @Override
    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public int getNumObs() {
        return numObs;
    }

    public void setNumObs(int numObs) {
        this.numObs = numObs;
    }

    public float[] getFeatureValues() {
        return featureValues;
    }

    // for Calvalus where bins are re-used
    public void setNumFeatures(int numFeatures) {
        if (numFeatures < 0) {
            throw new IllegalArgumentException("numFeatures < 0");
        }
        featureValues = new float[numFeatures];
    }

    public WritableVector toVector() {
        return new VectorImpl(getFeatureValues());
    }

    @Override
    public <T> T get(String name) {
        return contextMap != null ? (T) contextMap.get(name) : null;
    }

    @Override
    public void put(String name, Object value) {
        if (contextMap == null) {
            contextMap = new HashMap<String, Object>();
        }
        contextMap.put(name, value);
    }
}
