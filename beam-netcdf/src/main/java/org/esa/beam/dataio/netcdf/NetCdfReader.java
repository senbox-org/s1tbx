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

package org.esa.beam.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfileImpl;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileSpiRegistry;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.dataio.netcdf.util.VariableMap;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

/**
 * A product reader for NetCDF files following different metadata profiles.
 *
 * @author Norman Fomferra
 */
public class NetCdfReader extends AbstractProductReader {

    private final String profileClassName;

    private NetcdfFile netcdfFile;
    private boolean isYFlipped;
    private VariableMap rasterVariableMap;

    public NetCdfReader(ProductReaderPlugIn readerPlugIn, String profileClassName) {
        super(readerPlugIn);
        this.profileClassName = profileClassName;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws
                                             IOException {

        final File fileLocation = new File(getInput().toString());
        netcdfFile = NetcdfFile.open(fileLocation.getPath());

        ProfileSpiRegistry profileSpiRegistry = ProfileSpiRegistry.getInstance();
        ProfileSpi profileSpi;
        if (profileClassName != null) {
            profileSpi = profileSpiRegistry.getProfileFactory(profileClassName);
        } else {
            profileSpi = profileSpiRegistry.getProfileFactory(netcdfFile);
        }
        if (profileSpi == null) {
            netcdfFile.close();
            String message = "No convention factory found for netCDF ";
            if (profileClassName != null) {
                message += "(profile = " + profileClassName + ")";
            }
            throw new IllegalFileFormatException(message);
        }
        final ProfileReadContext context = profileSpi.createReadContext(netcdfFile);
        if (context.getRasterDigest() == null) {
            close();
            throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                                                 "be interpreted as remote sensing bands.");  /*I18N*/
        }
        Profile profile = new ProfileImpl();
        profileSpi.configureProfile(netcdfFile, profile);
        String productName = FileUtils.getFilenameWithoutExtension(fileLocation);
        context.setProperty(Constants.PRODUCT_NAME_PROPERTY_NAME, productName);
        final Product product = profile.readProduct(context);
        final Object object = context.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        if (object instanceof Boolean) {
            isYFlipped = (Boolean) object;
        }
        rasterVariableMap = context.getRasterVariableMap();
        product.setFileLocation(fileLocation);
        product.setProductReader(this);
        product.setModified(false);
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final int sceneHeight = destBand.getProduct().getSceneRasterHeight();
        final int y0 = isYFlipped ? (sceneHeight - 1) - sourceOffsetY : sourceOffsetY;

        String variableName = ReaderUtils.getVariableName(destBand);
        final Variable variable = rasterVariableMap.get(variableName);
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
        }
        shape[rank - 2] = 1;
        shape[rank - 1] = destWidth;
        origin[rank - 1] = sourceOffsetX;

        pm.beginTask("Reading data from band '" + destBand.getName() + "'", destHeight);
        try {
            for (int y = 0; y < destHeight; y++) {
                origin[rank - 2] = isYFlipped ? y0 - y : y0 + y;
                final Array array;
                synchronized (netcdfFile) {
                    array = variable.read(origin, shape);
                }
                final Object storage = array.getStorage();
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(storage, 0, destBuffer.getElems(), y * destWidth, destWidth);
                pm.worked(1);
                if (pm.isCanceled()) {
                    throw new IOException("Process terminated by user."); /*I18N*/
                }
            }
        } catch (InvalidRangeException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        } finally {
            pm.done();
        }
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
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
        super.close();
    }
}
