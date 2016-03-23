package org.esa.snap.core.gpf.descriptor;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.junit.Test;

import java.io.StringReader;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class DefaultOperatorDescriptorXmlTest {

    @Test
    public void testXmlOfEmptyDescriptor() throws Exception {
        assertXmlCorrect(new DefaultOperatorDescriptor(), "<operator/>");
    }

    @Test
    public void testXmlOfSimpleOne() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.snap.core.gpf.jpy.PyOperator";
        operatorDescriptor.alias = "PyOp";
        operatorDescriptor.internal = true;
        operatorDescriptor.label = "Python operator";
        assertXmlCorrect(operatorDescriptor, "<operator>\n" +
                                             "  <name>org.esa.snap.core.gpf.jpy.PyOperator</name>\n" +
                                             "  <alias>PyOp</alias>\n" +
                                             "  <label>Python operator</label>\n" +
                                             "  <internal>true</internal>\n" +
                                             "</operator>");
    }

    @Test
    public void testXmlOfPyOp() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.snap.core.gpf.jpy.PyOperator";
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
                                             "  <name>org.esa.snap.core.gpf.jpy.PyOperator</name>\n" +
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
                                             "      <dataType>String</dataType>\n" +
                                             "    </parameter>\n" +
                                             "  </parameters>\n" +
                                             "  <targetProduct/>\n" +
                                             "</operator>");
    }

    @Test
    public void testWithStringArrayParameterToXml() throws Exception {
        DefaultOperatorDescriptor operatorDescriptor = new DefaultOperatorDescriptor();
        operatorDescriptor.name = "org.esa.snap.some.Operator";
        DefaultParameterDescriptor parameterDescriptor1 = new DefaultParameterDescriptor();
        parameterDescriptor1.name = "names";
        parameterDescriptor1.dataType = String[].class;
        DefaultParameterDescriptor parameterDescriptor2 = new DefaultParameterDescriptor();
        parameterDescriptor2.name = "pin";
        parameterDescriptor2.dataType = Point[].class;
        operatorDescriptor.parameterDescriptors = new DefaultParameterDescriptor[]{
                parameterDescriptor1,
                parameterDescriptor2,
        };

        operatorDescriptor.targetProductDescriptor = new DefaultTargetProductDescriptor();

        assertXmlCorrect(operatorDescriptor, "<operator>\n" +
                                             "  <name>org.esa.snap.some.Operator</name>\n" +
                                             "  <parameters>\n" +
                                             "    <parameter>\n" +
                                             "      <name>names</name>\n" +
                                             "      <dataType>String[]</dataType>\n" +
                                             "    </parameter>\n" +
                                             "    <parameter>\n" +
                                             "      <name>pin</name>\n" +
                                             "      <dataType>Point[]</dataType>\n" +
                                             "    </parameter>\n" +
                                             "  </parameters>\n" +
                                             "  <targetProduct/>\n" +
                                             "</operator>");
    }

    @Test
    public void testWithStringArrayParameterFromXml() throws Exception {
        DefaultOperatorDescriptor expectedDescriptor = new DefaultOperatorDescriptor();
        expectedDescriptor.name = "org.esa.snap.some.Operator";
        expectedDescriptor.alias = "OpWithArray";
        DefaultParameterDescriptor parameterDescriptor1 = new DefaultParameterDescriptor();
        parameterDescriptor1.name = "names";
        parameterDescriptor1.dataType = String[].class;
        DefaultParameterDescriptor parameterDescriptor2 = new DefaultParameterDescriptor();
        parameterDescriptor2.name = "region";
        parameterDescriptor2.dataType = Polygon[].class;
        expectedDescriptor.parameterDescriptors = new DefaultParameterDescriptor[]{
                parameterDescriptor1,
                parameterDescriptor2,
        };

        expectedDescriptor.targetProductDescriptor = new DefaultTargetProductDescriptor();

        String actualXml = "<operator>\n" +
                          "  <name>org.esa.snap.some.Operator</name>\n" +
                          "  <alias>OpWithArray</alias>\n" +
                          "  <parameters>\n" +
                          "    <parameter>\n" +
                          "      <name>names</name>\n" +
                          "      <dataType>String[]</dataType>\n" +
                          "    </parameter>\n" +
                          "    <parameter>\n" +
                          "      <name>region</name>\n" +
                          "      <dataType>Polygon[]</dataType>\n" +
                          "    </parameter>\n" +
                           "  </parameters>\n" +
                          "  <targetProduct/>\n" +
                          "</operator>";
        DefaultOperatorDescriptor actualDescriptor = DefaultOperatorDescriptor.fromXml(new StringReader(actualXml), "xml-string",
                                                                                                DefaultOperatorDescriptor.class.getClassLoader());
        assertEquals(expectedDescriptor.getName(), actualDescriptor.getName());
        assertEquals(expectedDescriptor.getParameterDescriptors()[0].getName(), actualDescriptor.getParameterDescriptors()[0].getName());
        assertEquals(expectedDescriptor.getParameterDescriptors()[0].getDataType(), actualDescriptor.getParameterDescriptors()[0].getDataType());
    }

    @Test
    public void testXmlOfCollocOp() {
        URL url = getClass().getResource("CollocOp-descriptor.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(url, DefaultOperatorDescriptor.class.getClassLoader());
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
                                     "      <dataType>String</dataType>\n" +
                                     "    </parameter>\n" +
                                     "  </parameters>\n" +
                                     "  <targetProduct/>\n" +
                                     "</operator>");
    }

    @Test
    public void testXmlOfStatsOp() {
        URL url = getClass().getResource("StatsOp-descriptor.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(url, DefaultOperatorDescriptor.class.getClassLoader());
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
                                     "      <dataType>Date</dataType>\n" +
                                     "    </parameter>\n" +
                                     "    <parameter>\n" +
                                     "      <name>endDate</name>\n" +
                                     "      <dataType>Date</dataType>\n" +
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

    private void assertXmlCorrect(DefaultOperatorDescriptor operatorDescriptor, String expectedXml) {
        String actualXml = operatorDescriptor.toXml(DefaultOperatorDescriptor.class.getClassLoader());
        assertEquals(expectedXml, actualXml);
    }
}
