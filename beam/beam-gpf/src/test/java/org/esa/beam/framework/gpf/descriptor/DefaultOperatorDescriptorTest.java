package org.esa.beam.framework.gpf.descriptor;

import org.esa.beam.framework.gpf.Operator;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class DefaultOperatorDescriptorTest {

    @Test
    public void testDefaultProperties() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        assertNull(operatorDescriptor.getName());
        assertNull(operatorDescriptor.getAlias());
        assertNull(operatorDescriptor.getVersion());
        assertNull(operatorDescriptor.getDescription());
        assertArrayEquals(new ParameterDescriptor[0], operatorDescriptor.getParameterDescriptors());
        assertArrayEquals(new SourceProductDescriptor[0], operatorDescriptor.getSourceProductDescriptors());
        assertArrayEquals(new TargetPropertyDescriptor[0], operatorDescriptor.getTargetPropertyDescriptors());
        assertSame(Operator.class, operatorDescriptor.getOperatorClass());
    }
}
