package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4RasterDigest;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.Nc4VariableMap;
import org.esa.beam.dataio.netcdf4.convention.AbstractModelFactory;
import org.esa.beam.dataio.netcdf4.convention.InitialisationPart;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfInitialisationPart;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOException;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;


public class HdfEosFactory extends AbstractModelFactory {

    @Override
    public ModelPart getBandPart() {
        return new HdfEosBandPart();
    }

    @Override
    public InitialisationPart getInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ModelPart getFlagCodingPart() {
        return null;
    }

    @Override
    public ModelPart getGeocodingPart() {
        return new HdfEosGeocodingPart();
    }

    @Override
    public ModelPart getImageInfoPart() {
        return null;
    }

    @Override
    public ModelPart getIndexCodingPart() {
        return null;
    }

    @Override
    public ModelPart getMaskOverlayPart() {
        return null;
    }

    @Override
    public ModelPart getStxPart() {
        return null;
    }

    @Override
    public ModelPart getTiePointGridPart() {
        return null;
    }

    @Override
    public ModelPart getStartTimePart() {
        return new HdfEosStartTimePart();
    }

    @Override
    public ModelPart getEndTimePart() {
        return new HdfEosEndTimePart();
    }

    @Override
    public ModelPart getDescriptionPart() {
        return new HdfEosDescriptionPart();
    }

    @Override
    public ModelPart getMetadataPart() {
        return new HdfEosMetadata();
    }

    @Override
    protected Nc4ReaderParameters createReaderParameters(NetcdfFile netcdfFile) throws IOException {
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
        return new Nc4ReaderParameters(netcdfFile, rasterDigest, nc4VariableMap);
    }

    @Override
    protected DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
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
            return DecodeQualification.INTENDED;
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
