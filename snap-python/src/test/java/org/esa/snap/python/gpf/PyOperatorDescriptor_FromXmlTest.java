package org.esa.snap.python.gpf;

import org.esa.snap.core.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

public class PyOperatorDescriptor_FromXmlTest {


    @Test
    public void testReadingXml() {
        URL url = getClass().getResource("ExampleOp-info.xml");
        DefaultOperatorDescriptor opDescriptor = DefaultOperatorDescriptor.fromXml(url, DefaultOperatorDescriptor.class.getClassLoader());

        assertEquals("ExampleOp", opDescriptor.getAlias());
        assertEquals("org.esa.snap.ExampleOp", opDescriptor.getName());
        assertEquals(PyOperator.class, opDescriptor.getOperatorClass());
        assertEquals("Description text.", opDescriptor.getDescription());
        assertEquals("SNAP Devs", opDescriptor.getAuthors());
        assertEquals("2017 ESA", opDescriptor.getCopyright());
        assertEquals("Special Test Example Operator", opDescriptor.getLabel());

        assertTrue(opDescriptor.isInternal());
        assertTrue(opDescriptor.isAutoWriteDisabled());

        ParameterDescriptor[] paramDescriptors = opDescriptor.getParameterDescriptors();
        assertEquals(4, paramDescriptors.length);

        ParameterDescriptor algorithmDescriptor = paramDescriptors[0];
        assertEquals("algorithm", algorithmDescriptor.getName());
        assertEquals("Algorithm", algorithmDescriptor.getLabel());
        assertEquals("Algorithm to be used", algorithmDescriptor.getDescription());
        assertEquals(String.class, algorithmDescriptor.getDataType());
        assertEquals("split", algorithmDescriptor.getDefaultValue());
        assertArrayEquals(new String[]{"split", "mono"}, algorithmDescriptor.getValueSet());

        ParameterDescriptor rangeDescriptor = paramDescriptors[3];
        assertEquals("range", rangeDescriptor.getName());
        assertEquals("Allowed range", rangeDescriptor.getDescription());
        assertEquals(int.class, rangeDescriptor.getDataType());
        assertEquals("12", rangeDescriptor.getDefaultValue());
        assertEquals("[10,20)", rangeDescriptor.getInterval());
    }

}
