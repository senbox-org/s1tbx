package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos.HdfEosUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ucar.nc2.NetcdfFile.open;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
 */
public class L1BModisFileReader extends SeadasFileReader {

    private static final int[] MODIS_WVL = new int[]{645, 859, 469, 555, 1240, 1640, 2130, 412, 443, 488, 531, 547, 667, 678,
            748, 869, 905, 936, 940, 3750, 3959, 3959, 4050, 4465, 4515, 1375, 6715, 7325, 8550, 9730, 11030, 12020,
            13335, 13635, 13935, 14235};

    L1BModisFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        addGlobalAttributeModisL1B();
        String productName = getStringAttribute("Product_Name");
        int pixelmultiplier = 1;
        int scanmultiplier = 10;

        for (Attribute attribute : globalAttributes) {
            if (attribute.getShortName().equals("MODIS_Resolution")) {
                String resolution = attribute.getStringValue();
                if (resolution.equals("500m")) {
                    pixelmultiplier = 2;
                    scanmultiplier = 20;
                } else if (resolution.equals("250m")) {
                    pixelmultiplier = 4;
                    scanmultiplier = 40;
                }
            }
        }
        int sceneWidth = getIntAttribute("Max_Earth_View_Frames", globalAttributes) * pixelmultiplier;
        int sceneHeight = getIntAttribute("Number_of_Scans", globalAttributes) * scanmultiplier;

        mustFlipX = mustFlipY = mustFlipMODIS();
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start_Time");
        if (utcStart != null) {
            if (mustFlipY) {
                product.setEndTime(utcStart);
            } else {
                product.setStartTime(utcStart);
            }
        }
        ProductData.UTC utcEnd = getUTCAttribute("End_Time");
        if (utcEnd != null) {
            if (mustFlipY) {
                product.setStartTime(utcEnd);
            } else {
                product.setEndTime(utcEnd);
            }
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addScientificMetadata(product);

        variableMap = addModisBands(product, ncFile.getVariables());
        addTiePointGrids(product, ncFile.getVariables());
        addGeocoding(product);

        // todo - think about maybe possibly sometime creating a flag for questionable data
//        addFlagsAndMasks(product);
        product.setAutoGrouping("RefSB:Emissive");

        return product;

    }

    private void addTiePointGrids(Product product, List<Variable> variables) throws ProductIOException {

        final int tpHeight = getDimension("MODIS_SWATH_Type_L1B_2*nscans");
        final int tpWidth = getDimension("MODIS_SWATH_Type_L1B_1KM_geo_dim");
        for (Variable variable : variables) {
            final int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                if (dimensions[0] != tpHeight || dimensions[1] != tpWidth) {
                    continue;   // sort out all variables that have different sizes
                }
                if (!variable.getGroup().getShortName().equals("Data_Fields")) {
                    continue;   // sort out variables from other groups
                }
                // Todo! Although deprecated in 4.3.16, the official documentation for 4.3.18 does not state this method as deprecated.
                // At the moment, we keep it as it runs - should be checked again when updating the NetCDF library. tb 2013-10-18
                if (variable.getDataType().getSize() != 2) {
                    continue;   // sort out everything that is not 16 bit
                }

                final float scale_factor = getScaleFactor(variable);
                try {

                    short[] float_data_raw;
                    Array dataArray = variable.read();
                    if (mustFlipX && mustFlipY) {
                        float_data_raw = (short[]) dataArray.flip(0).flip(1).copyTo1DJavaArray();
                    } else {
                        float_data_raw = (short[]) dataArray.getStorage();
                    }

                    final double[] float_data = new double[(int) dataArray.getSize()];
                    for (int i = 0; i < float_data.length; i++) {
                        float_data[i] = float_data_raw[i] * scale_factor;
                    }

                    int subSampling;
                    float offsetY;
                    String resolution = getStringAttribute("MODIS_Resolution");
                    if (resolution.equals("500m")) {
                        subSampling = 2;
                        offsetY = 0.5f;
                    } else if (resolution.equals("250m")) {
                        subSampling = 4;
                        offsetY = 1.5f;
                    } else {
                        subSampling = 5;
                        offsetY = 0f;
                    }

                    final TiePointGrid tiePointGrid = new TiePointGrid(variable.getShortName(), tpWidth, tpHeight, 0, offsetY,
                            subSampling, subSampling, float_data);
                    product.addTiePointGrid(tiePointGrid);

                } catch (IOException e) {
                    throw new ProductIOException(e.getMessage());
                }
            }
        }
    }

    private float getScaleFactor(Variable variable) {
        float scale_factor = 1.f;
        final Attribute scale_factor_attribute = findAttribute("scale_factor", variable.getAttributes());
        if (scale_factor_attribute != null) {
            scale_factor = scale_factor_attribute.getNumericValue().floatValue();
        }
        return scale_factor;
    }

    private Map<Band, Variable> addModisBands(Product product, List<Variable> variables) {

        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            final int variableRank = variable.getRank();
            if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[0];
                final int height = dimensions[1];
                final int width = dimensions[2];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    final List<Attribute> list = variable.getAttributes();
                    Attribute band_names = findAttribute("band_names", list);
                    if (band_names != null) {
                        String bnames = band_names.getStringValue();
                        String[] bname_array = bnames.split(",");
                        String units = null;
                        String description = null;
                        Array slope = null;
                        Array intercept = null;
                        for (Attribute hdfAttribute : list) {
                            final String attribName = hdfAttribute.getShortName();
                            if ("units".equals(attribName)) {
                                units = hdfAttribute.getStringValue();
                            } else if ("long_name".equals(attribName)) {
                                description = hdfAttribute.getStringValue();
                            } else if ("reflectance_scales".equals(attribName)) {
                                slope = hdfAttribute.getValues();
                            } else if ("reflectance_offsets".equals(attribName)) {
                                intercept = hdfAttribute.getValues();
                            } else if (slope == null && "radiance_scales".equals(attribName)) {
                                slope = hdfAttribute.getValues();
                            } else if (intercept == null && "radiance_offsets".equals(attribName)) {
                                intercept = hdfAttribute.getValues();
                            }
                        }

                        for (int i = 0; i < bands; i++) {
                            final String shortname = variable.getShortName();
                            StringBuilder longname = new StringBuilder(shortname);
                            longname.append("_");
                            longname.append(bname_array[i]);
                            String name = longname.toString();
                            final int dataType = getProductDataType(variable);
                            final Band band = new Band(name, dataType, width, height);

                            product.addBand(band);

                            int wvlidx;

                            if (bname_array[i].contains("lo") || bname_array[i].contains("hi")) {
                                wvlidx = Integer.parseInt(bname_array[i].substring(0, 2)) - 1;
                            } else {
                                wvlidx = Integer.parseInt(bname_array[i]) - 1;
                            }

                            final float wavelength = (float) MODIS_WVL[wvlidx];
                            band.setSpectralWavelength(wavelength);
                            band.setSpectralBandIndex(spectralBandIndex++);

                            Variable sliced = null;
                            try {
                                sliced = variable.slice(0, i);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }
                            bandToVariableMap.put(band, sliced);
                            band.setUnit(units);
                            band.setDescription(description);
                            if (slope != null) {
                                band.setScalingFactor(slope.getDouble(i));

                                if (intercept != null)
                                    band.setScalingOffset(-intercept.getDouble(i) * slope.getDouble(i));
                            }
                        }
                    }
                }
            }

        }
        return bandToVariableMap;
    }

    public void addGeocoding(final Product product) throws ProductIOException {

        // read latitudes and longitudes
        int cntl_lat_ix;
        int cntl_lon_ix;
        float offsetY;
        boolean externalGeo = false;
        NetcdfFile geoNcFile = null;
        Variable lats = null;
        Variable lons = null;
        int scanHeight;

        try {
            String resolution = getStringAttribute("MODIS_Resolution");
            if (resolution.equals("500m")) {
                scanHeight = 20;
                cntl_lat_ix = 2;
                cntl_lon_ix = 2;
                offsetY = 0.5f;
            } else if (resolution.equals("250m")) {
                scanHeight = 40;
                cntl_lat_ix = 4;
                cntl_lon_ix = 4;
                offsetY = 1.5f;
            } else {
                scanHeight = 10;
                cntl_lat_ix = 5;
                cntl_lon_ix = 5;
                offsetY = 0f;

                File inputFile = productReader.getInputFile();
                String geoFileName = getStringAttribute("Geolocation_File");
                String path = inputFile.getParent();
                File geocheck = new File(path, geoFileName);
                if (geocheck.exists()) {
                    externalGeo = true;
                    cntl_lat_ix = 1;
                    cntl_lon_ix = 1;
                    offsetY = 0f;
                    geoNcFile = NetcdfFileOpener.open(geocheck.getPath());
                }
            }

            if (externalGeo) {
                lats = geoNcFile.findVariable("MODIS_Swath_Type_GEO/Geolocation_Fields/Latitude");
                lons = geoNcFile.findVariable("MODIS_Swath_Type_GEO/Geolocation_Fields/Longitude");
            } else {
                lats = ncFile.findVariable("MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
                lons = ncFile.findVariable("MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
            }

            //Use lat/lon with TiePointGeoCoding
            int[] dims = lats.getShape();
            double[] latTiePoints;
            double[] lonTiePoints;
            Array latarr = lats.read();
            Array lonarr = lons.read();
            if (mustFlipX && mustFlipY) {
                latTiePoints = (double[]) latarr.flip(0).flip(1).copyTo1DJavaArray();
                lonTiePoints = (double[]) lonarr.flip(0).flip(1).copyTo1DJavaArray();
            } else {
                latTiePoints = (double[]) latarr.getStorage();
                lonTiePoints = (double[]) lonarr.getStorage();
            }

            if (externalGeo) {
                geoNcFile.close();
            }

            final TiePointGrid latGrid = new TiePointGrid("latitude", dims[1], dims[0], 0, offsetY,
                    cntl_lon_ix, cntl_lat_ix, latTiePoints);
            product.addTiePointGrid(latGrid);

            final TiePointGrid lonGrid = new TiePointGrid("longitude", dims[1], dims[0], 0, offsetY,
                    cntl_lon_ix, cntl_lat_ix, lonTiePoints);
            product.addTiePointGrid(lonGrid);

            product.setGeoCoding(new BowtieTiePointGeoCoding(latGrid, lonGrid, scanHeight));
            //product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));

        } catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }

    }

    public boolean mustFlipMODIS() throws ProductIOException {
        String platform = getStringAttribute("MODIS_Platform");
        String dnflag = getStringAttribute("DayNightFlag");

        boolean startNodeAscending = false;
        boolean endNodeAscending = false;
        if (platform.contains("Aqua")) {
            if (dnflag.contains("Day")) {
                startNodeAscending = true;
                endNodeAscending = true;
            }
        } else if (platform.contains("Terra")) {
            if (dnflag.contains("Night")) {
                startNodeAscending = true;
                endNodeAscending = true;
            }
        }

        return (startNodeAscending && endNodeAscending);
    }

    public void addGlobalAttributeModisL1B() {
        Element eosElement = null;
        try {
            eosElement = HdfEosUtils.getEosElement("CoreMetadata", ncFile.getRootGroup());
        } catch (IOException e) {
            e.printStackTrace();  //Todo add a valid exception
            System.out.print("Whoops...");
        }

        //grab granuleID
        try {
            Element inventoryMetadata = eosElement.getChild("INVENTORYMETADATA");
            Element inventoryElem = inventoryMetadata.getChildren().get(0);
            Element ecsdataElem = inventoryElem.getChild("ECSDATAGRANULE");
            Element granIdElem = ecsdataElem.getChild("LOCALGRANULEID");
            String granId = granIdElem.getValue().substring(1);
            Attribute granIdAttribute = new Attribute("Product_Name", granId);
            globalAttributes.add(granIdAttribute);
            Element dnfElem = ecsdataElem.getChild("DAYNIGHTFLAG");
            String daynightflag = dnfElem.getValue().substring(1);
            Attribute dnfAttribute = new Attribute("DayNightFlag", daynightflag);
            globalAttributes.add(dnfAttribute);

            Element ancinputElem = inventoryElem.getChild("ANCILLARYINPUTGRANULE");
            Element ancgranElem = ancinputElem.getChild("ANCILLARYINPUTGRANULECONTAINER");
            Element ancpointerElem = ancgranElem.getChild("ANCILLARYINPUTPOINTER");
            Element ancvalueElem = ancpointerElem.getChild("VALUE");
            String geoFilename = ancvalueElem.getValue();
            Attribute geoFileAttribute = new Attribute("Geolocation_File", geoFilename);
            globalAttributes.add(geoFileAttribute);

            //grab granule date-time
            Element timeElem = inventoryElem.getChild("RANGEDATETIME");
            Element startTimeElem = timeElem.getChild("RANGEBEGINNINGTIME");
            String startTime = startTimeElem.getValue().substring(1);
            Element startDateElem = timeElem.getChild("RANGEBEGINNINGDATE");
            String startDate = startDateElem.getValue().substring(1);
            Attribute startTimeAttribute = new Attribute("Start_Time", startDate + ' ' + startTime);
            globalAttributes.add(startTimeAttribute);

            Element endTimeElem = timeElem.getChild("RANGEENDINGTIME");
            String endTime = endTimeElem.getValue().substring(1);
            Element endDateElem = timeElem.getChild("RANGEENDINGDATE");
            String endDate = endDateElem.getValue().substring(1);
            Attribute endTimeAttribute = new Attribute("End_Time", endDate + ' ' + endTime);
            globalAttributes.add(endTimeAttribute);

            Element measuredParamElem = inventoryElem.getChild("MEASUREDPARAMETER");
            Element measuredContainerElem = measuredParamElem.getChild("MEASUREDPARAMETERCONTAINER");
            Element paramElem = measuredContainerElem.getChild("PARAMETERNAME");
            String param = paramElem.getValue().substring(1);
            String resolution = "1km";
            if (param.contains("EV_500")) {
                resolution = "500m";
            } else if (param.contains("EV_250")) {
                resolution = "250m";
            }
            Attribute paramAttribute = new Attribute("MODIS_Resolution", resolution);
            globalAttributes.add(paramAttribute);

            //grab Mission Name
            Element platformElem = inventoryElem.getChild("ASSOCIATEDPLATFORMINSTRUMENTSENSOR");
            Element containerElem = platformElem.getChild("ASSOCIATEDPLATFORMINSTRUMENTSENSORCONTAINER");
            Element shortNameElem = containerElem.getChild("ASSOCIATEDPLATFORMSHORTNAME");
            String shortName = shortNameElem.getValue().substring(2);
            Attribute shortNameAttribute = new Attribute("MODIS_Platform", shortName);
            globalAttributes.add(shortNameAttribute);
        } catch (Exception ignored) {

        }
    }

    private int getDimension(String dimensionName) {
        final List<Dimension> dimensions = ncFile.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimension.getShortName().equals(dimensionName)) {
                return dimension.getLength();
            }
        }
//        MODIS_SWATH_Type_L1B_2*nscans
//        dimension = MODIS_SWATH_Type_L1B_1KM_geo_dim
        return -1;
    }
}
