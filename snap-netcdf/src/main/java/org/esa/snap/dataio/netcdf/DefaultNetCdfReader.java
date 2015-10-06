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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

class DefaultNetCdfReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;

    public DefaultNetCdfReader(AbstractNetCdfReaderPlugIn netCdfReaderPlugIn) {
        super(netCdfReaderPlugIn);
    }

    @Override
    public AbstractNetCdfReaderPlugIn getReaderPlugIn() {
        return (AbstractNetCdfReaderPlugIn) super.getReaderPlugIn();

    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final AbstractNetCdfReaderPlugIn plugIn = getReaderPlugIn();
        final File fileLocation = new File(getInput().toString());
        netcdfFile = NetcdfFileOpener.open(fileLocation.getPath());
        if (netcdfFile == null) {
            throw new IOException("Failed to open file " + fileLocation.getPath());
        }
        final ProfileReadContext context = new ProfileReadContextImpl(netcdfFile);
        String filename = extractProductName(fileLocation);
        context.setProperty(Constants.PRODUCT_FILENAME_PROPERTY, filename);
        plugIn.initReadContext(context);
        NetCdfReadProfile profile = new NetCdfReadProfile();
        configureProfile(plugIn, profile);
        final Product product = profile.readProduct(context);
        product.setFileLocation(fileLocation);
        product.setProductReader(this);
        product.setModified(false);
        return product;

    }

    static String extractProductName(File fileLocation) {
        String name = fileLocation.getName();
        if (name.endsWith(".gz") || name.endsWith(".zip")) {
            final int dotIndex = name.lastIndexOf(".");
            name = name.substring(0, dotIndex);
        }

        return FileUtils.getFilenameWithoutExtension(name);
    }

    private void configureProfile(AbstractNetCdfReaderPlugIn plugIn, NetCdfReadProfile profile) {
        profile.setInitialisationPartReader(plugIn.createInitialisationPartReader());
        profile.addProfilePartReader(plugIn.createMetadataPartReader());
        profile.addProfilePartReader(plugIn.createBandPartReader());
        profile.addProfilePartReader(plugIn.createTiePointGridPartReader());
        profile.addProfilePartReader(plugIn.createFlagCodingPartReader());
        profile.addProfilePartReader(plugIn.createGeoCodingPartReader());
        profile.addProfilePartReader(plugIn.createImageInfoPartReader());
        profile.addProfilePartReader(plugIn.createIndexCodingPartReader());
        profile.addProfilePartReader(plugIn.createMaskPartReader());
        profile.addProfilePartReader(plugIn.createStxPartReader());
        profile.addProfilePartReader(plugIn.createTimePartReader());
        profile.addProfilePartReader(plugIn.createDescriptionPartReader());

    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Data is provided by images");
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

}
