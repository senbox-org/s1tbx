package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L1ASeawifsFileReader extends SeadasFileReader {

    static final int[] SEAWIFS_WVL = new int[]{412, 443, 490, 510, 555, 670, 765, 865};

    L1ASeawifsFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Pixels_per_Scan_Line");
        int sceneHeight = getIntAttribute("Number_of_Scan_Lines");
        String productName = getStringAttribute("Product_Name");

        mustFlipX = mustFlipY = getDefaultFlip();
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start_Time");
        if (utcStart != null) {
            product.setStartTime(utcStart);
        }
        ProductData.UTC utcEnd = getUTCAttribute("End_Time");
        if (utcEnd != null) {
            product.setEndTime(utcEnd);
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addScientificMetadata(product);

        variableMap = addSeawifsBands(product, ncFile.getVariables());

        SeaWiFSL1AGeonav geonavCalculator = new SeaWiFSL1AGeonav(ncFile);
        float[] latitudes = flatten2DimArray(geonavCalculator.getLatitudes());
        float[] longitudes = flatten2DimArray(geonavCalculator.getLongitudes());
        Band latBand = new Band("latitude", ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        Band lonBand = new Band("longitude", ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        latBand.setNoDataValue(999.0);
        latBand.setNoDataValueUsed(true);
        lonBand.setNoDataValue(999.0);
        lonBand.setNoDataValueUsed(true);
        product.addBand(latBand);
        product.addBand(lonBand);

        ProductData lats = ProductData.createInstance(latitudes);
        latBand.setData(lats);
        ProductData lons = ProductData.createInstance(longitudes);
        lonBand.setData(lons);
        try {
            product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 10, ProgressMonitor.NULL));
        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }

        addFlagsAndMasks(product);

        return product;

    }

    private Map<Band, Variable> addSeawifsBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            int variableRank = variable.getRank();
            if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                   // final List<Attribute> list = variable.getAttributes();

                    String units = "radiance counts";
                    String description = "Level-1A data";

                    for (int i = 0; i < bands; i++) {
                        final String shortname = "L1A";
                        StringBuilder longname = new StringBuilder(shortname);
                        longname.append("_");
                        longname.append(SEAWIFS_WVL[i]);
                        String name = longname.toString();
                        final int dataType = getProductDataType(variable);
                        final Band band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        final float wavelength = Float.valueOf(SEAWIFS_WVL[i]);
                        band.setSpectralWavelength(wavelength);
                        band.setSpectralBandIndex(spectralBandIndex++);

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

}
