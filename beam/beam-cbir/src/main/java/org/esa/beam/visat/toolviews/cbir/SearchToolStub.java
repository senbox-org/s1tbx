package org.esa.beam.visat.toolviews.cbir;

import java.awt.*;

/**
 * Stub for PFA Search Tool
 */
public class SearchToolStub {


    public SearchToolStub() {

    }

    public String[] getAvailableFeatureExtractors(final String mission, final String productType) {
        return new String[] {
            "Algal Bloom Detection", "Urban Area Detection"
        };
    }

    public Dimension getPatchSize(final String featureExtractor) {
        return new Dimension(200, 200);
    }


}
