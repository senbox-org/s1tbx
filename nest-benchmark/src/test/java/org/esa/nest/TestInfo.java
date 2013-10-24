package org.esa.nest;

import java.io.File;

/**
 * Test Info
 */
public class TestInfo {

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
        if(expected_results != null)
            this.expectedFolder = new File(expected_results);
        else
            this.expectedFolder = null;
        this.outputFolder = new File(output_products);
    }
}
