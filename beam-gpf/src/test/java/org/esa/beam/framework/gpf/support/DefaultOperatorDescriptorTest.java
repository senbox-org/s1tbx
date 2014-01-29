package org.esa.beam.framework.gpf.support;

import com.thoughtworks.xstream.XStream;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

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
    public void testNonEmptyDescriptor() throws Exception {
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
    public void testNonEmptyDescriptorWithParametersAndSourcesAndATarget() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.beam.framework.gpf.jpy.PyOperator";
        operatorDescriptor.alias = "PyOp";
        operatorDescriptor.internal = true;
        operatorDescriptor.label = "Python operator";
        DefaultSourceProductDescriptor sourceProductDescriptor1 = new DefaultSourceProductDescriptor();
        sourceProductDescriptor1.name = "masterProduct";
        sourceProductDescriptor1.alias = "master";
        DefaultSourceProductDescriptor sourceProductDescriptor2 = new DefaultSourceProductDescriptor();
        sourceProductDescriptor2.name = "slaveProduct";
        sourceProductDescriptor2.alias = "slave";
        sourceProductDescriptor2.optional = true;
        operatorDescriptor.sourceProductDescriptors = new DefaultSourceProductDescriptor[] {
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
        operatorDescriptor.parameterDescriptors = new DefaultParameterDescriptor[] {
                parameterDescriptor1,
                parameterDescriptor2,
                parameterDescriptor3,
        };

        operatorDescriptor.targetProductDescriptor = new DefaultTargetProductDescriptor();


        assertXmlCorrect(operatorDescriptor, "<operator>\n" +
                                             "  <name>org.esa.beam.framework.gpf.jpy.PyOperator</name>\n" +
                                             "  <alias>PyOp</alias>\n" +
                                             "  <label>Python operator</label>\n" +
                                             "  <internal>true</internal>\n" +
                                             "  <sourcesProducts>\n" +
                                             "    <sourceProduct>\n" +
                                             "      <name>masterProduct</name>\n" +
                                             "      <alias>master</alias>\n" +
                                             "    </sourceProduct>\n" +
                                             "    <sourceProduct>\n" +
                                             "      <name>slaveProduct</name>\n" +
                                             "      <alias>slave</alias>\n" +
                                             "      <optional>true</optional>\n" +
                                             "    </sourceProduct>\n" +
                                             "  </sourcesProducts>\n" +
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
    public void testName() throws Exception {
        DefaultOperatorDescriptor root = new DefaultOperatorDescriptor();
        xStream.fromXML(getClass().getResource("operator-metadata.xml"), root);

        assertEquals("org.esa.beam.framework.gpf.jpy.PyOperator", root.name);
        assertEquals("PyOp", root.alias);
        assertEquals("Python operator", root.label);
        assertEquals(Boolean.TRUE, root.internal);

        assertEquals(2, root.sourceProductDescriptors.length);
        assertEquals("masterProduct", root.sourceProductDescriptors[0].name);
        assertEquals("master", root.sourceProductDescriptors[0].alias);

        assertEquals("slaveProduct", root.sourceProductDescriptors[1].name);
        assertEquals("slave", root.sourceProductDescriptors[1].alias);
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
        xStream.aliasField("sourcesProducts", DefaultOperatorDescriptor.class, "sourceProductDescriptors");

        xStream.alias("parameter", DefaultParameterDescriptor.class);
        xStream.aliasField("parameters", DefaultOperatorDescriptor.class, "parameterDescriptors");

        xStream.alias("targetProduct", DefaultTargetProductDescriptor.class);
        xStream.aliasField("targetProduct", DefaultOperatorDescriptor.class, "targetProductDescriptor");

        xStream.alias("targetProperty", DefaultTargetPropertyDescriptor.class);
        xStream.aliasField("targetProperties", DefaultOperatorDescriptor.class, "targetPropertyDescriptors");

        /*
        xStream.useAttributeFor(Graph.class, "id");
        xStream.addImplicitCollection(Graph.class, "nodeList", Node.class);

        xStream.alias("header", Header.class);

        xStream.alias("target", HeaderTarget.class);
        xStream.useAttributeFor(HeaderTarget.class, "nodeId");
        xStream.aliasAttribute(HeaderTarget.class, "nodeId", "refid");

        xStream.addImplicitCollection(Header.class, "sources", "source", HeaderSource.class);
        xStream.registerConverter(new HeaderSource.Converter());

        xStream.addImplicitCollection(Header.class, "parameters", "parameter", HeaderParameter.class);
        xStream.registerConverter(new HeaderParameter.Converter());

        xStream.alias("node", Node.class);
        xStream.aliasField("operator", Node.class, "operatorName");
        xStream.useAttributeFor(Node.class, "id");

        xStream.alias("sources", SourceList.class);
        xStream.aliasField("sources", Node.class, "sourceList");
        xStream.registerConverter(new SourceList.Converter());

        xStream.alias("parameters", DomElement.class);
        xStream.aliasField("parameters", Node.class, "configuration");
        xStream.registerConverter(new DomElementXStreamConverter());

        xStream.alias("applicationData", ApplicationData.class);
        xStream.addImplicitCollection(Graph.class, "applicationData", ApplicationData.class);
        xStream.registerConverter(new ApplicationData.AppConverter());
        */
        return xStream;
    }
}
