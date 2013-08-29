package org.esa.beam.dataio;


import java.util.ArrayList;
import java.util.Iterator;

class ProductReaderList implements Iterable<TestDefinition>{

    private ArrayList<TestDefinition> testReaders;

    ProductReaderList() {
        testReaders = new ArrayList<TestDefinition>();
    }

    ArrayList<TestDefinition> getTestReaders() {
        return testReaders;
    }

    void add(TestDefinition testDefinition) {
        testReaders.add(testDefinition);
    }

    void setTestReaders(ArrayList<TestDefinition> testReaders) {
        this.testReaders = testReaders;
    }

    @Override
    public Iterator<TestDefinition> iterator() {
        return testReaders.iterator();
    }
}
