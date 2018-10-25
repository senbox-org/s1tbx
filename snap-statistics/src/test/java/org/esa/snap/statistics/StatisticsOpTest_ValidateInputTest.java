package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class StatisticsOpTest_ValidateInputTest {

    private StatisticsOp statisticsOp;

    @Before
    public void setUp() throws Exception {
        statisticsOp = new StatisticsOp();
        statisticsOp.setParameterDefaultValues();
        statisticsOp.startDate = ProductData.UTC.parse("2010-01-31 14:45:23", "yyyy-MM-ss hh:mm:ss");
        statisticsOp.endDate = ProductData.UTC.parse("2010-01-31 14:46:23", "yyyy-MM-ss hh:mm:ss");
        statisticsOp.accuracy = 0;
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testValidation_PrecisionLessThanMinPrecision() {
        statisticsOp.accuracy = -1;

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertEquals("Parameter 'accuracy' must be greater than or equal to 0", expected.getMessage());
        }
    }

    @Test
    public void testValidation_PrecisionGreaterThanMaxPrecision() {
        statisticsOp.accuracy = 7;

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertEquals("Parameter 'accuracy' must be less than or equal to 6", expected.getMessage());
        }
    }

    @Test
    public void testStartDateHasToBeBeforeEndDate() throws Exception {
        statisticsOp.startDate = ProductData.UTC.parse("2010-01-31 14:46:23", "yyyy-MM-ss hh:mm:ss");
        statisticsOp.endDate = ProductData.UTC.parse("2010-01-31 14:45:23", "yyyy-MM-ss hh:mm:ss");

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("before start date"));
        }
    }

    @Test
    public void testSourceProductsMustBeGiven() {
        statisticsOp.sourceProducts = null;
        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must be given"));
        }

        statisticsOp.sourceProducts = new Product[0];
        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must be given"));
        }
    }

    @Test
    public void testInvalidBandConfiguration() throws IOException {
        final BandConfiguration configuration = new BandConfiguration();
        statisticsOp.bandConfigurations = new BandConfiguration[]{configuration};

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must contain either a source band name or an expression"));
        }

        configuration.expression = "algal_2 * PI";
        configuration.sourceBandName = "bandname";
        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must contain either a source band name or an expression"));
        }
    }

    @Test
    public void testValidBandConfiguration() throws IOException {
        final BandConfiguration configuration = new BandConfiguration();
        statisticsOp.bandConfigurations = new BandConfiguration[]{configuration};

        configuration.expression = "algal_2 * PI";
        statisticsOp.validateInput();

        configuration.expression = null;
        configuration.sourceBandName = "bandname";
        statisticsOp.validateInput();
    }
    @Test

    public void testWithNoPercentiles() throws IOException {
        final BandConfiguration configuration = new BandConfiguration();
        statisticsOp.bandConfigurations = new BandConfiguration[]{configuration};
        configuration.expression = "algal_2 * PI";

        statisticsOp.percentiles = null;
        statisticsOp.validateInput();

    }
}
