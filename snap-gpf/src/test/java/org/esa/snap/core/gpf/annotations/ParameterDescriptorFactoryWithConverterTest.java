package org.esa.snap.core.gpf.annotations;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.gpf.TestOps;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ParameterDescriptorFactoryWithConverterTest {

    @Test
    public void testWithSimpleConverter() {
        ParameterDescriptorFactory pdf = new ParameterDescriptorFactory();
        TestOps.OpParameterConverter object = new TestOps.OpParameterConverter();
        PropertyContainer container = PropertyContainer.createObjectBacked(object, pdf);
        Property parameterWithConverter = container.getProperty("parameterWithConverter");
        assertNotNull(parameterWithConverter.getDescriptor().getConverter());
        assertNull(parameterWithConverter.getDescriptor().getPropertySetDescriptor());
    }

    @Test
    public void testWithDomConverter() {
        ParameterDescriptorFactory pdf = new ParameterDescriptorFactory();
        TestOps.OpParameterConverter object = new TestOps.OpParameterConverter();
        PropertyContainer container = PropertyContainer.createObjectBacked(object, pdf);
        Property parameterWithDomConverter = container.getProperty("parameterWithDomConverter");
        assertNotNull(parameterWithDomConverter.getDescriptor().getDomConverter());
        assertNull(parameterWithDomConverter.getDescriptor().getPropertySetDescriptor());
    }

}
