package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.IOException;

/**
 * A common command-line interface for the GPF.
 */
public class Main {

    public static void main(String[] args) {
        try {
            new Main().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }

    private void run(String[] args) throws IOException, OperatorException, ValidationException, ConversionException {
        CommandLine line = new CommandLine(args);

        // todo

//        Product inputProduct = ProductIO.readProduct(new File(sourceFilepath), null);
//        if (inputProduct == null) {
//            throw new OperatorException("No approriate product reader found.");
//        }
//
//        Product outputProduct = GPF.createProduct("SpectralUnmixing",
//                                                  parameters,
//                                                  inputProduct,
//                                                  ProgressMonitor.NULL);
//
//        ProductIO.writeProduct(outputProduct, targetFilepath, outputFormat);
    }

    private static void error(String m) {
        System.err.println("Error: " + m);
        CommandLine.printUsage();
        System.exit(1);
    }

}
