package org.esa.beam.examples.codesnippets.framework.datamodel;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.ceres.core.PrintWriterProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: Tom Date: 29.04.2004 Time: 14:59:02 To change this template use File | Settings |
 * File Templates.
 */

/**
 * Each satellite product is an object of the class </p><pre>org.esa.beam.framework.datamodel.Product</pre></p>
 * This class contains code snippets that show some possibilities and constraints in the usage of the product class.
 */
public class ProductExamples {

    /**
     * Shows how to read a bitmask line-wise.
     */
    public static class ReadBitmaskLinewise {

        public static void main(String[] args) {
            try {
                Product product = ProductIO.readProduct("C:/Projects/BEAM/data/MER_RR__1P_A.N1");
                int width = product.getSceneRasterWidth();
                int height = product.getSceneRasterHeight();
                // parse a given expression
                final Term term = product.parseExpression("not l2_flags.INVALID and not l1_flags.BRIGHT");
                // allocate boolean mask values for a single scan line
                boolean[] maskLine = new boolean[width];
                for (int y = 0; y < height; y++) {
                    // read mask for each scan line
                    product.readBitmask(0, 0, width, 1, term, maskLine, new PrintWriterProgressMonitor(System.out));
                    // process boolean values contained in maskLine here
                    // ...
                }
                product.dispose();
            } catch (IOException e) {
                // handle error here...
                e.printStackTrace();
            } catch (ParseException e) {
                // handle error here...
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an in-memory product with given width and height and adds a band to the product.
     *
     * @return the created in-memory product.
     */
    public static Product createInMemoryProduct() {
        // define the product size in pixels
        int width = 200;
        int height = 200;

        // allocate data for the band to be added
        float[] bandData = new float[width * height];

        // create the product. Name and type of the product identify the product.
        // The constructor accepts four parameters:
        //  name - String  - the name of the product
        //  type - String - the product type description
        //  width - int - the width of the scene data raster of the product
        //  height - int - the height of the scene data raster of the product
        Product product = new Product("productName", "productType", width, height);

        // Create a band of type float. The width and height of the band must match the
        // product width and height where the band shall be added to.
        Band aBand = new Band("bandName", ProductData.TYPE_FLOAT32, width, height);
        // Attach the data array to the band.
        aBand.setData(ProductData.createInstance(bandData));

        // Add the band to the product
        product.addBand(aBand);

        product.setModified(false);

        return product;
    }

    /**
     * Attach a geolocation to a given product. In this example, the geolocation is defined by a tie-point geocoding
     * attached to the product.
     *
     * @param product
     *
     * @return the product with the attached tie-point geocoding
     */
    public Product addTiePointGeoCoding(Product product) {
        // define the geolocation as two array containing latitude and longitude of the product for
        // a subsampled range of the pixels.
        float[] latitudes = {
                64.508275f,
                64.673630f,
                64.836693f,
                64.224285f,
                64.387994f,
                64.549421f,
                63.939044f,
                64.101139f,
                64.260961f
        };
        float[] longitudes = {
                39.620031f,
                39.041526f,
                38.455981f,
                39.182590f,
                38.607548f,
                38.025667f,
                38.753915f,
                38.182357f,
                37.604155f
        };

        // Create two tie-point grids (i.e. subsampled bands)
        // The constructo parameters are the following:
        // name - String - the name of the tie-point grid
        // width - int - the width of the tie-point grid
        // height - int - the height of the tie-point grid
        // offsetX - the offset in x -direction in pixels relative to the band data pixels
        // offsetY - the offset in y -direction in pixels relative to the band data pixels
        // subsX - the subsampling on x-direction
        // subsY - the subsampling in y-direction
        // data - the raw data of the tie-point grid
        //
        // The calls below assume that the product has a size of 6*6 pixels - according to the
        // subsampling parameters
        TiePointGrid latGrid = new TiePointGrid("latitude", 3, 3, 0, 0, 2, 2, latitudes);
        TiePointGrid lonGrid = new TiePointGrid("longitude", 3, 3, 0, 0, 2, 2, longitudes);

        // add both tie point grids to the product
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        // create a tie-point geo coding with both tie point grids
        GeoCoding coding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        // and attach it to the product
        product.setGeoCoding(coding);

        return product;
    }

    public static class WriteInMemoryProductToDisk {

        public static void main(String[] args) {

            Product aProduct = createInMemoryProduct();

            // define where we want to save the product to ...
            File aFile = new File("C:/data/test_product.dim");

            try {
                ProductIO.writeProduct(aProduct, aFile, DimapProductWriterPlugIn.DIMAP_FORMAT_NAME, true,
                                       new PrintWriterProgressMonitor(System.out));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
