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
package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;


// import org.opengis.filter.spatial.Equals;

public class SeadasProductReader extends AbstractProductReader {

    private NetcdfFile ncfile;
    private ProductType productType;
    private SeadasFileReader seadasFileReader;


    enum ProductType {
        ANCNRT("SeaWiFS Near Real-Time Ancillary Data"),
        ANCCLIM("SeaWiFS Climatological Ancillary Data"),
        Bathy("Bathymetry"),
        BrowseFile("Browse Product"),
        Level1A_Aquarius("Aquarius Level 1A"),
        Level2_Aquarius("Aquarius Level 2"),
        Level1A_CZCS("CZCS Level 1A"),
        Level2_CZCS("Level 2"),
        Level1A_OCTS("OCTS Level 1A"),
        Level1A_Seawifs("SeaWiFS Level 1A"),
        Level1B("Generic Level 1B"),
        Level1B_HICO("HICO L1B"),
        Level1B_Modis("MODIS Level 1B"),
        Level1B_OCM2("OCM2_L1B"),
        Level2("Level 2"),
        Level3_Bin("Level 3 Binned"),
        MEaSUREs("MEaSUREs Mapped"),
        MEaSUREs_Bin("MEaSUREs Binned"),
        OISST("Daily-OI"),
        SeadasMapped("SeaDAS Mapped"),
        SMI("Level 3 Mapped"),
        VIIRS_IP("VIIRS IP"),
        VIIRS_SDR("VIIRS SDR"),
        VIIRS_EDR("VIIRS EDR"),
        VIIRS_GEO("VIIRS GEO"),
        UNKNOWN("WHATUTALKINBOUTWILLIS");


        private String name;

        private ProductType(String nm) {
            name = nm;
        }

        public String toString() {
            return name;
        }
    }


    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected SeadasProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        try {
//            Product product;
            final File inFile = getInputFile(getInput());
            final String path = inFile.getPath();

            ncfile = NetcdfFileOpener.open(path);
            productType = findProductType();

            switch (productType) {
                case Level1A_Aquarius:
                case Level2_Aquarius:
                    seadasFileReader = new AquariusL2FileReader(this);
                    break;
                case Level2:
                case Level1B:
                case Level1A_CZCS:
                case Level2_CZCS:
                    seadasFileReader = new L2FileReader(this);
                    break;
                case Level1A_OCTS:
                    seadasFileReader = new L1AOctsFileReader(this);
                    break;
                case Level1A_Seawifs:
                    seadasFileReader = new L1ASeawifsFileReader(this);
                    break;
                case Level1B_Modis:
                    seadasFileReader = new L1BModisFileReader(this);
                    break;
                case Level1B_HICO:
                    seadasFileReader = new L1BHicoFileReader(this);
                    break;
                case Level1B_OCM2:
                    seadasFileReader = new L1BOcm2FileReader(this);
                    break;
                case Level3_Bin:
                    seadasFileReader = new L3BinFileReader(this);
                    break;
                case MEaSUREs_Bin:
                    seadasFileReader = new MeasuresL3BinFileReader(this);
                    break;
                case BrowseFile:
                    seadasFileReader = new BrowseProductReader(this);
                    break;
                case SMI:
                case ANCNRT:
                case ANCCLIM:
                case OISST:
                case Bathy:
                case MEaSUREs:
                    seadasFileReader = new SMIFileReader(this);
                    break;
                case SeadasMapped:
                    seadasFileReader = new SeadasMappedFileReader(this);
                    break;
                case VIIRS_IP:
                case VIIRS_SDR:
                case VIIRS_EDR:
                case VIIRS_GEO:
                    seadasFileReader = new ViirsXDRFileReader(this);
                    break;
                case UNKNOWN:
                    throw new IOException("Unrecognized product type");
                default:
                    throw new IOException("Unrecognized product type");

            }

            return seadasFileReader.createProduct();

        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        if (getNcfile() != null) {
            getNcfile().close();
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {


        try {
            seadasFileReader.readBandData(destBand, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,destBuffer, pm);
        } catch (Exception e) {
            final ProductIOException exception = new ProductIOException(e.getMessage());
            exception.setStackTrace(e.getStackTrace());
            throw exception;
        }
    }

    public File getInputFile() {
        return SeadasProductReader.getInputFile(getInput());
    }

    public NetcdfFile getNcfile() {
        return ncfile;
    }

    public ProductType getProductType() {
        return productType;
    }

    public boolean checkSeadasMapped() {
        try {
            List<Variable> seadasMappedVariables = ncfile.getVariables();
            return seadasMappedVariables.get(0).findAttribute("Projection_Category").isString();
        } catch (Exception e) {
            return false;
        }
    }

    public ProductType checkMEaSUREs() {
        try {
            Attribute indexes = ncfile.findGlobalAttribute("Data_Bins");
            if (indexes != null) {
                return ProductType.MEaSUREs_Bin;
            } else {
                return ProductType.MEaSUREs;
            }

        } catch (Exception ignored) {
        }
        return ProductType.UNKNOWN;
    }

    public boolean checkModisL1B() {
        Group modisl1bGroup = ncfile.findGroup("MODIS_SWATH_Type_L1B");
        return modisl1bGroup != null;
    }

    public ProductType checkViirsXDR() {
        Attribute platformShortName = ncfile.findGlobalAttribute("Platform_Short_Name");
        try {
            if (platformShortName.getStringValue().equals("NPP")) {
                Group dataProduct = ncfile.findGroup("Data_Products");
                if (dataProduct.getGroups().get(0).getShortName().matches("VIIRS.*IP")) {
                    return ProductType.VIIRS_IP;
                }
                if (dataProduct.getGroups().get(0).getShortName().matches("VIIRS.*SDR")) {
                    return ProductType.VIIRS_SDR;
                }
                if (dataProduct.getGroups().get(0).getShortName().matches("VIIRS.*EDR")) {
                    return ProductType.VIIRS_EDR;
                }
                if (dataProduct.getGroups().get(0).getShortName().matches("VIIRS.*GEO.*")) {
                    return ProductType.VIIRS_GEO;
                }
            }

        } catch (Exception ignored) {
        }
        return ProductType.UNKNOWN;
    }

    public ProductType findProductType() throws ProductIOException {
        Attribute titleAttr = ncfile.findGlobalAttributeIgnoreCase("Title");
        String title;
        ProductType tmp;
        if (titleAttr != null) {
            title = titleAttr.getStringValue().trim();
            if (title.equals("Oceansat OCM2 Level-1B Data")) {
                return ProductType.Level1B_OCM2;
            } else if (title.equals("CZCS Level-2 Data")) {
                return ProductType.Level2_CZCS;
            } else if (title.contains("Aquarius Level 1A Data")) {
                return ProductType.Level1A_Aquarius;
            } else if (title.contains("Aquarius Level 2 Data")) {
                return ProductType.Level2_Aquarius;
            } else if (title.contains("Level-1B")) {
                return ProductType.Level1B;
            } else if (title.equals("CZCS Level-1A Data")) {
                return ProductType.Level1A_CZCS;
            } else if (title.equals("OCTS Level-1A GAC Data")) {
                return ProductType.Level1A_OCTS;
            } else if (title.contains("Browse")) {
                return ProductType.BrowseFile;
            } else if (title.contains("Level-2")) {
                return ProductType.Level2;
            } else if (title.contains("Level 2")) {
                return ProductType.Level2;
            } else if (title.equals("SeaWiFS Level-1A Data")) {
                return ProductType.Level1A_Seawifs;
            } else if (title.contains("Daily-OI")) {
                return ProductType.OISST;
            } else if (title.contains("ETOPO")) {
                return ProductType.Bathy;
            } else if (title.equals("SeaWiFS Near Real-Time Ancillary Data")) {
                return ProductType.ANCNRT;
            } else if (title.equals("SeaWiFS Climatological Ancillary Data")) {
                return ProductType.ANCCLIM;
            } else if (title.contains("Level-3 Standard Mapped Image")) {
                return ProductType.SMI;
            } else if (title.contains("Level-3 Binned Data")) {
                return ProductType.Level3_Bin;
            } else if (title.contains("GSM") && (tmp = checkMEaSUREs()) != ProductType.UNKNOWN) {
                return tmp;
            }

        } else if (checkModisL1B()) {
            return ProductType.Level1B_Modis;
        } else if (checkHicoL1B()) {
            return ProductType.Level1B_HICO;
        } else if ((tmp = checkViirsXDR()) != ProductType.UNKNOWN) {
            return tmp;
        } else if (checkSeadasMapped()) {
            return ProductType.SeadasMapped;
        }

        throw new ProductIOException("Unrecognized product type");

    }

    private boolean checkHicoL1B() {
        Attribute hicol1bName = ncfile.findGlobalAttribute("metadata_FGDC_Instrument_Information_Instrument_Name");
        return hicol1bName != null;
    }

    public static File getInputFile(Object input) {
        File inputFile;
        if (input instanceof File) {
            inputFile = (File) input;
        } else if (input instanceof String) {
            inputFile = new File((String) input);
        } else {
            return null;
        }
        return inputFile;
    }

}
