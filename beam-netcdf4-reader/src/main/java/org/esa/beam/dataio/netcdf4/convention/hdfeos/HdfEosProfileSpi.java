package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4RasterDigest;
import org.esa.beam.dataio.netcdf4.Nc4FileInfo;
import org.esa.beam.dataio.netcdf4.Nc4VariableMap;
import org.esa.beam.dataio.netcdf4.convention.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf4.convention.ProfileInitPart;
import org.esa.beam.dataio.netcdf4.convention.ProfilePart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfInitialisationPart;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOException;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;


public class HdfEosProfileSpi extends AbstractProfileSpi {

    @Override
    public ProfilePart getBandPart() {
        return new HdfEosBandPart();
    }

    @Override
    public ProfileInitPart getInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePart getFlagCodingPart() {
        return null;
    }

    @Override
    public ProfilePart getGeocodingPart() {
        return new HdfEosGeocodingPart();
    }

    @Override
    public ProfilePart getImageInfoPart() {
        return null;
    }

    @Override
    public ProfilePart getIndexCodingPart() {
        return null;
    }

    @Override
    public ProfilePart getMaskOverlayPart() {
        return null;
    }

    @Override
    public ProfilePart getStxPart() {
        return null;
    }

    @Override
    public ProfilePart getTiePointGridPart() {
        return null;
    }

    @Override
    public ProfilePart getStartTimePart() {
        return new HdfEosStartTimePart();
    }

    @Override
    public ProfilePart getEndTimePart() {
        return new HdfEosEndTimePart();
    }

    @Override
    public ProfilePart getDescriptionPart() {
        return new HdfEosDescriptionPart();
    }

    @Override
    public ProfilePart getMetadataPart() {
        return new HdfEosMetadata();
    }

    @Override
    protected Nc4FileInfo createFileInfo(NetcdfFile netcdfFile) throws IOException {
        Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
        String gridName = getGridName(eosElement);
        if (gridName == null || gridName.isEmpty()) {
            throw new ProductIOException("Could not find grid.");
        }
        Group gridGroup = HdfEosUtils.findGroupNested(netcdfFile.getRootGroup(), gridName);
        if (gridGroup == null) {
            throw new ProductIOException("Could not find grid group.");
        }
        Nc4RasterDigest rasterDigest = Nc4RasterDigest.createRasterDigest(gridGroup);
        Variable[] rasterVariables = rasterDigest.getRasterVariables();
        Nc4VariableMap nc4VariableMap = new Nc4VariableMap(rasterVariables.length);
        for (Variable variable : rasterVariables) {
            nc4VariableMap.put(variable.getShortName(), variable);
        }
        return new Nc4FileInfo(netcdfFile, rasterDigest, nc4VariableMap);
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
            if (!projection.equals("GCTP_GEO")) {
                return DecodeQualification.UNABLE;
            }
            return DecodeQualification.SUITABLE;
        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
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
