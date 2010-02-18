package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeRunnable;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.Rectangle;
import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Used to execute a processing {@link Graph} defined
 * in an XML file. Use the following command line argumetns to substitute template
 * variables inside of the graph definition:
 * <p/>
 * <ul>
 * <li>-i &#60inputproductfile1&#62,&#60inputproductfile2&#62,... : substitutes
 * ${inputproductfile1}, ${inputproductfile2}, ... with the given file paths.
 * <li>-o -o &#60outputfile1&#62,&#60outputfile2&#62,... : substitues
 * ${outputfile1}, ${outputfile2}, ... with the given file paths.
 * <li>-v variablename=value : substitutes ${variablename} with the given value.
 * <li>-p &#60propertiesfile&#62
 * </ul>
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class GraphProcessorMain implements RuntimeRunnable {

    /**
     * The main entry point when started as an application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        run(args, ProgressMonitor.NULL);
    }

    /**
     * The main entry point when started as a Ceres module.
     *
     * @param object parameters to run this instance, expected as {@code String[]}
     * @param pm     a progress monitor. Can be used to signal progress.
     * @throws Exception if any error occures
     */
    public void run(Object object, ProgressMonitor pm) throws Exception {
        String[] args = new String[0];
        if (object instanceof String[]) {
            args = (String[]) object;
        }

        run(args, pm);
    }

    private static void run(String[] args, ProgressMonitor pm) {
        if (args.length < 1) {
            printUsageMessage();
            return;
        }
        Map variables = parseArgs(args);
        Logger logger = BeamLogManager.getSystemLogger();

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        try {
            StringReader reader = new StringReader(readConfigFile(args[0]));
            // load graph
            Graph graph = GraphIO.read(reader, variables);

            // execute graph
            GraphProcessor processor = new GraphProcessor();
            processor.setLogger(logger);
            processor.addObserver(new GraphProcessingObserver() {
                public void graphProcessingStarted(GraphContext graphContext) {
                    graphContext.getLogger().info("graph processing started: " + graphContext.getGraph().getId());
                }

                public void graphProcessingStopped(GraphContext graphContext) {
                    graphContext.getLogger().info("graph processing stopped: " + graphContext.getGraph().getId());
                }

                public void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle) {
                    graphContext.getLogger().info(
                            "tile processing started: " + graphContext.getGraph().getId() + ", " + tileRectangle);
                }

                public void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle) {
                    graphContext.getLogger().info(
                            "tile processing stopped: " + graphContext.getGraph().getId() + ", " + tileRectangle);
                }
            });
            processor.executeGraph(graph, pm);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void printUsageMessage() {
        System.out.println("Usage : java GraphProcessorMain <graphDefinitionFile> [-options]");
        System.out.println("\nwhere options include:\n");
        System.out.println("-i <inputFile1>,<inputFile2>,...");
        System.out.println("-o <outputFile1>,<outputFile2>,...");
        System.out.println("-v <variableName>=<value>");
        System.out.println("-p <propertiesFile>");
    }

    private static Properties parseArgs(String[] args) {
        if (args.length < 2) {
            return null;
        }
        Properties argsMap = new Properties();
        for (int i = 1; i < args.length; i++) {
            // -o Output Files: a commaseperated list of output filepaths mapped
            // to "outputFile1", "outputFile2", ....
            if (args[i].equals("-o")) {
                String[] filePaths = args[i + 1].split(",");
                for (int j = 0; j < filePaths.length; j++) {
                    argsMap.put("outputFile" + (j + 1), filePaths[j]);
                }
                argsMap.put("outputFile", filePaths[0]);
            }
            // -i Iutput Files: a commaseperated list of input filepaths mapped
            // to "inputFile1", "inputFile2", ....
            else if (args[i].equals("-i")) {
                String[] filePaths = args[i + 1].split(",");
                for (int j = 0; j < filePaths.length; j++) {
                    argsMap.put("inputFile" + (j + 1), filePaths[j]);
                }
                argsMap.put("inputFile", filePaths[0]);
            }
            // -v Variables: -v variablename=value is a mapping from
            // variablename to value
            else if (args[i].equals("-v")) {
                String[] variableMapping = args[i + 1].split("=");
                argsMap.put(variableMapping[0], variableMapping[1]);
            }
            // -p Properties File: the filepath of a properties file
            else if (args[i].equals("-p")) {
                String propsFilePath = args[i + 1];
                FileInputStream inStream = null;
                try {
                    inStream = new FileInputStream(propsFilePath);
                    argsMap.load(inStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            String msg = String.format("Could not clode properties file: '%s' correctly.", propsFilePath);
                            throw new IllegalArgumentException(msg , e);
                        }
                    }
                }
            }
        }
        return argsMap;
    }

    private static String readConfigFile(String configFilePath) throws IOException {
        StringBuilder xmlRequest = new StringBuilder();
        FileReader inputData = null;
        try {
            int character;
            inputData = new FileReader(configFilePath);
            while ((character = inputData.read()) != -1) {
                xmlRequest.append((char) character);
            }
        } finally {
            if (inputData != null) {
                inputData.close();
            }
        }
        return xmlRequest.toString();
    }
}
