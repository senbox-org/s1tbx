package org.esa.beam.visat.actions.imgfilter;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Norman
 */
public class FilterSetTest {


    @Test
    public void testXml() throws Exception {

        FilterSet filterSet = new FilterSet("Test", true);
        filterSet.addFilterModel(new Filter("Wraw", "w", 2, 3, new double[]{1, 2, 3, 2, 3, 4}, 2.0));
        filterSet.addFilterModel(new Filter("Gruul", "g", Filter.Operation.ERODE, 4, 2));

        XStream xStream = filterSet.createXStream();
        String xml = xStream.toXML(filterSet);
        //System.out.println(xml);
        Assert.assertEquals("" +
                "<filterSet>\n" +
                "  <name>Test</name>\n" +
                "  <editable>true</editable>\n" +
                "  <filters>\n" +
                "    <filter>\n" +
                "      <name>Wraw</name>\n" +
                "      <shorthand>w</shorthand>\n" +
                "      <operation>CONVOLVE</operation>\n" +
                "      <editable>false</editable>\n" +
                "      <tags/>\n" +
                "      <kernelElements>1,2,3,2,3,4</kernelElements>\n" +
                "      <kernelWidth>2</kernelWidth>\n" +
                "      <kernelHeight>3</kernelHeight>\n" +
                "      <kernelQuotient>2.0</kernelQuotient>\n" +
                "      <kernelOffsetX>1</kernelOffsetX>\n" +
                "      <kernelOffsetY>1</kernelOffsetY>\n" +
                "    </filter>\n" +
                "    <filter>\n" +
                "      <name>Gruul</name>\n" +
                "      <shorthand>g</shorthand>\n" +
                "      <operation>ERODE</operation>\n" +
                "      <editable>false</editable>\n" +
                "      <tags/>\n" +
                "      <kernelElements>1,1,1,1,1,1,1,1</kernelElements>\n" +
                "      <kernelWidth>4</kernelWidth>\n" +
                "      <kernelHeight>2</kernelHeight>\n" +
                "      <kernelQuotient>1.0</kernelQuotient>\n" +
                "      <kernelOffsetX>2</kernelOffsetX>\n" +
                "      <kernelOffsetY>1</kernelOffsetY>\n" +
                "    </filter>\n" +
                "  </filters>\n" +
                "</filterSet>",
                xml);
    }

}
