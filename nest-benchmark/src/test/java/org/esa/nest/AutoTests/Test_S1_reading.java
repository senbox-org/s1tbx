package org.esa.nest.AutoTests;

import org.esa.nest.TestAutomatedGraphProcessing;

/**
 * Runs graphs as directed by the tests config file
 */
public class Test_S1_reading extends TestAutomatedGraphProcessing {

    protected String getTestFileName() {
        return "autoTest_S1_reading.tests";
    }
}
