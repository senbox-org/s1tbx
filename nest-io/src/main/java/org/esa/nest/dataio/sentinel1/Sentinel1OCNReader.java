/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileReadContextImpl;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.netcdf.NcRasterDim;
import org.esa.nest.dataio.netcdf.NetCDFUtils;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

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
    private final Map<String, NetcdfFile> bandNCFileMap = new HashMap<String, NetcdfFile>(1);
    private final Sentinel1ProductDirectory dataDir;

    public Sentinel1OCNReader(final Sentinel1ProductDirectory dataDir) {
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
        for(String file : files) {
            final NetcdfFile netcdfFile = bandNCFileMap.get(file);
            final MetadataElement bandElem = NetCDFUtils.addAttributes(annotationElement, file,
                    netcdfFile.getGlobalAttributes());

            final List<Variable> variableList = netcdfFile.getVariables();
            for (Variable variable : variableList) {
                bandElem.addElement(MetadataUtils.createMetadataElement(variable));
            }

            final ProfileReadContext context = new ProfileReadContextImpl(netcdfFile);
            final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
            if (rasterDigest == null) {
                return;
            }
            context.setRasterDigest(rasterDigest);

            if(product.getSceneRasterWidth() > 0 && product.getSceneRasterHeight() > 0) {
                CfBandPart bandReader = new CfBandPart();
                try {
                    bandReader.decode(context, product);
                } catch(Exception e) {

                }
            }

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
            if(variables.length > 1) {
                final String polStr = "pol";//getPolarization(product, cnt);
                if(polStr != null) {
                    cntStr = "_"+polStr;
                } else {
                    cntStr = "_"+cnt;
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
                ReaderUtils.createVirtualIntensityBand(product, band, cntStr);
          //  }
        }
    }
}
