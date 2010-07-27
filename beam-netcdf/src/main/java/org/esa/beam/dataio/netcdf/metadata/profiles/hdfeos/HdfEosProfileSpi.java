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

package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.metadata.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContextImpl;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.dataio.netcdf.util.VariableMap;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.io.BeamFileFilter;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;


public class HdfEosProfileSpi extends AbstractProfileSpi {

    private static final String[] FILE_EXTENSIONS = new String[]{
            Constants.FILE_EXTENSION_HDF, Constants.FILE_EXTENSION_HDF_GZ
    };

    @Override
    public ProfilePart createBandPart() {
        return new HdfEosBandPart();
    }

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePart createFlagCodingPart() {
        return null;
    }

    @Override
    public ProfilePart createGeocodingPart() {
        return new HdfEosGeocodingPart();
    }

    @Override
    public ProfilePart createImageInfoPart() {
        return null;
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return null;
    }

    @Override
    public ProfilePart createMaskPart() {
        return null;
    }

    @Override
    public ProfilePart createStxPart() {
        return null;
    }

    @Override
    public ProfilePart createTiePointGridPart() {
        return null;
    }

    @Override
    public ProfilePart createTimePart() {
        return new HdfEosTimePart();
    }

    @Override
    public ProfilePart createDescriptionPart() {
        return new HdfEosDescriptionPart();
    }

    @Override
    public ProfilePart createMetadataPart() {
        return new HdfEosMetadataPart();
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter("HDF EOS", FILE_EXTENSIONS, "HDF-EOS products");
    }

    @Override
    public ProfileReadContext createReadContext(NetcdfFile netcdfFile) throws IOException {
        Group eosGroup = netcdfFile.getRootGroup();
        Element eosStructElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, eosGroup);
        String gridName = getGridName(eosStructElement);
        if (gridName == null || gridName.isEmpty()) {
            throw new ProductIOException("Could not find grid.");
        }
        Group gridGroup = HdfEosUtils.findGroupNested(eosGroup, gridName);
        if (gridGroup == null) {
            throw new ProductIOException("Could not find grid group.");
        }
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(gridGroup);
        Variable[] rasterVariables = rasterDigest.getRasterVariables();
        VariableMap variableMap = new VariableMap(rasterVariables.length);
        for (Variable variable : rasterVariables) {
            variableMap.put(variable.getShortName(), variable);
        }
        ProfileReadContextImpl readContext = new ProfileReadContextImpl(netcdfFile, rasterDigest, variableMap);
        readContext.setProperty(HdfEosUtils.STRUCT_METADATA, eosStructElement);
        readContext.setProperty(HdfEosUtils.CORE_METADATA,
                                HdfEosUtils.getEosElement(HdfEosUtils.CORE_METADATA, eosGroup));
        readContext.setProperty(HdfEosUtils.ARCHIVE_METADATA,
                                HdfEosUtils.getEosElement(HdfEosUtils.ARCHIVE_METADATA, eosGroup));
        return readContext;
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        try {
            Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
            // check for GRID
            String gridName = getGridName(eosElement);
            if (gridName == null || gridName.isEmpty()) {
                return DecodeQualification.UNABLE;
            }
            //check for projection
            Element gridStructure = eosElement.getChild("GridStructure");
            Element gridElem = (Element) gridStructure.getChildren().get(0);
            Element projectionElem = gridElem.getChild("Projection");
            if (projectionElem == null) {
                return DecodeQualification.UNABLE;
            }
            String projection = projectionElem.getValue();
            if (projection.equals("GCTP_GEO") || projection.equals("GCTP_SNSOID")) {
                return DecodeQualification.SUITABLE;
            }
        } catch (Exception ignore) {
        }
        return DecodeQualification.UNABLE;
    }

    private String getGridName(Element eosElement) throws IOException {
        if (eosElement != null) {
            Element gridStructure = eosElement.getChild("GridStructure");
            if (gridStructure != null && gridStructure.getChildren() != null && gridStructure.getChildren().size() > 0) {
                Element gridElem = (Element) gridStructure.getChildren().get(0);
                if (gridElem != null) {
                    Element gridNameElem = gridElem.getChild("GridName");
                    if (gridNameElem != null) {
                        return gridNameElem.getText();
                    }
                }
            }
        }
        return null;
    }
}
