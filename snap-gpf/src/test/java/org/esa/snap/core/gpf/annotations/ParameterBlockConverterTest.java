package org.esa.snap.core.gpf.annotations;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author Norman Fomferra
 */
public class ParameterBlockConverterTest {
    @Test
    public void testSimpleStruct() throws ConversionException, ValidationException {
        final ParameterBlockConverter converter = new ParameterBlockConverter();
        final String xml = converter.convertObjectToXml(new SimpleStruct("LM", 0.23));
        Assert.assertEquals("<parameters>\n" +
                                    "  <algorithm>LM</algorithm>\n" +
                                    "  <threshold>0.23</threshold>\n" +
                                    "</parameters>", xml);
        final Map<String, Object> map = converter.convertXmlToMap(xml, SimpleStruct.class);
        Assert.assertNotNull(map);
        Assert.assertEquals("LM", map.get("algorithm"));
        Assert.assertEquals(0.23, (Double) map.get("threshold"), 0.0);
    }

    public static class SimpleStruct {

        @Parameter
        String algorithm;

        @Parameter
        double threshold;

        public SimpleStruct(String algorithm, double threshold) {
            this.algorithm = algorithm;
            this.threshold = threshold;
        }

    }
}
