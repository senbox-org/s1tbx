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

package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

/**
 * A generic product reader for NetCDF files. Trying to find the best matching metadata profile
 * for the given input.
 */
public class GenericNetCdfReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;
    private ProductReader netCdfReader;

    public GenericNetCdfReader(GenericNetCdfReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        final File fileLocation = new File(getInput().toString());
        netcdfFile = NetcdfFileOpener.open(fileLocation.getPath());
        if (netcdfFile == null) {
            throw new IOException("Failed to open file " + fileLocation.getPath());
        }
        AbstractNetCdfReaderPlugIn[] plugIns = GenericNetCdfReaderPlugIn.getAllNetCdfReaderPlugIns();
        AbstractNetCdfReaderPlugIn bestPlugIn = null;
        for (AbstractNetCdfReaderPlugIn plugIn : plugIns) {
            DecodeQualification decodeQualification = plugIn.getDecodeQualification(netcdfFile);
            if (DecodeQualification.INTENDED.equals(decodeQualification)) {
                bestPlugIn = plugIn;
                break;
            }
            if (DecodeQualification.SUITABLE.equals(decodeQualification) && bestPlugIn == null) {
                bestPlugIn = plugIn;
            }
        }
        if (bestPlugIn == null) {
            String msg = String.format("Not able to read %s. No suitable NetCDF reader found.", getInput());
            throw new IOException(msg);
        }
        netCdfReader = bestPlugIn.createReaderInstance();
        return netCdfReader.readProductNodes(getInput(), getSubsetDef());
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Data is provided by different reader");
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        if (netCdfReader != null) {
            netCdfReader.close();
            netCdfReader = null;
        }
        super.close();
    }
}
