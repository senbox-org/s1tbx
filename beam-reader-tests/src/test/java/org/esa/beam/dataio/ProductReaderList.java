package org.esa.beam.dataio;


import java.util.ArrayList;

class ProductReaderList {

    private ArrayList<TestProductReader> testReaders;

    ProductReaderList() {
        testReaders = new ArrayList<TestProductReader>();
    }

    ArrayList<TestProductReader> getTestReaders() {
        return testReaders;
    }

    void add(TestProductReader testProductReader) {
        testReaders.add(testProductReader);
    }

    void setTestReaders(ArrayList<TestProductReader> testReaders) {
        this.testReaders = testReaders;
    }
}
