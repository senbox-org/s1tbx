package org.esa.beam.processor.cloud.internal.util;

import org.esa.beam.util.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Properties;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 */
public class RequestGenerator {

    public static final String OLD_PRODUCT_PREFIX = "old_product_prefix";
    public static final String NEW_PRODUCT_PREFIX = "new_product_prefix";

    public static final String OLD_FILE_EXTENSION = "old_file_extension";
    public static final String NEW_FILE_EXTENSION = "new_file_extension";

    public static final String FILENAME_PART = "filename_part";
    public static final String REQUEST_TYPE = "request_type";

    public static final String OVERWRITE_OUTPUT_PRODUCTS = "overwrite_output_products";
    public static final String RELATIVE_ORBITS = "relative_orbits";

    public static final String COUNT = "count";
    public static final String START_FROM_COUNT = "start_from_count";

    /////////////////////////////////////////////////////////////////////////

    private static final String requestFileName = "generated_request.xml";
    private Writer output;

    private String oldPrefix;
    private String newPrefix;
    private String oldExtension;
    private String newExtension;
    private String filenamePart;
    private String requestType;
    private boolean overwriteOutputProducts;
    private int count;
    private int startFromCount;
    private int[] relativeOrbits = null;


    public RequestGenerator(Properties properties) {
        oldPrefix = properties.getProperty(OLD_PRODUCT_PREFIX);
        if (oldPrefix == null) {
            throw new IllegalArgumentException(OLD_PRODUCT_PREFIX + " not set!");
        }
        newPrefix = properties.getProperty(NEW_PRODUCT_PREFIX);
        if (newPrefix == null) {
            throw new IllegalArgumentException(NEW_PRODUCT_PREFIX + " not set!");
        }

        oldExtension = properties.getProperty(OLD_FILE_EXTENSION);
        if (oldExtension == null) {
            throw new IllegalArgumentException(OLD_FILE_EXTENSION + " not set!");
        }
        newExtension = properties.getProperty(NEW_FILE_EXTENSION);
        if (newExtension == null) {
            throw new IllegalArgumentException(NEW_FILE_EXTENSION + " not set!");
        }

        filenamePart = properties.getProperty(FILENAME_PART);
        if (filenamePart == null) {
            throw new IllegalArgumentException(FILENAME_PART + " not set!");
        }

        requestType = properties.getProperty(REQUEST_TYPE);
        if (requestType == null) {
            throw new IllegalArgumentException(REQUEST_TYPE + " not set!");
        }

        overwriteOutputProducts = Boolean.parseBoolean(properties.getProperty(OVERWRITE_OUTPUT_PRODUCTS));
        String countString = properties.getProperty(COUNT);
        count = 0;
        if (countString != null && countString.length() != 0) {
            try {
                count = Integer.parseInt(countString);
            } catch (NumberFormatException e) {
                count = 0;
            }
        }
        String startFromCountString = properties.getProperty(START_FROM_COUNT);
        startFromCount = 0;
        if (startFromCountString != null && startFromCountString.length() != 0) {
            try {
                startFromCount = Integer.parseInt(startFromCountString);
            } catch (NumberFormatException e) {
                startFromCount = 0;
            }
        }

        String relativeOrbitsString = properties.getProperty(RELATIVE_ORBITS);
        if (relativeOrbitsString != null && relativeOrbitsString.length() != 0) {
            String[] relativeOrbitsStringArray = StringUtils.csvToArray(relativeOrbitsString);
            relativeOrbits = new int[relativeOrbitsStringArray.length];
            for (int i = 0; i < relativeOrbitsStringArray.length; i++) {
                String s = relativeOrbitsStringArray[i];
                relativeOrbits[i] = Integer.parseInt(s.trim());
            }
            Arrays.sort(relativeOrbits);
        }
    }

    public void generateRequest(File inDir, File outDir) throws IOException {
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.startsWith(oldPrefix) &&
                        name.endsWith(oldExtension) &&
                        name.indexOf(filenamePart) != -1) {
                    if (relativeOrbits != null) {
                        String relOrbitString = (name.split("_"))[6];
                        int relOrbit = Integer.parseInt(relOrbitString);
                        int foundIndex = Arrays.binarySearch(relativeOrbits, relOrbit);
                        return foundIndex >= 0;
                    }else{
                        return true;
                    }
                }else{
                    return false;
                }
            }
        };

        createWriter();
        writeHeader();
        String[] fileNames = inDir.list(filter);
        if (fileNames != null && fileNames.length > 0) {
            Arrays.sort(fileNames);
            for (int file = startFromCount; file < fileNames.length; file++) {
                final String inputName = fileNames[file];
                int index = inputName.lastIndexOf(oldExtension);
                final String nameWithoutExtension = inputName.substring(0, index);
                final String outputName = newPrefix + nameWithoutExtension.substring(oldPrefix.length()) + newExtension;

                final String inputFileName = inDir + File.separator + inputName;
                final String outputFileName = outDir + File.separator + outputName;

                File outFile = new File(outputFileName);
                if (!outFile.exists() || overwriteOutputProducts) {
                    writeRequest(inputFileName, outputFileName);
                }
                if (count != 0 && file >= count) {
                    break;
                }
            }
        }
        writeFooter();
        output.close();
    }

    private void createWriter() throws IOException {
        output = new BufferedWriter(new FileWriter(requestFileName));
    }

    private void writeHeader() throws IOException {
        output.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        output.write("<RequestList>\n");
    }

    private void writeFooter() throws IOException {
        output.write("</RequestList>\n");
    }

    private void writeRequest(String inputFileName, String outputFileName) throws IOException {
        output.write(" <Request type=\"" + requestType + "\">\n");
        output.write("  <InputProduct  file=\"" + inputFileName + "\" />\n");
        output.write("  <OutputProduct file=\"" + outputFileName + "\"  format=\"BEAM-DIMAP\" />\n");
        output.write(" </Request>\n");
    }

    //////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            print_usage();
        }
        final String configFileName = args[0];
        final String inDirName = args[1];
        final String outDirName = args[2];

        File configFile = new File(configFileName);
        if (configFile != null && configFile.exists()) {
            Properties props = loadConfigProperties(configFile);
            File inputDir = new File(inDirName);
            if (inputDir == null || !inputDir.exists() || !inputDir.isDirectory()) {
                print_usage();
                throw new IllegalArgumentException("The input directory: " + inDirName + " doesn't exist.");
            }
            File outputDir = new File(outDirName);
            if (outputDir == null || !outputDir.exists() || !outputDir.isDirectory()) {
                print_usage();
                throw new IllegalArgumentException("The output directory: " + outDirName + " doesn't exist.");
            }

            RequestGenerator generator = new RequestGenerator(props);
            generator.generateRequest(inputDir, outputDir);
        } else {
            print_usage();
        }
    }

    private static Properties loadConfigProperties(File configFile) throws IOException {
        Properties props = new Properties();
        FileInputStream inStream = new FileInputStream(configFile);
        props.load(inStream);
        inStream.close();
        return props;
    }

    private static void print_usage() {
        System.out.println("usage:   config_file  input_dir  output_dir");
    }
}