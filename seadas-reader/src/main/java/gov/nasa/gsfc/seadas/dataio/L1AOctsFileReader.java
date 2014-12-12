package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L1AOctsFileReader extends SeadasFileReader {

    static final int[] OCTS_WVL = new int[]{412, 443, 490, 520, 560, 670, 765, 865};

    L1AOctsFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Pixels_per_Scan_Line");
        int sceneHeight = getIntAttribute("Number_of_Scan_Lines") * 2;
        String productName = productReader.getInputFile().getName();

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

        variableMap = addOctsBands(product, ncFile.getVariables());
        // todo: OCTS L1 uses the same orbit vector geolocation as SeaWiFS - make the SeaWiFSL1aGeonav code generic
        addGeocoding(product);

//        addFlagsAndMasks(product);

        return product;

    }

    public void addGeocoding(final Product product) throws ProductIOException {
        String navGroup = "Scan-Line_Attributes";
        final String longitude = "lon";
        final String latitude = "lat";
        int susampX = 16;
        int subsampY = 2;


        Variable lats = ncFile.findVariable(navGroup + "/" +latitude);
        Variable lons = ncFile.findVariable(navGroup + "/" +longitude);

        int[] dims = lats.getShape();

        float[] latTiePoints;
        float[] lonTiePoints;

        try {
            Array latarr = lats.read();
            Array lonarr = lons.read();

            latTiePoints = (float[]) latarr.getStorage();
            lonTiePoints = (float[]) lonarr.getStorage();

            final TiePointGrid latGrid = new TiePointGrid("latitude", dims[1], dims[0], 0, 0,
                    susampX, subsampY, latTiePoints);

            product.addTiePointGrid(latGrid);

            final TiePointGrid lonGrid = new TiePointGrid("longitude", dims[1], dims[0], 0, 0,
                    susampX, subsampY, lonTiePoints);

            product.addTiePointGrid(lonGrid);

            product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));

        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    private Map<Band, Variable> addOctsBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            int variableRank = variable.getRank();
            if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[0];
                final int height = dimensions[1];
                final int width = dimensions[2];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    final List<Attribute> list = variable.getAttributes();

                    String units = "radiance counts";
                    String description = "Level-1A data";

                    for (int i = 0; i < bands; i++) {
                        final String shortname = "L1A";
                        StringBuilder longname = new StringBuilder(shortname);
                        longname.append("_");
                        longname.append(OCTS_WVL[i]);
                        String name = longname.toString();
                        final int dataType = getProductDataType(variable);
                        final Band band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        final float wavelength = Float.valueOf(OCTS_WVL[i]);
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

                    }
                }
            }
        }
        return bandToVariableMap;
    }

}
