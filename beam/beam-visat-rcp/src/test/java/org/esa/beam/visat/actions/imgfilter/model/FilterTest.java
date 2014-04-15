package org.esa.beam.visat.actions.imgfilter.model;

import com.thoughtworks.xstream.XStream;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class FilterTest {
    @Test
    public void testGetElementsAsText() throws Exception {
        Filter filter = Filter.create(5);
        filter.setKernelSize(3, 2);
        filter.setKernelElements(new double[]{1, 2, 3, 2, 3, 4});
        String elementsAsText = filter.getKernelElementsAsText();
        assertEquals("1\t2\t3\n2\t3\t4", elementsAsText);
    }

    @Test
    public void testSetElementsFromText() throws Exception {
        Filter filter = Filter.create(5);
        filter.setKernelSize(3, 2);
        filter.setKernelElements(new double[]{1, 2, 3, 4, 5, 6});
        filter.setKernelElementsFromText("1,2,3\n4,5,6\n");
        assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6}, filter.getKernelElements(), 1.e-10);
        filter.setKernelElementsFromText("2;3;4\n5;6;7\n");
        assertArrayEquals(new double[]{2, 3, 4, 5, 6, 7}, filter.getKernelElements(), 1.e-10);
        filter.setKernelElementsFromText("3\t4\t5\n6\t7\t8\n");
        assertArrayEquals(new double[]{3, 4, 5, 6, 7, 8}, filter.getKernelElements(), 1.e-10);
        filter.setKernelElementsFromText("2 3 4\n5 6 7\n");
        assertArrayEquals(new double[]{2, 3, 4, 5, 6, 7}, filter.getKernelElements(), 1.e-10);
    }

    @Test
    public void testXml() throws Exception {
        Filter filter = Filter.create(5);
        filter.setTags("sharpen", "all");
        XStream xStream = Filter.createXStream();
        String xml = xStream.toXML(filter);
        assertTrue(xml.startsWith("" +
                                  "<filter>\n" +
                                  "  <name>" + filter.getName() + "</name>\n" +
                                  "  <shorthand>" + filter.getShorthand() + "</shorthand>\n" +
                                  "  <operation>CONVOLVE</operation>\n" +
                                  "  <editable>false</editable>\n" +
                                  "  <tags>\n"
        ));
        assertTrue(xml.contains("    <string>sharpen</string>\n"));
        assertTrue(xml.contains("    <string>all</string>\n"));
        assertTrue(xml.endsWith("" +
                             "  </tags>\n" +
                             "  <kernelElements>0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0</kernelElements>\n" +
                             "  <kernelWidth>5</kernelWidth>\n" +
                             "  <kernelHeight>5</kernelHeight>\n" +
                             "  <kernelQuotient>1.0</kernelQuotient>\n" +
                             "  <kernelOffsetX>2</kernelOffsetX>\n" +
                             "  <kernelOffsetY>2</kernelOffsetY>\n" +
                             "</filter>"));
    }

}
