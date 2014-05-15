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
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.netcdf.NcRasterDim;
import org.esa.nest.dataio.netcdf.NetCDFUtils;
import org.esa.nest.datamodel.Unit;
//import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.ma2.*;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NetCDF reader for Level-2 OCN products
 */
public class Sentinel1OCNReader {

    // This maps the MDS .nc file name to the NetcdfFile
    private final Map<String, NetcdfFile> bandNCFileMap = new HashMap<String, NetcdfFile>(1);

    private final Sentinel1Level1Directory dataDir;

    // For WV, there can be more than one MDS .nc file. See Table 4-3 in Product Spec v2/7 (S1-RS-MDA-52-7441).
    // Each MDS has same variables, so we want unique band names for variables of same name from different .nc file.
    // Then given a band name, we want to map back to the .nc file.
    private final Map<String, NetcdfFile> bandNameNCFileMap = new HashMap<String, NetcdfFile>(1);

    public Sentinel1OCNReader(final Sentinel1Level1Directory dataDir) {
        this.dataDir = dataDir;
    }

    public void addImageFile(final File file, final String name) throws IOException {
        final NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
        readNetCDF(netcdfFile);
        bandNCFileMap.put(name, netcdfFile);
    }

    private void readNetCDF(final NetcdfFile netcdfFile) {
        final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
        if (!variableListMap.isEmpty()) {
            final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);

            dataDir.setSceneWidthHeight(rasterDim.getDimX().getLength(), rasterDim.getDimY().getLength());
        }
    }

    public void addNetCDFMetadata(final Product product, final MetadataElement annotationElement) {
        final Set<String> files = bandNCFileMap.keySet();
        for (String file : files) {
            final NetcdfFile netcdfFile = bandNCFileMap.get(file);
            final MetadataElement bandElem = NetCDFUtils.addAttributes(annotationElement, file,
                    netcdfFile.getGlobalAttributes());

            //System.out.println("Sentinel1OCNReader.addNetCDFMetadata: file = " + file);

            final List<Variable> variableList = netcdfFile.getVariables();
            for (Variable variable : variableList) {
                bandElem.addElement(MetadataUtils.createMetadataElement(variable, 1000));
            }

            /*
            final ProfileReadContext context = new ProfileReadContextImpl(netcdfFile);
            final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
            if (rasterDigest == null) {
                return;
            }
            context.setRasterDigest(rasterDigest);

            if (product.getSceneRasterWidth() > 0 && product.getSceneRasterHeight() > 0) {
                CfBandPart bandReader = new CfBandPart();
                try {
                    bandReader.decode(context, product);
                } catch (Exception e) {

                }
            }
            */

            // Find index of 3rd '-'
            int idx = -1;
            for (int i = 0; i < 3; i++) {
                idx = file.indexOf('-', idx+1);
            }

            final String pol = file.substring(idx+1, idx+3);

            idx = file.lastIndexOf('-');
            final String imageNum = file.substring(idx+1, idx+4);


            for (Variable variable : variableList) {

                if (variable.getRank() == 2) {

                    final String bandName = pol + "_" + imageNum + "_" + variable.getFullName();

                    addBand(product, variable, bandName);
                    bandNameNCFileMap.put(bandName, netcdfFile);
                }
            }

            /*
            List<Dimension> dimensionList = netcdfFile.getDimensions();
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

       /*     final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
            if (!variableListMap.isEmpty()) {
                // removeQuickLooks(variableListMap);

                final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);
                final Variable[] rasterVariables = NetCDFUtils.getRasterVariables(variableListMap, rasterDim);
                final Variable[] tiePointGridVariables = NetCDFUtils.getTiePointGridVariables(variableListMap, rasterVariables);
                final NcVariableMap variableMap = new NcVariableMap(rasterVariables);

                for (final Variable variable : variableMap.getAll()) {
                    NetCDFUtils.addAttributes(bandElem, variable.getName(), variable.getAttributes());
                }

                // add bands
                addBandsToProduct(product, rasterVariables);
            } */
        }
    }

    private void addBandsToProduct(final Product product, final Variable[] variables) {
        int cnt = 1;
        for (Variable variable : variables) {
            final int height = variable.getDimension(0).getLength();
            final int width = variable.getDimension(1).getLength();
            String cntStr = "";
            if (variables.length > 1) {
                final String polStr = "pol";//getPolarization(product, cnt);
                if (polStr != null) {
                    cntStr = "_" + polStr;
                } else {
                    cntStr = "_" + cnt;
                }
                ++cnt;
            }

       /*     if(isComplex) {     // add i and q
                final Band bandI = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandI, "i"+cntStr);
                bandI.setUnit(Unit.REAL);
                product.addBand(bandI);
                bandMap.put(bandI, variable);

                final Band bandQ = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandQ, "q"+cntStr);
                bandQ.setUnit(Unit.IMAGINARY);
                product.addBand(bandQ);
                bandMap.put(bandQ, variable);

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
                ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, cntStr);
            } else { */
            final Band band = NetCDFUtils.createBand(variable, width, height);
            //  createUniqueBandName(product, band, "Amplitude"+cntStr);
            band.setUnit(Unit.AMPLITUDE);
            product.addBand(band);
            //     bandMap.put(band, variable);
            SARReader.createVirtualIntensityBand(product, band, cntStr);
            //  }
        }
    }

    private void addBand(final Product product, final Variable variable, final String bandName) {

        int[] shape = variable.getShape();

        //  shape[1] is width, shape[0] is height
        final Band band = NetCDFUtils.createBand(variable, shape[1], shape[0]);

        band.setName(bandName);

        product.addBand(band);

        try {
            Array arr = variable.read();

            /*
            for (int i = 0; i < arr.getSize(); i++) {
                System.out.println("Sentinel1OCNReader.addBand: " + variable.getFullName() + "[" + i + "] = " + arr.getFloat(i));
            }
            */

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.addBand: failed to add variable " + variable.getFullName() + " as band " + bandName);
        }
    }

    private void addTiePointGridToProduct(final Variable variable, final Product product) {

        // This does not work

        int[] shape = variable.getShape();

        //  shape[1] is width, shape[0] is height

        //System.out.println("Sentinel1OCNReader.addTiePointGridToProduct for " + variable.getFullName() + " w = " + shape[1] + " h = " + shape[0]);

        try {

            final TiePointGrid tiePointGrid = NetCDFUtils.createTiePointGrid(variable, shape[1], shape[0], product.getSceneRasterWidth(), product.getSceneRasterHeight());

            product.addTiePointGrid(tiePointGrid);

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.addTiePointGridToProduct: failed for " + variable.getFullName());
        }
    }

    public void readData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
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
                " destHeight = " + destHeight);
        */

        final String bandName = destBand.getName();

        final NetcdfFile netcdfFile = bandNameNCFileMap.get(bandName);

        final int idx = bandName.lastIndexOf('_');
        final String varFullName = bandName.substring(idx+1);

        //System.out.println("Sentinel1OCNReader.readData: bandName = " + bandName + " varFullName = " + varFullName);

        final Variable var = netcdfFile.findVariable(varFullName);

        final int[] origin = {sourceOffsetY, sourceOffsetX};
        final int[] shape = {sourceHeight*sourceStepY, sourceWidth*sourceStepX};

        try {

            final Array srcArray = var.read(origin, shape);

            for (int i = 0; i < destHeight; i++) {

                for (int j = 0; j < destWidth; j++) {

                    final int destIdx = i*destWidth + j;
                    final int srcIdx = i*shape[1] + j*sourceStepX;
                    destBuffer.setElemFloatAt(destIdx, srcArray.getFloat(srcIdx));

                    //System.out.println("Sentinel1OCNReader.readData:  i = " + i + " j = " + j + " destIdx = " + destIdx + " srcIdx = " + srcIdx);
                }
            }

        } catch (IOException e) {

            System.out.println("Sentinel1OCNReader.readData: IOException when reading variable " + varFullName);

        } catch (InvalidRangeException e) {

            System.out.println("Sentinel1OCNReader.readData: InvalidRangeException when reading variable " + varFullName);
        }
    }
}
