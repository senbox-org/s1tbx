package org.esa.nest;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.nest.gpf.GPFProcessor;
import org.esa.nest.util.MemUtils;
import org.esa.nest.util.ProcessTimeMonitor;
import org.esa.nest.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Runs graphs as directed by the tests config file
 */
public abstract class TestAutomatedGraphProcessing {

    private final boolean failOnFirstProblem = true;
    protected TestConfig config = null;

    protected abstract String getTestFileName();

    @Before
    public void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        config = new TestConfig(getTestFileName());
    }

    @Test
    public void testAutomatedGraphProcessing() throws Throwable {
        if(config == null)
            throw new Exception("Config not initialized in test");

        final ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();
        long totalTime = 0;

        final List<TestInfo> testList = config.getTestList();
        for(TestInfo test : testList) {
            try {
                final ArrayList<File> productList = new ArrayList<>(100);
                TestUtils.recurseFindReadableProducts(test.inputFolder, productList, config.getMaxProductsPerInputFolder());
                int c;
                int numFiles = productList.size();

                TestUtils.log.info(test.num+" "+test.graphFile.getAbsolutePath()+" on "+test.inputFolder);

                if(test.expectedFolder != null) {
                    // generate expected if needed
                    c = 1;
                    for(File file : productList) {
                        try {
                            final String expectedName = pad(c)+'_'+file.getName();
                            final File expectedFile = getOutputProductFile(new File(test.expectedFolder, expectedName));
                            if(!expectedFile.exists()) {
                                TestUtils.log.info(test.num+" ("+c+" of "+numFiles+") "+
                                        "Generating expected " + test.graphFile.getName() +' '+ expectedName);

                                final GPFProcessor proc = new GPFProcessor(test.graphFile);
                                proc.setIO(file, new File(test.expectedFolder, expectedName), null);
                                proc.executeGraph(ProgressMonitor.NULL);

                                MemUtils.freeAllMemory();
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
                    final String outputName = pad(c)+'_'+file.getName();
                    TestUtils.log.info(test.num+" ("+c+" of "+numFiles+") "+
                            "Processing " + test.graphFile.getName() + ' ' + outputName);

                    // Start Benchmark
                    timeMonitor.start();

                    final GPFProcessor proc = new GPFProcessor(test.graphFile);
                    proc.setIO(file, new File(test.outputFolder, outputName), null);
                    proc.executeGraph(ProgressMonitor.NULL);

                    final long duration = timeMonitor.stop();
                    totalTime += duration;
                    TestUtils.log.info(test.num+" time: "+ ProcessTimeMonitor.formatDuration(duration) +" ("+ duration +" s)");
                    // End Benchmark

                    if(test.expectedFolder != null) {
                        final File outputFile = getOutputProductFile(new File(test.outputFolder, outputName));
                        final File expectedFile = getOutputProductFile(new File(test.expectedFolder, outputName));

                        if(outputFile.exists()) {
                            final Product outputProduct = ProductIO.readProduct(outputFile);
                            final Product expectedProduct = ProductIO.readProduct(expectedFile);

                            // compare output to expected
                            TestUtils.compareProducts(outputProduct, expectedProduct);
                        }
                    }

                    MemUtils.freeAllMemory();
                    ++c;
                }

                long duration = totalTime / (long)(c);
                TestUtils.log.info(test.num+" total time: "+ ProcessTimeMonitor.formatDuration(totalTime)
                        +", avg time: "+ ProcessTimeMonitor.formatDuration(duration) +" ("+ duration +" s)");

            } catch(Throwable t) {
                if(failOnFirstProblem)
                    throw t;
                else
                    t.printStackTrace();
            }
        }
    }

    private static String pad(final int c) {
        StringBuilder str = new StringBuilder(String.valueOf(c));
        if(c < 1000) {
            str.insert(0, '0');
        }
        if(c < 100) {
            str.insert(0, '0');
        }
        if(c < 10) {
            str.insert(0, '0');
        }
        return str.toString();
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
            final File[] files = file.getParentFile().listFiles();
            boolean found = false;
            if(files != null) {
                for(File f : files) {
                    if(!f.isDirectory() && f.getName().startsWith(file.getName())) {
                        file = f;
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                file = new File(file.getParentFile(), file.getName()+".dim");
            }
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
}
