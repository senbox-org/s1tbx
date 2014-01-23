package org.esa.beam.dataio;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TestDefinitionTestList {

    private TestDefinitionList testDefinitionList;

    @Before
    public void setUp() {
        testDefinitionList = new TestDefinitionList();
    }

    @Test
    public void testConstruction() {
        final Iterator<TestDefinition> iterator = testDefinitionList.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddDefinition() {
        final TestDefinition definition = new TestDefinition();

        testDefinitionList.add(definition);
        final Iterator<TestDefinition> iterator = testDefinitionList.iterator();
        assertTrue(iterator.hasNext());
        assertSame(definition, iterator.next());
    }
}
