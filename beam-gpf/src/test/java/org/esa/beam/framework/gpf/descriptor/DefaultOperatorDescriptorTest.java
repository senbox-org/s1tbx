package org.esa.beam.framework.gpf.descriptor;

import com.thoughtworks.xstream.XStream;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class DefaultOperatorDescriptorTest {

    private XStream xStream;

    @Before
    public void setUp() throws Exception {
        xStream = createXSTream();
    }

    @Test
    public void testEmptyDescriptor() throws Exception {
        assertXmlCorrect(new DefaultOperatorDescriptor(), "<operator/>");
    }

    @Test
    public void testSimpleOne() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.beam.framework.gpf.jpy.PyOperator";
        operatorDescriptor.alias = "PyOp";
        operatorDescriptor.internal = true;
        operatorDescriptor.label = "Python operator";
        assertXmlCorrect(operatorDescriptor, "<operator>\n" +
                                             "  <name>org.esa.beam.framework.gpf.jpy.PyOperator</name>\n" +
                                             "  <alias>PyOp</alias>\n" +
                                             "  <label>Python operator</label>\n" +
                                             "  <internal>true</internal>\n" +
                                             "</operator>");
    }

    @Test
    public void testPyOp() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.beam.framework.gpf.jpy.PyOperator";
        operatorDescriptor.alias = "MyPyOp";
        operatorDescriptor.internal = true;
        operatorDescriptor.label = "Python operator";
        DefaultSourceProductDescriptor sourceProductDescriptor1 = new DefaultSourceProductDescriptor();
        sourceProductDescriptor1.name = "masterProduct";
        sourceProductDescriptor1.alias = "master";
        DefaultSourceProductDescriptor sourceProductDescriptor2 = new DefaultSourceProductDescriptor();
        sourceProductDescriptor2.name = "slaveProduct";
        sourceProductDescriptor2.alias = "slave";
        sourceProductDescriptor2.optional = true;
        operatorDescriptor.sourceProductDescriptors = new DefaultSourceProductDescriptor[]{
                sourceProductDescriptor1,
                sourceProductDescriptor2
        };

        DefaultParameterDescriptor parameterDescriptor1 = new DefaultParameterDescriptor();
        parameterDescriptor1.name = "threshold";
        parameterDescriptor1.dataType = Double.TYPE;
        DefaultParameterDescriptor parameterDescriptor2 = new DefaultParameterDescriptor();
        parameterDescriptor2.name = "ignoreInvalids";
        parameterDescriptor2.dataType = Boolean.TYPE;
        DefaultParameterDescriptor parameterDescriptor3 = new DefaultParameterDescriptor();
        parameterDescriptor3.name = "algorithmName";
        parameterDescriptor3.dataType = String.class;
        operatorDescriptor.parameterDescriptors = new DefaultParameterDescriptor[]{
                parameterDescriptor1,
                parameterDescriptor2,
                parameterDescriptor3,
        };

        operatorDescriptor.targetProductDescriptor = new DefaultTargetProductDescriptor();

        assertXmlCorrect(operatorDescriptor, "<operator>\n" +
                                             "  <name>org.esa.beam.framework.gpf.jpy.PyOperator</name>\n" +
                                             "  <alias>MyPyOp</alias>\n" +
                                             "  <label>Python operator</label>\n" +
                                             "  <internal>true</internal>\n" +
                                             "  <namedSourceProducts>\n" +
                                             "    <sourceProduct>\n" +
                                             "      <name>masterProduct</name>\n" +
                                             "      <alias>master</alias>\n" +
                                             "    </sourceProduct>\n" +
                                             "    <sourceProduct>\n" +
                                             "      <name>slaveProduct</name>\n" +
                                             "      <alias>slave</alias>\n" +
                                             "      <optional>true</optional>\n" +
                                             "    </sourceProduct>\n" +
                                             "  </namedSourceProducts>\n" +
                                             "  <parameters>\n" +
                                             "    <parameter>\n" +
                                             "      <name>threshold</name>\n" +
                                             "      <dataType>double</dataType>\n" +
                                             "    </parameter>\n" +
                                             "    <parameter>\n" +
                                             "      <name>ignoreInvalids</name>\n" +
                                             "      <dataType>boolean</dataType>\n" +
                                             "    </parameter>\n" +
                                             "    <parameter>\n" +
                                             "      <name>algorithmName</name>\n" +
                                             "      <dataType>java.lang.String</dataType>\n" +
                                             "    </parameter>\n" +
                                             "  </parameters>\n" +
                                             "  <targetProduct/>\n" +
                                             "</operator>");
    }

    @Test
    public void testCollocOp() {
        URL url = getClass().getResource("CollocOp-descriptor.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(url);
        assertXmlCorrect(descriptor, "<operator>\n" +
                                     "  <name>com.acme.CollocOp</name>\n" +
                                     "  <alias>colloc</alias>\n" +
                                     "  <label>Collocation</label>\n" +
                                     "  <internal>true</internal>\n" +
                                     "  <namedSourceProducts>\n" +
                                     "    <sourceProduct>\n" +
                                     "      <name>masterProduct</name>\n" +
                                     "      <alias>master</alias>\n" +
                                     "    </sourceProduct>\n" +
                                     "    <sourceProduct>\n" +
                                     "      <name>slaveProduct</name>\n" +
                                     "      <alias>slave</alias>\n" +
                                     "      <optional>true</optional>\n" +
                                     "    </sourceProduct>\n" +
                                     "  </namedSourceProducts>\n" +
                                     "  <parameters>\n" +
                                     "    <parameter>\n" +
                                     "      <name>threshold</name>\n" +
                                     "      <dataType>double</dataType>\n" +
                                     "    </parameter>\n" +
                                     "    <parameter>\n" +
                                     "      <name>ignoreInvalids</name>\n" +
                                     "      <dataType>boolean</dataType>\n" +
                                     "    </parameter>\n" +
                                     "    <parameter>\n" +
                                     "      <name>algorithmName</name>\n" +
                                     "      <dataType>java.lang.String</dataType>\n" +
                                     "    </parameter>\n" +
                                     "  </parameters>\n" +
                                     "  <targetProduct/>\n" +
                                     "</operator>");
    }

    @Test
    public void testStatsOp() {
        URL url = getClass().getResource("StatsOp-descriptor.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(url);
        assertXmlCorrect(descriptor, "<operator>\n" +
                                     "  <name>com.acme.StatsOp</name>\n" +
                                     "  <alias>stats</alias>\n" +
                                     "  <sourceProducts>\n" +
                                     "    <count>-1</count>\n" +
                                     "    <bands>\n" +
                                     "      <string>band_1</string>\n" +
                                     "      <string>band_2</string>\n" +
                                     "      <string>band_3</string>\n" +
                                     "    </bands>\n" +
                                     "  </sourceProducts>\n" +
                                     "  <parameters>\n" +
                                     "    <parameter>\n" +
                                     "      <name>startDate</name>\n" +
                                     "      <dataType>java.util.Date</dataType>\n" +
                                     "    </parameter>\n" +
                                     "    <parameter>\n" +
                                     "      <name>endDate</name>\n" +
                                     "      <dataType>java.util.Date</dataType>\n" +
                                     "    </parameter>\n" +
                                     "    <parameter>\n" +
                                     "      <name>resolution</name>\n" +
                                     "      <dataType>double</dataType>\n" +
                                     "    </parameter>\n" +
                                     "  </parameters>\n" +
                                     "  <targetProperties>\n" +
                                     "    <targetProperty>\n" +
                                     "      <name>count</name>\n" +
                                     "      <dataType>int</dataType>\n" +
                                     "    </targetProperty>\n" +
                                     "    <targetProperty>\n" +
                                     "      <name>monthlyAvg</name>\n" +
                                     "      <dataType>double</dataType>\n" +
                                     "    </targetProperty>\n" +
                                     "  </targetProperties>\n" +
                                     "</operator>");
    }

    private void assertXmlCorrect(DefaultOperatorDescriptor operatorDescriptor, String expected) {
        StringWriter sw = new StringWriter();
        xStream.toXML(operatorDescriptor, sw);
        assertEquals(expected, sw.toString());
    }

    private XStream createXSTream() {
        XStream xStream = new XStream();
        xStream.alias("operator", DefaultOperatorDescriptor.class);

        xStream.alias("sourceProduct", DefaultSourceProductDescriptor.class);
        xStream.aliasField("namedSourceProducts", DefaultOperatorDescriptor.class, "sourceProductDescriptors");

        xStream.alias("sourceProducts", DefaultSourceProductsDescriptor.class);
        xStream.aliasField("sourceProducts", DefaultOperatorDescriptor.class, "sourceProductsDescriptor");

        xStream.alias("parameter", DefaultParameterDescriptor.class);
        xStream.aliasField("parameters", DefaultOperatorDescriptor.class, "parameterDescriptors");

        xStream.alias("targetProduct", DefaultTargetProductDescriptor.class);
        xStream.aliasField("targetProduct", DefaultOperatorDescriptor.class, "targetProductDescriptor");

        xStream.alias("targetProperty", DefaultTargetPropertyDescriptor.class);
        xStream.aliasField("targetProperties", DefaultOperatorDescriptor.class, "targetPropertyDescriptors");

        return xStream;
    }
}
