package org.esa.nest;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.PropertyMap;
import org.esa.nest.gpf.GPFProcessor;
import org.esa.nest.util.Config;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Runs graphs as directed by the tests config file
 */
public class TestAutomatedGraphProcessing extends TestCase {

    private static final PropertyMap testPreferences = Config.getAutomatedTestConfigPropertyMap();
    private final static String contextID = ResourceUtils.getContextID();

    private final List<TestInfo> testList = new ArrayList<TestInfo>(20);
    private static final Logger log = Logger.getLogger("Test");

    private final boolean failOnFirstProblem = true;

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        importTests();
    }

    @Override
    protected void tearDown() throws Exception {

    }

    public void testAutomatedGraphProcessing() throws Throwable {

        for(TestInfo test : testList) {
            try {
                final ArrayList<File> productList = new ArrayList<>(100);
                TestUtils.recurseFindReadableProducts(test.inputFolder, productList);
                int c = 1;
                int numFiles = productList.size();

                if(test.expectedFolder != null) {
                    // generate expected if needed
                    c = 1;
                    for(File file : productList) {
                        try {
                            final File expectedFile = getOutputProductFile(new File(test.expectedFolder, file.getName()));
                            if(!expectedFile.exists()) {
                                log.info(test.num+" ("+c+" of "+numFiles+") "+
                                        "Generating expected " + test.graphFile.getName() +' '+ file.getName());

                                final GPFProcessor proc = new GPFProcessor(test.graphFile);
                                proc.setIO(file, new File(test.expectedFolder, file.getName()), "BEAM-DIMAP");
                                proc.executeGraph(ProgressMonitor.NULL);
                            }
                            ++c;
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }

                deleteOldOutput(test, productList);

                // process output
                c = 1;
                for(File file : productList) {
                    log.info(test.num+" ("+c+" of "+numFiles+") "+
                            "Processing " + test.graphFile.getName() + ' ' + file.getName());

                    final GPFProcessor proc = new GPFProcessor(test.graphFile);
                    proc.setIO(file, new File(test.outputFolder, file.getName()), "BEAM-DIMAP");
                    proc.executeGraph(ProgressMonitor.NULL);

                    if(test.expectedFolder != null) {
                        final File outputFile = getOutputProductFile(new File(test.outputFolder, file.getName()));
                        final File expectedFile = getOutputProductFile(new File(test.expectedFolder, file.getName()));

                        final Product outputProduct = ProductIO.readProduct(outputFile);
                        final Product expectedProduct = ProductIO.readProduct(expectedFile);

                        // compare output to expected
                        TestUtils.compareProducts(outputProduct, expectedProduct);
                    }
                    ++c;
                }

            } catch(Throwable t) {
                if(failOnFirstProblem)
                    throw t;
                else
                    t.printStackTrace();
            }
        }
    }

    private void importTests() throws Exception {
        final String prefix = contextID+".test.";

        final Properties prop = testPreferences.getProperties();
        final int numProperties = prop.size()/4;
        for(int i=0; i < numProperties; ++i) {
            final String key = prefix+i;
            final String graph = prop.getProperty(key + ".graph");
            if(graph != null) {
                final String input_products = testPreferences.getPropertyString(key+".input_products");
                final String expected_results = testPreferences.getPropertyString(key+".expected_results");
                final String output_products = testPreferences.getPropertyString(key+".output_products");

                if(input_products == null || output_products == null) {
                    throw new Exception("Test configuration "+key+" is incomplete");
                }

                final TestInfo test = new TestInfo(i, graph, input_products, expected_results, output_products);
                if(!test.graphFile.exists())
                    throw new Exception(test.graphFile.getAbsolutePath() +" does not exist for "+key);

                testList.add(test);
            }
        }
    }

    private static void deleteOldOutput(final TestInfo test, final ArrayList<File> productList) throws Throwable {

        for(File file : productList) {
            try {
                deleteProduct(getOutputProductFile(new File(test.outputFolder, file.getName())));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static File getOutputProductFile(File file) {
        if(!file.exists()) {
            file = new File(file.getParentFile(), file.getName()+".dim");
        }
        return file;
    }

    private static boolean deleteProduct(File file) throws IOException {
        if(file.getName().endsWith(".dim")) {
            final File dataFolder = new File(file.getParentFile(), file.getName().replace(".dim", ".data"));

            FileUtils.deleteDirectory(dataFolder);
        }
        return file.delete();
    }

    private static class TestInfo {
        final int num;
        final File graphFile;
        final File inputFolder;
        final File expectedFolder;
        final File outputFolder;

        public TestInfo(final int num, final String graph, final String input_products,
                        final String expected_results, final String output_products) {
            this.num = num;
            this.graphFile = new File(graph);
            this.inputFolder = new File(input_products);
            this.expectedFolder = new File(expected_results);
            this.outputFolder = new File(output_products);
        }
    }
}
