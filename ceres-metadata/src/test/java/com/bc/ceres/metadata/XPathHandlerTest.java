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

    @Test
    public void testExtractXml() throws Exception {
        XPathHandler xPathHandler = new XPathHandler();
        String document =
                "  <metadata>" +
                        "  <name>some name</name>" +
                        "  <steps>\n" +
                        "        <step>\n" +
                        "            <processor>\n" +
                        "                <name>Subset</name>\n" +
                        "                <version>1.0</version>\n" +
                        "            </processor>\n" +
                        "            <parameters>\n" +
                        "                <fullSwath>false</fullSwath>\n" +
                        "                <copyMetadata>false</copyMetadata>\n" +
                        "                <subSamplingX>1</subSamplingX>\n" +
                        "                <subSamplingY>1</subSamplingY>\n" +
                        "            </parameters>\n" +
                        "        </step>\n" +
                        "        <step>\n" +
                        "            <processor>\n" +
                        "                <name>Meris.CorrectRadiometry</name>\n" +
                        "                <version>1.1</version>\n" +
                        "            </processor>\n" +
                        "            <parameters>\n" +
                        "                <doRadToRefl>false</doRadToRefl>\n" +
                        "                <doCalibration>true</doCalibration>\n" +
                        "                <doSmile>true</doSmile>\n" +
                        "                <doEqualization>true</doEqualization>\n" +
                        "                <reproVersion>REPROCESSING_3</reproVersion>\n" +
                        "            </parameters>\n" +
                        "        </step>" +
                        "    </steps>" +
                        "</metadata>";

        //execution 1
        String result1 = xPathHandler.extractXml("//steps/step[1]", document);
        final String expected1 = "" +
                "<step>\n" +
                "            <processor>\n" +
                "                <name>Subset</name>\n" +
                "                <version>1.0</version>\n" +
                "            </processor>\n" +
                "            <parameters>\n" +
                "                <fullSwath>false</fullSwath>\n" +
                "                <copyMetadata>false</copyMetadata>\n" +
                "                <subSamplingX>1</subSamplingX>\n" +
                "                <subSamplingY>1</subSamplingY>\n" +
                "            </parameters>\n" +
                "        </step>";
        assertEquals(expected1, result1);

        //execution 2
        String result2 = xPathHandler.extractXml("//steps/step[2]", document);
        final String expected2 = "" +
                "<step>\n" +
                "            <processor>\n" +
                "                <name>Meris.CorrectRadiometry</name>\n" +
                "                <version>1.1</version>\n" +
                "            </processor>\n" +
                "            <parameters>\n" +
                "                <doRadToRefl>false</doRadToRefl>\n" +
                "                <doCalibration>true</doCalibration>\n" +
                "                <doSmile>true</doSmile>\n" +
                "                <doEqualization>true</doEqualization>\n" +
                "                <reproVersion>REPROCESSING_3</reproVersion>\n" +
                "            </parameters>\n" +
                "        </step>";
        assertEquals(expected2, result2);
    }
}
