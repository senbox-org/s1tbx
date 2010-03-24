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

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.esa.beam.dataio.spot.SpotVgtConstants.HDF_FILTER;
import static org.esa.beam.dataio.spot.SpotVgtProductReaderPlugIn.getBandName;
import static org.esa.beam.dataio.spot.SpotVgtProductReaderPlugIn.getFileInput;

// todo - derive GeoCoding from productDescriptor:
/*
    MAP_PROJ_NAME             PLATE_CARREE_1KMG nom provisoirement egal au code
    MAP_PROJ_FAMILY           UNPROJECTED
    MAP_PROJ_CODE             PLATE_CARREE_1KMG
    MAP_PROJ_UNIT             DEGREES
    MAP_PROJ_RESOLUTION       0.0089285714
    GEODETIC_SYST_NAME        WGS 1984
    GEODETIC_SYST_CODE        WG84
    HORIZ_DATUM               WGS 1984
    MERIDIAN_NAME             GREENWICH
    MERIDIAN_ORIGIN           +000.000
    SPHEROID_NAME             WGS 1984
    SPHEROID_SEMI_MAJ_AXIS    6378137.000
    SPHEROID_SEMI_MIN_AXIS    6356752.314
    CARTO_UPPER_LEFT_X         -11.000000
    CARTO_UPPER_LEFT_Y          75.000000
    CARTO_UPPER_RIGHT_X         62.000000
    CARTO_UPPER_RIGHT_Y         75.000000
    CARTO_LOWER_RIGHT_X         62.000000
    CARTO_LOWER_RIGHT_Y         25.000000
    CARTO_LOWER_LEFT_X         -11.000000
    CARTO_LOWER_LEFT_Y          25.000000
    CARTO_CENTER_X              25.500000
    CARTO_CENTER_Y              50.000000
    CARTO_HEIGHT                50.000000
    CARTO_WIDTH                 73.000000
 */

// todo - set product start/stop time from productDescriptor:
/*
    SYNTHESIS_FIRST_DATE      20060720223132
    SYNTHESIS_LAST_DATE       20060730235628
*/

// todo - define FlagCoding and default Masks
// todo - define RGB profiles
// todo - set spectral band properties for B0, B2, B3, MIR
// todo - What is TG? Flags?

/**
 * Reader for SPOT VGT products.
 *
 * @author Norman Fomferra
 * @version 1.0
 */
public class SpotVgtProductReader extends AbstractProductReader {

    private HashMap<Band, FileVar> fileVars;

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
        File inputFile = getFileInput(getInput());

        PhysVolDescriptor physVolDescriptor = new PhysVolDescriptor(inputFile);

        String productDescriptorName = String.format("%04d_LOG", physVolDescriptor.getPhysVolNumber());
        File productDescriptorFile = new File(physVolDescriptor.getDataDir(), productDescriptorName + ".TXT");
        ProductDescriptor productDescriptor = new ProductDescriptor(productDescriptorFile);

        File[] hdfFiles = physVolDescriptor.getDataDir().listFiles(HDF_FILTER);

        fileVars = new HashMap<Band, FileVar>(33);

        Product product = null;
        for (File hdfFile : hdfFiles) {
            NetcdfFile netcdfFile = NetcdfFile.open(hdfFile.getPath());

            HashMap<String, Variable> variables = new HashMap<String, Variable>();
            for (Variable variable : netcdfFile.getVariables()) {
                variables.put(variable.getName(), variable);
            }

            Variable pixelDataVar = variables.get("PIXEL DATA");
            if (pixelDataVar == null) {
                pixelDataVar = variables.get("ANGLES_VALUES");
            }
            if (pixelDataVar != null && pixelDataVar.getRank() == 2 && pixelDataVar.getDataType().isNumeric()) {
                DataType netCdfDataType = pixelDataVar.getDataType();
                int bandDataType = ProductData.TYPE_UNDEFINED;
                if (Byte.TYPE == netCdfDataType.getPrimitiveClassType()) {
                    bandDataType = ProductData.TYPE_INT8;
                } else if (Short.TYPE == netCdfDataType.getPrimitiveClassType()) {
                    bandDataType = ProductData.TYPE_INT16;
                } else if (Integer.TYPE == netCdfDataType.getPrimitiveClassType()) {
                    bandDataType = ProductData.TYPE_INT32;
                } else if (Float.TYPE == netCdfDataType.getPrimitiveClassType()) {
                    bandDataType = ProductData.TYPE_FLOAT32;
                } else if (Double.TYPE == netCdfDataType.getPrimitiveClassType()) {
                    bandDataType = ProductData.TYPE_FLOAT64;
                }
                if (bandDataType != ProductData.TYPE_UNDEFINED) {
                    if (product == null) {
                        product = new Product(productDescriptor.getProductId(),
                                              physVolDescriptor.getFormatReference(),
                                              pixelDataVar.getDimension(1).getLength(),
                                              pixelDataVar.getDimension(0).getLength(), this);
                        product.setFileLocation(inputFile);
                    }
                    Band band = product.addBand(getBandName(hdfFile), bandDataType);
                    fileVars.put(band, new FileVar(netcdfFile, pixelDataVar));
                }
            }
        }

        product.getMetadataRoot().addElement(createMetadataElement("PHYS_VOL",
                                                                   physVolDescriptor.getPropertySet().getProperties()));
        product.getMetadataRoot().addElement(createMetadataElement(productDescriptorName, 
                                                                   productDescriptor.getPropertySet().getProperties()));

        return product;
    }

    private MetadataElement createMetadataElement(String name, Property[] properties) {
        MetadataElement physVolElement = new MetadataElement(name);
        for (Property property : properties) {
            physVolElement.addAttribute(new MetadataAttribute(property.getName(),
                                                              ProductData.createInstance(property.getValueAsText()), true));
        }
        return physVolElement;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
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

        FileVar fileVar = fileVars.get(targetBand);
        if (fileVar == null) {
            return;
        }
        final Variable variable = fileVar.var;
        synchronized (variable) {
            try {
                Array array = variable.read(new int[]{targetOffsetY, targetOffsetX},
                                            new int[]{targetHeight, targetWidth});
                System.arraycopy(array.getStorage(),
                                 0,
                                 targetBuffer.getElems(),
                                 0, targetWidth * targetHeight);
            } catch (InvalidRangeException e) {
                // ?
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<Band, FileVar> entry : fileVars.entrySet()) {
            NetcdfFile netcdfFile = entry.getValue().file;
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // ok
            }
        }
        fileVars.clear();
        super.close();
    }

    private static class FileVar {
        final NetcdfFile file;
        final Variable var;

        private FileVar(NetcdfFile file, Variable var) {
            this.file = file;
            this.var = var;
        }
    }

}
