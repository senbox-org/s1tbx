package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
  */
public class BrowseProductReader extends SeadasFileReader {

    BrowseProductReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Pixels_per_Scan_Line");
        int sceneHeight = getIntAttribute("Number_of_Scan_Lines");

        String productName = getStringAttribute("Product_Name");

        mustFlipX = mustFlipY = getDefaultFlip();
        SeadasProductReader.ProductType productType = productReader.getProductType();
        if (productType == SeadasProductReader.ProductType.Level1A_CZCS ||
                productType == SeadasProductReader.ProductType.Level2_CZCS)
            mustFlipX = false;

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start_Time");
        if (utcStart != null) {
            if (mustFlipY){
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
//        addBandMetadata(product);
        variableMap = addBrsBands(product, ncFile.getVariables());
//        variableMap = addNewBrsBand(product, ncFile.getVariables());

        addGeocoding(product);

        return product;
    }

//    @Override
//    public Map<Band, Variable> addBands(Product product,
//                                        List<Variable> variables) {
//        final Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
//        for (Variable variable : variables) {
//            Map<Band, Variable> band = addNewBrsBand(product, variable);
//            if (band != null) {
//                bandToVariableMap.put(band, variable);
//            }
//        }
//
//        return bandToVariableMap;
//    }
//    @Override

    protected Map<Band, Variable>  addBrsBands(Product product,  List<Variable>  variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band;
        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        String description = "Level-1A Browse data";
        String units = "Relative Reflectance units";

        for (Variable variable : variables) {
            int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0] - leadLineSkip - tailLineSkip;
                final int width = dimensions[1];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    String name = variable.getShortName();
                    boolean isBrs = false;
                    try {
                        isBrs = variable.findAttribute("long_name").getStringValue().contains("brs_data");
                    } catch (Exception ignored) { }
                    if (isBrs){
                        try {
                            name = getStringAttribute("Parameter");
                            description = name;
                        } catch (Exception ignore) { }
                    }
                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);

                    product.addBand(band);

                    if (isBrs){
                        try {
                            band.setScalingFactor(getFloatAttribute("Slope"));
                            band.setScalingOffset(getFloatAttribute("Intercept"));
//                            band.setUnit(units);
                            band.setNoDataValue(253.);// * band.getScalingFactor() + band.getScalingOffset()));
                            band.setNoDataValueUsed(true);
                        } catch (Exception ignored) { }
                        band.setDescription(description);
                    } else {
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
                    bandToVariableMap.put(band, variable);
                }
            } else if (variable.getRank() == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[2];
                final int height = dimensions[0] - leadLineSkip - tailLineSkip;
                final int width = dimensions[1];


                final String bandnames[] = {"Red", "Green", "Blue"};
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();


                    for (int i = 0; i < bands; i++) {
                        final String name = bandnames[i];
                        final int dataType = getProductDataType(variable);
                        band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        Variable sliced = null;
                        try {
                            sliced = variable.slice(2, i);
                        } catch (InvalidRangeException e) {
                            e.printStackTrace();  //Todo change body of catch statement.
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

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "longitude";
        final String latitude = "latitude";
        Band latBand;
        Band lonBand;

        latBand = product.getBand(latitude);
        lonBand = product.getBand(longitude);
        latBand.setNoDataValue(-999.);
        lonBand.setNoDataValue(-999.);
        latBand.setNoDataValueUsed(true);
        lonBand.setNoDataValueUsed(true);

        try {

            product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5, ProgressMonitor.NULL));

        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}