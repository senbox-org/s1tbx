package gov.nasa.gsfc.seadas.dataio;

import gov.nasa.gsfc.seadas.dataio.SeadasProductReader.ProductType;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
 */
public class SMIFileReader extends SeadasFileReader {

    SMIFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {
        //todo incoroprate the SMI product table info to replace the getL2BandInfoMap stuff.

        int [] dims = ncFile.getVariables().get(0).getShape();
        int sceneHeight = dims[0];
        int sceneWidth = dims[1];
        
        String productName = productReader.getInputFile().getName();
        try {
                productName = getStringAttribute("Product Name");
        } catch (Exception ignored) {

        }

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addSmiMetadata(product);
        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);
        addFlagsAndMasks(product);
        return product;
    }

    @Override
    protected Band addNewBand(Product product, Variable variable) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band = null;

        int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    String name = variable.getShortName();
                    if (name.equals("l3m_data")){
                        try {
                            name = new StringBuilder().append(getStringAttribute("Parameter")).append(" ").append(getStringAttribute("Measure")).toString();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);

                    product.addBand(band);

                    try {
                        Attribute fillvalue = variable.findAttribute("_FillValue");
                        if (fillvalue == null){
                            fillvalue = variable.findAttribute("Fill");
                        }
                        if (fillvalue != null){
                            band.setNoDataValue((double) fillvalue.getNumericValue().floatValue());
                            band.setNoDataValueUsed(true);
                        }
                    } catch (Exception ignored) {

                    }
                    // Set units, if defined
                    try {
                        band.setUnit(getStringAttribute("Units"));
                    }  catch (Exception ignored){

                    }

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                         if ("Slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("Intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        }
                    }
                }
            }
        return band;
    }

    public void addGeocoding(Product product) {
        //float pixelX = 0.0f;
        //float pixelY = 0.0f;
        // Changed after conversation w/ Sean, Norman F., et al.
        float pixelX = 0.5f;
        float pixelY = 0.5f;
        String east = "Easternmost_Longitude";
        String west = "Westernmost_Longitude";
        String north = "Northernmost_Latitude";
        String south = "Southernmost_Latitude";

        final MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
        float easting = (float) globalAttributes.getAttribute(east).getData().getElemDouble();
        float westing = (float) globalAttributes.getAttribute(west).getData().getElemDouble();
        float pixelSizeX = (easting - westing) / product.getSceneRasterWidth();
        float northing = (float) globalAttributes.getAttribute(north).getData().getElemDouble();
        float southing = (float) globalAttributes.getAttribute(south).getData().getElemDouble();
        if (northing < southing){
            mustFlipY=true;
            northing = (float) globalAttributes.getAttribute(south).getData().getElemDouble();
            southing = (float) globalAttributes.getAttribute(north).getData().getElemDouble();
        }
        float pixelSizeY = (northing - southing) / product.getSceneRasterHeight();

        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    westing, northing,
                    pixelSizeX, pixelSizeY,
                    pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addSmiMetadata(final Product product) {
//        Variable l3mvar = ncFile.findVariable("l3m_data");
        Variable l3mvar = ncFile.getVariables().get(0);
        List<Attribute> variableAttributes = l3mvar.getAttributes();
        final MetadataElement smiElement = new MetadataElement("SMI Product Parameters");
        addAttributesToElement(variableAttributes, smiElement);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(smiElement);
    }
        @Override
    protected void addFlagsAndMasks(Product product) {
        Band QFBand = product.getBand("l3m_qual");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("SST_Quality");
            flagCoding.addFlag("QualityLevel", 0, "Best quality");
//            flagCoding.addFlag("Best", 0, "Best quality");
//            flagCoding.addFlag("Good", 0x01, "Good quality");
//            flagCoding.addFlag("Questionable", 0x02, "Questionable quality");
//            flagCoding.addFlag("Bad", 0x03, "Bad quality");
//            flagCoding.addFlag("NoValue", 4, "Not Processed");


            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);

           product.getMaskGroup().add(Mask.BandMathsType.create("Best", "Highest quality retrieval",
                                                                product.getSceneRasterWidth(),
                                                                product.getSceneRasterHeight(), "l3m_qual == 0",
                                                                SeadasFileReader.Cornflower, 0.6));
           product.getMaskGroup().add(Mask.BandMathsType.create("Good", "Good quality retrieval",
                                                                product.getSceneRasterWidth(),
                                                                product.getSceneRasterHeight(), "l3m_qual == 1",
                                                                SeadasFileReader.LightPurple, 0.6));
           product.getMaskGroup().add(Mask.BandMathsType.create("Questionable", "Questionable quality retrieval",
                                                                product.getSceneRasterWidth(),
                                                                product.getSceneRasterHeight(), "l3m_qual == 2",
                                                                SeadasFileReader.BurntUmber, 0.6));
           product.getMaskGroup().add(Mask.BandMathsType.create("Bad", "Bad quality retrieval",
                                                                product.getSceneRasterWidth(),
                                                                product.getSceneRasterHeight(), "l3m_qual == 3",
                                                                SeadasFileReader.FailRed, 0.6));
//           product.getMaskGroup().add(Mask.BandMathsType.create("NoValue", "No Retrieval",
//                                                                product.getSceneRasterWidth(),
//                                                                product.getSceneRasterHeight(), "l3m_qual.NotComputed",
//                                                                SeadasFileReader.BrightPink, 0.6));

        }
    }
}