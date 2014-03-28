package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
  */
public class AquariusL2FileReader extends SeadasFileReader {

    AquariusL2FileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Number_of_Beams");
        int sceneHeight = getIntAttribute("Number_of_Blocks");
        String productName = getStringAttribute("Product_Name");


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
        addBandMetadata(product);
//        addScientificMetadata(product);

        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);

//        todo: add flags
/*
    The radiometer flag field has the following dimensions (in FORTRAN order):
    (index, beam, block).


    Bits     Condition         Index Dimension                L3 Flagname
    ----     ---------         ---------------                -----------
     1       RFI moderate      (V,P,M,H) polarization         RFIYELLOW
     2       RFI severe        (V,P,M,H) polarization         RFIRED
     3       Rain              (V mod, V sev, H mod, H sev)   RAINYELLOW, RAINRED
     4       Land              (moderate, severe)             LANDYELLOW, LANDRED
     5       Ice               (moderate, severe)             ICEYELLOW, ICERED
     6       Wind/Foam         (moderate, severe)             WINDYELLOW, WINDRED
     7       Temp              (V mod, V sev, H mod, H sev)   TEMPYELLOW, TEMPRED
     8       Solar Flux D      (V mod, V sev, H mod, H sev)   FLUXDYELLOW, FLUXDRED
     9       Solar Flux R      (V mod, V sev, H mod, H sev)   FLUXRYELLOW, FLUXRRED
    10       Sun Glint mod     (V mod, V sev, H mod, H sev)   GLINTYELLOW, GLINTRED
    11       Moon              (V mod, V sev, H mod, H sev)   MOONYELLOW, MOONRED
    12       Galaxy            (V mod, V sev, H mod, H sev)   GALYELLOW,  GALRED
    13       Nav               (Roll, Pitch, Yaw, OOB)        NAV
    14       SA overflow       On                             SAOVERFLOW
    15       Roughness fail    On                             ROUGH

*/
//        addFlagsAndMasks(product);
        product.setAutoGrouping("Kpc:SSS:anc:dTB:rad:scat:sun");

        return product;
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "scat_beam_clon";
        final String latitude = "scat_beam_clat";
        Band latBand = null;
        Band lonBand = null;


        if (product.containsBand(latitude) && product.containsBand(longitude)) {
            latBand = product.getBand(latitude);
            lonBand = product.getBand(longitude);
        }
        try {
            if (latBand != null && lonBand != null) {
                product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5, ProgressMonitor.NULL));
            }
        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }

    }
}