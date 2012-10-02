package com.bc.ceres.metadata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 02.10.12 11:56
 */
public class XPathHandlerTest {

    @Test
    public void testRun_1() throws Exception {
        XPathHandler xPathHandler = new XPathHandler();
        String document =
                "<package>\n" +
                        "    <class name=\"Director\">\n" +
                        "        <field name=\"test1\"/>\n" +
                        "    </class>\n" +
                        "</package>\n";

        String result = xPathHandler.run("//package/class/@name", document);
        assertEquals("Director", result);
    }

    @Test
    public void testRun_2() throws Exception {
        XPathHandler xPathHandler = new XPathHandler();
        String document =
                "<package>\n" +
                        "    <class name=\"Director\">\n" +
                        "        <field name=\"test1\">Headquaters</field>\n" +
                        "    </class>\n" +
                        "</package>";

        String result = xPathHandler.run("//package/class/field", document);
        assertEquals("Headquaters", result);
    }

    @Test
    public void testRun_3() throws Exception {
        XPathHandler xPathHandler = new XPathHandler();
        String document =
                "<package>\n" +
                        "    <class name=\"Director\">\n" +
                        "        <field name=\"test1\">Headquaters</field>\n" +
                        "    </class>\n" +
                        "</package>";

        String result = xPathHandler.run("//field/@name", document);
        assertEquals("test1", result);
    }
}
