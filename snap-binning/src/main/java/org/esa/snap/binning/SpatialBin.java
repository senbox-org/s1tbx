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

package org.esa.snap.binning;

import org.esa.snap.binning.support.GrowableVector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;


/**
 * A spatial bin.
 *
 * @author Norman Fomferra
 */
public class SpatialBin extends Bin {

    protected GrowableVector[] vectors;

    // this constructor is only public because it is used in Calvalus - undocumented API! tb 2018-03-26
    public SpatialBin() {
        super();
        vectors = new GrowableVector[0];
    }

    public SpatialBin(long index, int numFeatures) {
        this(index, numFeatures, 0);
    }

    public SpatialBin(long index, int numFeatures, int numGrowableFeatures) {
        super(index, numFeatures);
        if (numGrowableFeatures > 0) {
            vectors = new GrowableVector[numGrowableFeatures];
            for (int i = 0; i < numGrowableFeatures; i++) {
                vectors[i] = new GrowableVector(256);   // @todo 2 tb/tb check if this is a meaningful default value 2018-03-12
            }
        } else {
            vectors = new GrowableVector[0];
        }
    }

    public void write(DataOutput dataOutput) throws IOException {
        // Note, we don't serialise the index, because it is usually the MapReduce key
        dataOutput.writeInt(numObs);
        dataOutput.writeInt(featureValues.length);
        for (float value : featureValues) {
            dataOutput.writeFloat(value);
        }

        final int numVectors = vectors.length;
        dataOutput.writeInt(numVectors);
        for (final GrowableVector vector : vectors) {
            final int vectorSize = vector.size();
            dataOutput.writeInt(vectorSize);
            for (int k = 0; k < vectorSize; k++) {
                dataOutput.writeFloat(vector.get(k));
            }
        }
    }

    public void readFields(DataInput dataInput) throws IOException {
        // // Note, we don't serialise the index, because it is usually the MapReduce key
        numObs = dataInput.readInt();
        final int numFeatures = dataInput.readInt();
        featureValues = new float[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            featureValues[i] = dataInput.readFloat();
        }
        final int numVectors = dataInput.readInt();
        vectors = new GrowableVector[numVectors];
        for (int i = 0; i < numVectors; i++) {
            final int vectorLength = dataInput.readInt();
            final GrowableVector vector = new GrowableVector(vectorLength);
            vectors[i] = vector;
            for (int k = 0; k < vectorLength; k++) {
                vector.add(dataInput.readFloat());
            }
        }
    }

    public static SpatialBin read(DataInput dataInput) throws IOException {
        return read(-1L, dataInput);
    }

    public static SpatialBin read(long index, DataInput dataInput) throws IOException {
        SpatialBin bin = new SpatialBin();
        bin.index = index;
        bin.readFields(dataInput);
        return bin;
    }

    public GrowableVector[] getVectors() {
        return vectors;
    }

    @Override
    public String toString() {
        return String.format("%s{index=%d, numObs=%d, featureValues=%s}",
                getClass().getSimpleName(), index, numObs, Arrays.toString(featureValues));
    }
}
