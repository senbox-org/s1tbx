package org.esa.nest.AutoTests;

import org.esa.nest.TestAutomatedGraphProcessing;

/**
 * Runs graphs as directed by the tests config file
 */
public class TestStandardGraphs extends TestAutomatedGraphProcessing {

    protected String getTestFileName() {
        return "autoTest_standard.tests";
    }
}
