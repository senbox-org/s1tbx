package org.esa.beam.binning.operator;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class BinningOpRobustnessTest {

    @Before
    public void setUp() throws Exception {
        BinningOpTest.TESTDATA_DIR.mkdir();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteTree(BinningOpTest.TESTDATA_DIR);
    }


    @Test
    public void testNoSourceProductSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        testThatOperatorExceptionIsThrown(binningOp, ".*or parameter 'sourceProductPaths' must be.*");
    }

    @Test
    public void testBinningConfigNotSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
        testThatOperatorExceptionIsThrown(binningOp, ".*parameter 'binningConfig'.*");
    }

    @Test
    public void testInvalidConfigsSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
        binningOp.setBinningConfig(new BinningConfig());        // not ok, numRows == 0
        testThatOperatorExceptionIsThrown(binningOp, ".*parameter 'binningConfig.numRows'.*");
    }

//    @Test
//    public void testNoStartDateSet() throws Exception {
//        final BinningOp binningOp = new BinningOp();
//        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
//        binningOp.setBinningConfig(BinningOpTest.createBinningConfig());
//        binningOp.setFormatterConfig(BinningOpTest.createFormatterConfig());
//        testThatOperatorExceptionIsThrown(binningOp, ".*determine 'startDate'.*");
//    }

//    @Test
//    public void testNoEndDateSet() throws Exception {
//        final BinningOp binningOp = new BinningOp();
//        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
//        binningOp.setStartDate("2007-06-21");
//        binningOp.setBinningConfig(BinningOpTest.createBinningConfig());
//        binningOp.setFormatterConfig(BinningOpTest.createFormatterConfig());
//        testThatOperatorExceptionIsThrown(binningOp, ".*determine 'endDate'.*");
//    }

    private void testThatOperatorExceptionIsThrown(BinningOp binningOp, String regex) {
        String message = "OperatorException expected with message regex: " + regex;
        try {
            binningOp.getTargetProduct();
            fail(message);
        } catch (OperatorException e) {
            assertTrue(message + ", got [" + e.getMessage() + "]", Pattern.matches(regex, e.getMessage()));
        }
    }

}
