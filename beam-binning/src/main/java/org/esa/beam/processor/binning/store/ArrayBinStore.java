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

package org.esa.beam.processor.binning.store;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinLocator;

import java.io.IOException;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class ArrayBinStore extends AbstractLinearBinStore {
    private int numVarsPerBin;
    private float[] tempBinContent;
    private float[] data;

    public ArrayBinStore(BinLocator locator, int numVarsPerBin) {
        this.locator = locator;
        this.numVarsPerBin = numVarsPerBin;
        tempBinContent = new float[numVarsPerBin];
        data = new float[locator.getNumCells() * numVarsPerBin];
    }

    @Override
    public void write(int index, Bin bin) {
        bin.save(tempBinContent);
        System.arraycopy(tempBinContent, 0, data, index * numVarsPerBin, numVarsPerBin);
    }

    @Override
    public void read(int index, Bin bin) {
        System.arraycopy(data, index * numVarsPerBin, tempBinContent, 0, numVarsPerBin);
        bin.load(tempBinContent);
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }

    public void delete() throws IOException {
        data = null;
    }
}
