package org.esa.beam.dataio;


import java.util.ArrayList;
import java.util.Iterator;

class TestDefinitionList implements Iterable<TestDefinition> {

    private ArrayList<TestDefinition> testReaders;

    TestDefinitionList() {
        testReaders = new ArrayList<TestDefinition>();
    }

    void add(TestDefinition testDefinition) {
        testReaders.add(testDefinition);
    }

    @Override
    public Iterator<TestDefinition> iterator() {
        return testReaders.iterator();
    }
}
