package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
  */
public class L1BHicoFileReader extends SeadasFileReader {

    L1BHicoFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    Array wavlengths = ncFile.findVariable("products/Lt").findAttribute("wavelengths").getValues();

    @Override
    public Product createProduct() throws ProductIOException {

        int[] dims = ncFile.findVariable("products/Lt").getShape();
        int sceneWidth = dims[1];
        int sceneHeight = dims[0];

        String productName = getStringAttribute("metadata/FGDC/Identification_Information/Dataset_Identifier");

        mustFlipX = mustFlipY = getDefaultFlip();
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start",globalAttributes);
        if (utcStart != null) {
            if (mustFlipY){
                product.setEndTime(utcStart);
            } else {
                product.setStartTime(utcStart);
            }
        }
        ProductData.UTC utcEnd = getUTCAttribute("End",globalAttributes);
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
        variableMap = addHicoBands(product, ncFile.getVariables());

        addGeocoding(product);
        addMetadata(product, "products", "Band_Metadata");
        addMetadata(product, "navigation", "Navigation_Metadata");
        addMetadata(product, "images", "Image_Metadata");
        addMetadata(product,"quality","Quality_Metadata");

        /*
        todo add ability to read the true_color image inculded in the file
        todo the flag bit variable is in width x height not height x width as the other bands...so need to figure
             out how to read it...
        */
//        addQualityFlags(product);


        product.setAutoGrouping("Lt");

        return product;
    }

//    f_01_name	LAND	ascii
//    f_02_name	NAVFAIL	ascii
//    f_03_name	NAVWARN	ascii
//    f_04_name	HISOLZEN	ascii
//    f_05_name	HISATZEN	ascii
//    f_07_name	CALFAIL	ascii
//    f_08_name	CLOUD	ascii

    private void addQualityFlags(Product product) {
        Variable quality_flags = ncFile.findVariable("quality/flags");
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final int[] dimensions = quality_flags.getShape();
        final int height = dimensions[1] - leadLineSkip - tailLineSkip;
        final int width = dimensions[0];

        if (height == sceneRasterHeight && width == sceneRasterWidth) {
            final String name = "HICO_flags";
//            final String name = quality_flags.getShortName();
            final int dataType = getProductDataType(quality_flags);
            Band QFband = new Band(name, dataType, width, height);

            product.addBand(QFband);

            FlagCoding flagCoding = new FlagCoding("Quality_Flags");
            flagCoding.addFlag("LAND", 0x01, "Land");
            flagCoding.addFlag("NAVFAIL", 0x02, "Navigation failure");
            flagCoding.addFlag("NAVWARN", 0x04, "Navigation suspect");
            flagCoding.addFlag("HISOLZEN", 0x08, "High solar zenith angle");
            flagCoding.addFlag("HISATZEN", 0x10, "Large satellite zenith angle");
            flagCoding.addFlag("SPARE", 0x20, "Unused");
            flagCoding.addFlag("CALFAIL", 0x40, "Calibration failure");
            flagCoding.addFlag("CLOUD", 0x80, "Cloud determined");

            product.getFlagCodingGroup().add(flagCoding);
            QFband.setSampleCoding(flagCoding);
            variableMap.put(QFband,quality_flags);

            product.getMaskGroup().add(Mask.BandMathsType.create("LAND", "Land",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.LAND",
                    LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISATZEN", "Large satellite zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.HISATZEN",
                    LightCyan, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD", "Cloud determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.CLOUD",
                    Color.WHITE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISOLZEN", "High solar zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.HISOLZEN",
                    Purple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CALFAIL", "Calibration failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.CALFAIL",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVWARN", "Navigation suspect",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.NAVWARN",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVFAIL", "Navigation failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.NAVFAIL",
                    FailRed, 0.0));
        }
    }

    private ProductData.UTC getUTCAttribute(String key, List<Attribute> globalAttributes) {
        String timeString = null;
        try {
            if (key.equals("Start")){
                Attribute date_attribute = findAttribute("metadata/FGDC/Identification_Information/Time_Period_of_Content/Beginning_Date", globalAttributes);
                Attribute time_attribute = findAttribute("metadata/FGDC/Identification_Information/Time_Period_of_Content/Beginning_Time", globalAttributes);
                StringBuilder tstring = new StringBuilder(date_attribute.getStringValue().trim());
                tstring.append(time_attribute.getStringValue().trim());
                tstring.append("000");
                timeString = tstring.toString();
            }
            if (key.equals("End")){
                Attribute date_attribute = findAttribute("metadata/FGDC/Identification_Information/Time_Period_of_Content/Ending_Date", globalAttributes);
                Attribute time_attribute = findAttribute("metadata/FGDC/Identification_Information/Time_Period_of_Content/Ending_Time", globalAttributes);
                StringBuilder tstring = new StringBuilder(date_attribute.getStringValue().trim());
                tstring.append(time_attribute.getStringValue().trim());
                tstring.append("000");
                timeString = tstring.toString();
            }
        } catch (Exception ignored) {
        }

        if (timeString != null) {

            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyDDDHHmmssSSS");
            try {
                final Date date = dateFormat.parse(timeString);
                String milliSeconds = timeString.substring(timeString.length() - 3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);

            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    public void addMetadata(Product product, String groupname, String meta_element) throws ProductIOException {
        Group group =  ncFile.findGroup(groupname);

        if (group != null) {
            final MetadataElement bandAttributes = new MetadataElement(meta_element);
            List<Variable> variables = group.getVariables();
            for (Variable variable : variables) {
                final String name = variable.getShortName();
                final MetadataElement sdsElement = new MetadataElement(name + ".attributes");
                final int dataType = getProductDataType(variable);
                final MetadataAttribute prodtypeattr = new MetadataAttribute("data_type", dataType);

                sdsElement.addAttribute(prodtypeattr);
                bandAttributes.addElement(sdsElement);

                final List<Attribute> list = variable.getAttributes();
                for (Attribute varAttribute : list) {
                    addAttributeToElement(sdsElement, varAttribute);
                }
            }
            final MetadataElement metadataRoot = product.getMetadataRoot();
            metadataRoot.addElement(bandAttributes);
        }
    }

    private Map<Band, Variable> addHicoBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band = null;

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            if ((variable.getShortName().equals("latitudes")) || (variable.getShortName().equals("longitudes")))
                continue;
            int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0] - leadLineSkip - tailLineSkip;
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    final String name = variable.getShortName();
                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);
                    final String validExpression = bandInfoMap.get(name);
                    if (validExpression != null && !validExpression.equals("")) {
                        band.setValidPixelExpression(validExpression);
                    }
                    product.addBand(band);

                    try {
                        band.setNoDataValue((double) variable.findAttribute("bad_value_scaled").getNumericValue().floatValue());
                        band.setNoDataValueUsed(true);
                    } catch (Exception ignored) { }

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        }
                    }
                }
            }
            if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    for (int i = 0; i < bands; i++) {
                        final float wavelength = getHicoWvl(i);
                        StringBuilder longname = new StringBuilder(description);
                        longname.append("_");
                        longname.append(wavelength);
                        String name = longname.toString();
                        final int dataType = getProductDataType(variable);
                        band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        band.setSpectralWavelength(wavelength);
                        band.setSpectralBandIndex(spectralBandIndex++);

                        Variable sliced = null;
                        try {
                            sliced = variable.slice(2, i);
                        } catch (InvalidRangeException e) {
                            e.printStackTrace();  //Todo change body of catch statement.
                        }
                        try {
                            band.setNoDataValue((double) variable.findAttribute("bad_value_scaled").getNumericValue().floatValue());
                            band.setNoDataValueUsed(true);
                        } catch (Exception ignored) { }

                        final List<Attribute> list = variable.getAttributes();
                        for (Attribute hdfAttribute : list) {
                            final String attribName = hdfAttribute.getShortName();
                            if ("units".equals(attribName)) {
                                band.setUnit(hdfAttribute.getStringValue());
                            } else if ("long_name".equals(attribName)) {
                                band.setDescription(hdfAttribute.getStringValue());
                            } else if ("slope".equals(attribName)) {
                                band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                            } else if ("intercept".equals(attribName)) {
                                band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                            }
                        }
                        bandToVariableMap.put(band, sliced);
                        band.setUnit(units);
                        band.setDescription(description);

                    }
                }
            }
        }
        return bandToVariableMap;
    }

    float getHicoWvl(int index) {
        return wavlengths.getFloat(index);
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "longitudes";
        final String latitude = "latitudes";
        String navGroup = "navigation";

        Variable latVar = ncFile.findVariable(navGroup + "/" + latitude);
        Variable lonVar = ncFile.findVariable(navGroup + "/" + longitude);
        if (latVar != null && lonVar != null ) {
            final ProductData lonRawData = readData(lonVar);
            final ProductData latRawData = readData(latVar);
            Band latBand = null;
            Band lonBand = null;
            latBand = product.addBand(latVar.getShortName(), ProductData.TYPE_FLOAT32);
            lonBand = product.addBand(lonVar.getShortName(), ProductData.TYPE_FLOAT32);
            latBand.setNoDataValue(-999.);
            lonBand.setNoDataValue(-999.);
            latBand.setNoDataValueUsed(true);
            lonBand.setNoDataValueUsed(true);
            latBand.setData(latRawData);
            lonBand.setData(lonRawData);

            try {
                if (latBand != null && lonBand != null) {
                    product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5, ProgressMonitor.NULL));
                }
            } catch (IOException e) {
                throw new ProductIOException(e.getMessage());
            }
        }
    }
}