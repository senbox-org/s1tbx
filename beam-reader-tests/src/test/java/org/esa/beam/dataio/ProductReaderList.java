package org.esa.beam.dataio;


import java.util.ArrayList;

class ProductReaderList {

    private ArrayList<TestProductReader> testReaders;

    ArrayList<TestProductReader> getTestReaders() {
        return testReaders;
    }

    void setTestReaders(ArrayList<TestProductReader> testReaders) {
        this.testReaders = testReaders;
    }
}
