/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.classifiers.test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.clustering.dirichlet.DirichletOp;
import org.esa.nest.clustering.fuzzykmeans.FuzzyKMeansOp;

/**
 *
 * @author emmab
 */
public class Test {

    private static String inputPathQuad = "ASA_IMP_1PNDPA20080505_033824_000000152068_00190_32308_7426.N1";
    private static OperatorSpi spi;
    private static String root = "D:\\";

    public static Product readSourceProduct(final String path) throws IOException {
        final File inputFile = new File(path);
        if (!inputFile.exists()) {
            throw new IOException(path + " not found");
        }
        final ProductReader reader = ProductIO.getProductReaderForInput(inputFile);
        if (reader == null) {
            throw new IOException("No reader found for " + inputFile);
        }
        return reader.readProductNodes(inputFile, null);
    }
    

    public static void main(String[] dfgdf) {
        spi = new FuzzyKMeansOp.Spi();
        try {
            final Product sourceProduct = ProductIO.readProduct(root + inputPathQuad);
            System.out.println(root + inputPathQuad);
            FuzzyKMeansOp op = (FuzzyKMeansOp) spi.createOperator();
            op.setSourceProduct(sourceProduct);

            Product targetProduct = op.getTargetProduct();
            final Band band = targetProduct.getBandAt(0);

            final float[] floatValues = new float[8];
            band.readPixels(156, 365, 4, 2, floatValues);
//            for (float f : floatValues) {
//                System.out.println(f);
//            }
            File aFile = new File(root + "FuzzyKMeansOp" + inputPathQuad);
            ImageIO.write(band.getSourceImage(), "png", new File(root + "FuzzyKMeansOp.png"));

//            ProductIO.writeProduct(targetProduct, aFile, DimapProductWriterPlugIn.DIMAP_FORMAT_NAME, true,
//                    new PrintWriterProgressMonitor(System.out));

        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
