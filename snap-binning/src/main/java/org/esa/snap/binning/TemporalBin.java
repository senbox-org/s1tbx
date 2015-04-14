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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;


/**
 * A temporal bin.
 *
 * @author Norman Fomferra
 */
public class TemporalBin extends Bin {

    int numPasses;

    public TemporalBin() {
        super();
    }

    public TemporalBin(long index, int numFeatures) {
        super(index, numFeatures);
    }

    public int getNumPasses() {
        return numPasses;
    }

    public void setNumPasses(int numPasses) {
        this.numPasses = numPasses;
    }

    public void write(DataOutput dataOutput) throws IOException {
        // Note, we don't serialise the index, because it is usually the MapReduce key
        dataOutput.writeInt(numObs);
        dataOutput.writeInt(numPasses);
        dataOutput.writeInt(featureValues.length);
        for (float value : featureValues) {
            dataOutput.writeFloat(value);
        }
    }

    public void readFields(DataInput dataInput) throws IOException {
        // Note, we don't serialise the index, because it is usually the MapReduce key
        numObs = dataInput.readInt();
        numPasses = dataInput.readInt();
        final int numFeatures = dataInput.readInt();
        featureValues = new float[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            featureValues[i] = dataInput.readFloat();
        }
    }

    public static TemporalBin read(DataInput dataInput) throws IOException {
        TemporalBin bin = new TemporalBin();
        bin.readFields(dataInput);
        return bin;
    }

    public static TemporalBin read(long index, DataInput dataInput) throws IOException {
        TemporalBin bin = new TemporalBin();
        bin.readFields(dataInput);
        bin.index = index;
        return bin;
    }

    @Override
    public String toString() {
        return String.format("%s{index=%d, numObs=%d, numPasses=%d, featureValues=%s}",
                             getClass().getSimpleName(), index, numObs, numPasses, Arrays.toString(featureValues));
    }

}
