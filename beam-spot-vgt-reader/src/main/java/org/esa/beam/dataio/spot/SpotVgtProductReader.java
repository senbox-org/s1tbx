/*
 * Copyright (C) 2010  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.spot;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Reader for SPOT VGT products.
 *
 * @author Norman Fomferra
 * @version 1.0
 */
public class SpotVgtProductReader extends AbstractProductReader {

    private int sceneRasterWidth;
    private int sceneRasterHeight;

    /**
     * Constructor.
     *
     * @param productReaderPlugIn the product reader plug-in used to create this reader instance.
     */
    SpotVgtProductReader(final SpotVgtProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = SpotVgtProductReaderPlugIn.getFileInput(getInput());

        File dataDir = new File(inputFile.getParentFile(), SpotVgtConstants.DEFAULT_DATA_DIR_NAME);
        File[] hdfFiles = dataDir.listFiles(SpotVgtProductReaderPlugIn.HDF_FILTER);

        sceneRasterWidth = -1;
        sceneRasterHeight = -1;

        for (File hdfFile : hdfFiles) {
            NetcdfFile netcdfFile = NetcdfFile.open(hdfFile.getAbsolutePath());
            List<Variable> variableList = netcdfFile.getVariables();
            for (int j = 0; j < variableList.size(); j++) {
                Variable variable = variableList.get(j);
                System.out.println("hdfFile[" + hdfFile.getName() + "].variable[" + j + "] = " + variable.getNameAndDimensions());
            }
        }


        Band[] bands = new Band[hdfFiles.length];
        for (int i = 0; i < bands.length; i++) {
            File hdfFile = hdfFiles[i];
            String s = SpotVgtProductReaderPlugIn.getBandName(hdfFile);
            System.out.println("band = " + s);
            // bands[i] = new Band(s, );
        }

        return createProduct(inputFile);
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX,
                                                       int sourceOffsetY,
                                                       int sourceWidth,
                                                       int sourceHeight,
                                                       int sourceStepX,
                                                       int sourceStepY,
                                                       Band targetBand,
                                                       int targetOffsetX,
                                                       int targetOffsetY,
                                                       int targetWidth,
                                                       int targetHeight,
                                                       ProductData targetBuffer,
                                                       ProgressMonitor pm) throws IOException {
        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        // todo
    }

    @Override
    public void close() throws IOException {
        // TODO - close NetCDFs
        super.close();
    }

    private Product createProduct(File inputFile) {
        final String name = "SPOT_VGT"; // TODO - read from phys vol
        final String type = "SPOT_VGT"; // TODO - read from phys vol

        final Product product = new Product(name, type, sceneRasterWidth, sceneRasterHeight, this);
        product.setFileLocation(inputFile);

        return product;
    }

}
