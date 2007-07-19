package org.esa.beam.unmixing;

import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.ParameterDefinitionFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataio.ProductIO;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.core.ProgressMonitor;

import java.util.HashMap;
import java.io.IOException;
import java.io.File;

/**
 * The "Unmix" command line.
 */
public class SpectralUnmixingMain  {
    private String inputFilepath;
    private String outputFilepath;
    private String outputFormat;
    private HashMap<String, Object> params;

    public static void main(String[] args) {
        try {
            new SpectralUnmixingMain(). run(args);
        } catch (Exception e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }

    private  void run(String[] args) throws IOException, OperatorException, ValidationException, ConversionException {
        parseArgs(args);

        Product inputProduct = ProductIO.readProduct(new File(inputFilepath), null);
        if (inputProduct == null) {
            throw new OperatorException("No approriate product reader found.");
        }

        Product outputProduct = GPF.createProduct("SpectralUnmixing",
                                                  params,
                                                  inputProduct,
                                                  ProgressMonitor.NULL);

        ProductIO.writeProduct(outputProduct, outputFilepath, outputFormat);
    }

    private void parseArgs(String[] args) throws ValidationException, ConversionException {
        inputFilepath = null;
        outputFilepath = null;
        outputFormat = "BEAM-DIMAP";
        params = new HashMap<String, Object>();

        ValueContainer container = ParameterDefinitionFactory.createMapBackedOperatorValueContainer("SpectralUnmixing", params);

        int argCount = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.startsWith("-P")) {
                    int pos = arg.indexOf('=');
                    String name = arg.substring(2, pos);
                    String value = arg.substring(pos + 1);
                    container.getModel(name).setFromText(value);
                } else {
                    error("Unknown option '" + arg + "'");
                }
            } else {
                if (argCount == 0) {
                    inputFilepath = arg;
                } else if (argCount == 1) {
                    outputFilepath = arg;
                } else if (argCount == 2) {
                    outputFormat = arg;
                } else {
                    error("Too many arguments");
                }
                argCount++;
            }
        }
        if (inputFilepath == null) {
            error("Missing input file (argument #1)");
        }
        if (outputFilepath == null) {
            error("Missing output file (argument #2)");
        }
    }

    private static void error(String m) {
        System.err.println("Error: " + m);
        printUsage();
        System.exit(1);
    }

    private static void printUsage() {
        System.err.println("Usage: " + SpectralUnmixingMain.class.getName() + " <inputFile> <outputFile> [<outputFormat>] [-P<paramName>=<paramValue> ...]");
    }
}
