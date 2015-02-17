/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.datamodel.*;
import org.esa.nest.dataio.netcdf.NetCDFUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * NetCDF reader for Level-2 OCN products
 */
public class Sentinel1OCNReader {

    // A NetCDF file consists of
    // 1) attributes
    //      Global Attributes are added to Product as attributes in
    //          Metadata --> Original_Product_Metadata --> annotation --> <filename of MDS>.nc
    // 2) dimensions
    //      Dimensions are added to Product as attributes in
    //          Metadata --> Original_Product_Metadata --> annotation --> <filename of MDS>.nc
    // 3) variables
    //      - measurement data
    //          If rank is 1, it is added as "Values" in annotation for that variable, e.g., see oswK
    //          If rank > 1 but it is a vector of values, it is added as "values" in annotation for that variable
    //          If rank is 2, it is added as a band
    //          If rank is 3, it is added as a band. An "outer" grid of cells and then each cell is a single column of values.
    //          If rank is 4, it is added as a band. An "outer" grid of cells and then each cell is an "inner" grid of bins.
    //      - annotations
    //          scalar or 1D array with same name as variable
    //          Added to product as attributes in
    //          Metadata --> Original_Product_Metadata --> annotation --> <filename of MDS>.nc
    //
    // MDS = Measurement Data Set

    // This maps the MDS .nc file name to the NetcdfFile
    private final Map<String, NetcdfFile> bandNCFileMap = new HashMap<>(1);

    private final Sentinel1Level2Directory dataDir;

    // For WV, there can be more than one MDS .nc file. See Table 4-3 in Product Spec v2/7 (S1-RS-MDA-52-7441).
    // Each MDS has the same variables, so we want unique band names for variables of same name from different .nc file.
    // Given a band name, we want to map back to the .nc file.
    private final Map<String, NetcdfFile> bandNameNCFileMap = new HashMap<>(1);

    public Sentinel1OCNReader(final Sentinel1Level2Directory dataDir) {

        this.dataDir = dataDir;
    }

    public void addImageFile(final File file, final String name) throws IOException {

        // The image file here is the MDS .nc file.

        final NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
        bandNCFileMap.put(name, netcdfFile);
    }

    public void addNetCDFMetadata(final MetadataElement annotationElement) {

        final Set<String> files = bandNCFileMap.keySet();

        for (String file : files) { // for each MDS which is a .nc file

            //System.out.println("Sentinel1OCNReader.addNetCDFMetadataAndBands: file = " + file);

            final NetcdfFile netcdfFile = bandNCFileMap.get(file);

            // Add Global Attributes as Metadata
            final MetadataElement bandElem = NetCDFUtils.addAttributes(annotationElement,
                    file,
                    netcdfFile.getGlobalAttributes());

            // Add dimensions as Metadata
            final MetadataElement dimElem = new MetadataElement("Dimensions");
            bandElem.addElement(dimElem);
            List<Dimension> dimensionList = netcdfFile.getDimensions();
            for (Dimension d : dimensionList) {
                ProductData productData;
                productData = ProductData.createInstance(ProductData.TYPE_UINT32, 1);
                productData.setElemUInt(d.getLength());
                final MetadataAttribute metadataAttribute = new MetadataAttribute(d.getFullName(), productData, true);
                dimElem.addAttribute(metadataAttribute);
            }

            final List<Variable> variableList = netcdfFile.getVariables();

            // Add attributes inside variables as Metadata
            for (Variable variable : variableList) {
                bandElem.addElement(MetadataUtils.createMetadataElement(variable, 1000));
            }

            for (Variable variable : variableList) {

                if (variableIsVector(variable) && variable.getRank() > 1) {

                    // If rank is 1 then it has already been taken care of by
                    // MetadataUtils.createMetadataElement()

                    final MetadataElement elem = bandElem.getElement(variable.getFullName());
                    final MetadataElement valuesElem = new MetadataElement("Values");
                    elem.addElement(valuesElem);
                    MetadataUtils.addAttribute(variable, valuesElem, 1000);
                }
            }

            /*
            for (Dimension d : dimensionList) {
                int len = d.getLength();
                String name = d.getFullName();
                System.out.println("Sentinel1OCNReader.addNetCDFMetadata: dim name = " + name + " len = " + len);
            }

            for (Variable variable : variableList) {
                int[] varShape = variable.getShape();
                System.out.print("Sentinel1OCNReader.addNetCDFMetadata: variable name = " + variable.getFullName() + " ");
                for (int i = 0; i < varShape.length; i++) {
                    System.out.print(varShape[i] + " ");
                }
                System.out.println();
            }
            */
        }
    }

    public void addNetCDFBands(final Product product) {

        final Set<String> files = bandNCFileMap.keySet();

        for (String file : files) { // for each MDS which is a .nc file

            final NetcdfFile netcdfFile = bandNCFileMap.get(file);

            final List<Variable> variableList = netcdfFile.getVariables();

            // Add bands to product...

            // Find index of 3rd '-'
            int idx = -1;
            for (int i = 0; i < 3; i++) {
                idx = file.indexOf('-', idx + 1);
            }

            final String pol = file.substring(idx + 1, idx + 3);

            idx = file.lastIndexOf('-');
            final String imageNum = file.substring(idx + 1, idx + 4);

            for (Variable variable : variableList) {

                if (variableIsVector(variable) && variable.getRank() > 1) {
                    continue;
                }

                final String bandName = pol + "_" + imageNum + "_" + variable.getFullName();
                final int[] shape = variable.getShape();

                switch (variable.getRank()) {

                    case 1:
                        // The data has been added as part of annotation for the variable under "Values".
                        break;
                    case 2: {
                        addBand(product, bandName, variable, shape[1], shape[0]);
                        bandNameNCFileMap.put(bandName, netcdfFile);
                        /*
                        if (bandName.contains("rvlLon")) {
                            dumpVariableValues(variable, bandName);
                        }
                        */
                    }
                    break;
                    case 3:
                        // When the rank is 3, there is an "outer" grid of cells and each cell contains a vector of values.
                        // The "outer" grid is oswAzSize (rows) by oswRaSize (cols) of cells.
                        // Each cell is a vector of values.
                        // For owsSpecRes, the dimensions are oswAzSize x oswRaSize x oswAngularBinSize
                        // So it is more natural to have the band be (oswAzSize*oswAngularBinSize) rows by oswRaSize columns.
                        // All other rank 3 variables are oswAzSize x oswRaSize x oswPartitions
                        // To be consistent, the bands will be (oswAzSize*oswPartitions) rows by oswRaSize columns.
                    {
                        // Tbe band will have dimensions: shape[0]*shape[2] (rows) by shape[1] (cols).
                        // So band width = shape[1] and band height = shape[0]*shape[2]
                        addBand(product, bandName, variable, shape[1], shape[0] * shape[2]);
                        bandNameNCFileMap.put(bandName, netcdfFile);
                        /*
                        if (bandName.contains("oswSpecRes")) {
                            dumpVariableValues(variable, bandName);
                        }
                        */
                    }
                    break;

                    case 4:
                        // When the rank is 4, there is an "outer" grid of cells and each cell contains an "inner" grid of bins.
                        // The "outer" grid is oswAzSize (rows) by oswRaSize (cols) of cells.
                        // Each cell is oswAngularBinSize (rows) by oswWaveNumberBinSize (cols) of bins.
                        // shape[0] is height of "outer" grid.
                        // shape[1] is width of "outer" grid.
                        // shape[2] is height of "inner" grid.
                        // shape[3] is width of "inner" grid.
                    {
                        // Tbe band will have dimensions: shape[0]*shape[2] (rows) by shape[1]*shape[3] (cols).
                        // So band width = shape[1]*shape[3] and band height = shape[0]*shape[2]
                        addBand(product, bandName, variable, shape[1] * shape[3], shape[0] * shape[2]);
                        bandNameNCFileMap.put(bandName, netcdfFile);
                        /*
                        if (bandName.contains("oswPolSpec")) {
                            dumpVariableValues(variable, bandName);
                        }
                        */
                    }
                    break;
                    default:
                        System.out.println("SentinelOCNReader.addNetCDFMetadataAndBands: ERROR invalid variable rank " + variable.getRank() + " for " + variable.getFullName());
                        break;
                }
            }
        }
    }

    public void addGeoCodingToBands(final Product product) {

    /*    final Band[] bands = product.getBands();

        for (Band band : bands) {

            final String bandName = band.getName();
            if (bandName.substring(7).equals("rvlRadVel") ||
                bandName.substring(7).equals("owiWindSpeed")    ) {

                final String bandNamePrefix = bandName.substring(0,10);
                final Band latBand = product.getBand(bandNamePrefix + "Lat");
                final Band lonBand = product.getBand(bandNamePrefix + "Lon");

                if (latBand == null || lonBand == null) {
                    System.out.println("Sentinel1OCNReader.addDisplayBands: missing " + bandName + " Lat and/or Lon: latBand is " + latBand + " lonBand is " + lonBand);
                    continue;
                }

                final int searchRadius = 5; // TODO No idea what this should be
                PixelGeoCoding pixGeoCoding = new PixelGeoCoding(latBand, lonBand, null, searchRadius);
                band.setGeoCoding(pixGeoCoding);
            }
        }
*/
    }

    private void addBand(final Product product, String bandName, final Variable variable, final int width, final int height) {

        final Band band = NetCDFUtils.createBand(variable, width, height);
        band.setName(bandName);
        product.addBand(band);
    }

    public synchronized void readData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                         int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                         int destOffsetY, int destWidth, int destHeight, ProductData destBuffer) {

        /*
        System.out.println("Sentinel1OCNReader.readData: sourceOffsetX = " + sourceOffsetX +
                " sourceOffsetY = " + sourceOffsetY +
                " sourceWidth = " + sourceWidth +
                " sourceHeight = " +  sourceHeight +
                " sourceStepX = " + sourceStepX +
                " sourceStepY = " + sourceStepY +
                " destOffsetX = " + destOffsetX +
                " destOffsetY = " + destOffsetY +
                " destWidth = " + destWidth +
                " destHeight = " + destHeight +
                " destBuffer.getNumElems() = " + destBuffer.getNumElems());
        */

        // Can source and destination have different height and width? TODO
        if (sourceWidth != destWidth || sourceHeight != destHeight) {

            System.out.println("Sentinel1OCNReader.readData: ERROR sourceWidth = " + sourceWidth + " sourceHeight = " + sourceHeight);
            return;
        }

        // It looks like this will be called once for the entire band at the beginning to fill up the display
        // and then when we slide the cursor over each pixel, this is called again just for that pixel.
        // In the former case, we see this print statement...
        //  Sentinel1OCNReader.readData: sourceOffsetX = 0 sourceOffsetY = 0 sourceWidth = 80 sourceHeight = 10 sourceStepX = 1 sourceStepY = 1 destOffsetX = 0 destOffsetY = 0 destWidth = 80 destHeight = 10 destBuffer.getNumElems() = 800
        // In the latter case, we see this print statement...
        //  Sentinel1OCNReader.readData: sourceOffsetX = 32 sourceOffsetY = 4 sourceWidth = 1 sourceHeight = 1 sourceStepX = 1 sourceStepY = 1 destOffsetX = 32 destOffsetY = 4 destWidth = 1 destHeight = 1 destBuffer.getNumElems() = 1
        // So it looks like we can ignore destOffsetX and destOffsetY.

        final String bandName = destBand.getName();

        final NetcdfFile netcdfFile = bandNameNCFileMap.get(bandName);

        final int idx = bandName.lastIndexOf('_');
        final String varFullName = bandName.substring(idx + 1);

        //System.out.println("Sentinel1OCNReader.readData: bandName = " + bandName + " varFullName = " + varFullName);

        final Variable var = netcdfFile.findVariable(varFullName);

        switch (var.getRank()) {
            case 2:
                readDataForRank2Variable(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY, var, destWidth, destHeight, destBuffer);
                break;
            case 3:
                readDataForRank3Variable(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY, var, destWidth, destHeight, destBuffer);
                break;
            case 4:
                readDataForRank4Variable(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY, var, destWidth, destHeight, destBuffer);
                break;
        }
    }

    public void readDataForRank2Variable(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                         int sourceStepX, int sourceStepY, Variable var,
                                         int destWidth, int destHeight, ProductData destBuffer) {

        final int[] origin = {sourceOffsetY, sourceOffsetX};
        final int[] shape = {(sourceHeight - 1) * sourceStepY + 1, (sourceWidth - 1) * sourceStepX + 1};
        // ADDED
        /*
        System.out.println(":::::" + sourceOffsetX + " " + sourceOffsetY);
        System.out.println(":::::" + sourceStepX + " " + sourceStepY);
        System.out.println(":::::" + sourceWidth + " " + sourceHeight);
        System.out.println(":::::" + destWidth + " " + destHeight);
        */
        try {

            final Array srcArray = var.read(origin, shape);

            for (int i = 0; i < destHeight; i++) {

                for (int j = 0; j < destWidth; j++) {

                    final int destIdx = i * destWidth + j;
                    final int srcIdx = i * sourceStepY * shape[1] + j * sourceStepX;
                    destBuffer.setElemFloatAt(destIdx, srcArray.getFloat(srcIdx));

                    //System.out.println("Sentinel1OCNReader.readData:  i = " + i + " j = " + j + " destIdx = " + destIdx + " srcIdx = " + srcIdx);
                }
            }

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank2Variable: IOException when reading variable " + var.getFullName());

        } catch (InvalidRangeException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank2Variable: InvalidRangeException when reading variable " + var.getFullName());
        }
    }

    private void readDataForRank3Variable(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Variable var,
                                          int destWidth, int destHeight, ProductData destBuffer) {

        final int[] shape0 = var.getShape();

        // shape0[0] is height of "outer" grid.
        // shape0[1] is width of "outer" grid.
        // shape0[2] is height of the column in each cell in the "outer" grid.

        final int[] origin = {sourceOffsetY / shape0[2], sourceOffsetX, 0};

        final int outerYEnd = (sourceOffsetY + (sourceHeight - 1) * sourceStepY) / shape0[2];
        final int outerXEnd = (sourceOffsetX + (sourceWidth - 1) * sourceStepX);

        final int[] shape = {outerYEnd - origin[0] + 1, outerXEnd - origin[1] + 1, shape0[2]};

        try {

            final Array srcArray = var.read(origin, shape);

            for (int i = 0; i < destHeight; i++) {

                // srcY is wrt to what is read in srcArray
                final int srcY = (sourceOffsetY - shape0[2] * origin[0]) + i * sourceStepY;

                for (int j = 0; j < destWidth; j++) {

                    // srcX is wrt to what is read in srcArray
                    final int srcX = j * sourceStepX;

                    final int[] idx = new int[3];
                    idx[0] = srcY / shape[2];
                    idx[1] = srcX;
                    idx[2] = srcY - idx[0] * shape[2];

                    final int srcIdx = (idx[0] * shape[1] * shape[2]) +
                            (idx[1] * shape[2]) +
                            idx[2];

                    final int destIdx = i * destWidth + j;

                    destBuffer.setElemFloatAt(destIdx, srcArray.getFloat(srcIdx));
                }
            }

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank3Variable: IOException when reading variable " + var.getFullName());

        } catch (InvalidRangeException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank3Variable: InvalidRangeException when reading variable " + var.getFullName());
        }
    }

    private void readDataForRank4Variable(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Variable var,
                                          int destWidth, int destHeight, ProductData destBuffer) {

        final int[] shape0 = var.getShape();

        // shape0[0] is height of "outer" grid.
        // shape0[1] is width of "outer" grid.
        // shape0[2] is height of "inner" grid.
        // shape0[3] is width of "inner" grid.

        final int[] origin = {sourceOffsetY / shape0[2], sourceOffsetX / shape0[3], 0, 0};

        //System.out.println("sourceOffsetY = " + sourceOffsetY + " shape0[2] = " + shape0[2] + " sourceOffsetX = " + sourceOffsetX + " shape0[3] = " + shape0[3]);
        //System.out.println("origin " + origin[0] + " " + origin[1]);

        final int outerYEnd = (sourceOffsetY + (sourceHeight - 1) * sourceStepY) / shape0[2];
        final int outerXEnd = (sourceOffsetX + (sourceWidth - 1) * sourceStepX) / shape0[3];

        //System.out.println("sourceHeight = " + sourceHeight + " sourceStepY = " + sourceStepY + " outerYEnd = " + outerYEnd);
        //System.out.println("sourceWidth = " + sourceWidth + " sourceStepX = " + sourceStepX + " outerXEnd = " + outerXEnd);

        final int[] shape = {outerYEnd - origin[0] + 1, outerXEnd - origin[1] + 1, shape0[2], shape0[3]};

        try {

            final Array srcArray = var.read(origin, shape);

            for (int i = 0; i < destHeight; i++) {

                // srcY is wrt to what is read in srcArray
                final int srcY = (sourceOffsetY - shape0[2] * origin[0]) + i * sourceStepY;

                for (int j = 0; j < destWidth; j++) {

                    // srcX is wrt to what is read in srcArray
                    final int srcX = (sourceOffsetX - shape0[3] * origin[1]) + j * sourceStepX;

                    final int[] idx = new int[4];
                    idx[0] = srcY / shape[2];
                    idx[1] = srcX / shape[3];
                    idx[2] = srcY - idx[0] * shape[2];
                    idx[3] = srcX - idx[1] * shape[3];

                    final int srcIdx = (idx[0] * shape[1] * shape[2] * shape[3]) +
                            (idx[1] * shape[2] * shape[3]) +
                            (idx[2] * shape[3]) +
                            idx[3];

                    final int destIdx = i * destWidth + j;

                    destBuffer.setElemFloatAt(destIdx, srcArray.getFloat(srcIdx));
                }
            }

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank4Variable: IOException when reading variable " + var.getFullName());

        } catch (InvalidRangeException e) {

            System.out.println("Sentinel1OCNReader.readDataForRank4Variable: InvalidRangeException when reading variable " + var.getFullName());
        }
    }

    private static boolean variableIsVector(Variable variable) {

        final int[] shape = variable.getShape();

        int cnt = 0;

        for (int i : shape) {
            if (i == 1) {
                cnt++;
            }
        }

        return cnt + 1 >= shape.length;
    }

    private void dumpVariableValues(final Variable variable, final String bandName) {

        try {
            Array arr = variable.read();

            for (int i = 0; i < arr.getSize(); i++) {
                System.out.println("Sentinel1OCNReader: " + variable.getFullName() + "[" + i + "] = " + arr.getFloat(i));
            }

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader: failed to read variable " + variable.getFullName() + " for band " + bandName);
        }

    }
}
