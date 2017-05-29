package org.esa.snap.core.gpf.main;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * @author Norman Fomferra
 */
public class CommandLineToolTemplateTest {

    private CommandLineTool tool;
    private TestCommandLineContext context;

    private static File sourceFile;
    private static File targetFile;

    @BeforeClass
    public static void initClass() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        sourceFile = new File(tempDir, "test.testdata");
        // only run this test if the file could be created or it already exists
        Assume.assumeTrue(sourceFile.createNewFile() || sourceFile.exists());
        sourceFile.deleteOnExit();
        targetFile = new File("20120607-CHL-1D.testdata");
    }

    @Before
    public void init() throws Exception {
        TestProductIOPlugIn.INSTANCE.clear();
        context = new TestCommandLineContext();
        tool = new CommandLineTool(context);
    }

    @Test
    public void testTemplateMergingWithOpName() throws Exception {
        String metadataPath = "test.properties";
        String parameterPath = "params.xml";
        File templateDir = getTemplateDir();

        TestProductIOPlugIn.INSTANCE.getSourceProducts().put(sourceFile.getAbsoluteFile(), createSourceProduct());

        context.textFiles.put(metadataPath, "" +
                "processingCenter=BC\n" +
                "softwareName=BEAM\n");

        context.textFiles.put(parameterPath, "" +
                "<parameters>\n" +
                "    <pixelSizeX>0.04</pixelSizeX>\n" +
                "    <pixelSizeY>0.02</pixelSizeY>\n" +
                "    <crs>EPSG:4326</crs>\n" +
                "</parameters>\n");


        tool.run("Reproject",
                "-p", parameterPath,
                "-t", targetFile.getPath(),
                "-m", metadataPath,
                "-v", templateDir.getPath(),
                sourceFile.getPath());

        assertNotNull(context.writers.get("20120607-CHL-1D-op-metadata.xml"));
        assertNotNull(context.writers.get("20120607-CHL-1D-op-metadata.html"));


        assertEquals("<metadata>\n" +
                        "\n" +
                        "    <source>\n" +
                        "        <name>MERIS</name>\n" +
                        "        <width>10</width>\n" +
                        "        <height>10</height>\n" +
                        "    </source>\n" +
                        "\n" +
                        "    <sources>\n" +
                        "        <sourceProduct>MERIS</sourceProduct>\n" +
                        "        <sourceProduct.1>MERIS</sourceProduct.1>\n" +
                        "        <sourceProduct1>MERIS</sourceProduct1>\n" +
                        "    </sources>\n" +
                        "\n" +
                        "    <target>\n" +
                        "        <name>projected_MERIS</name>\n" +
                        "        <width>251</width>\n" +
                        "        <height>501</height>\n" +
                        "    </target>\n" +
                        "\n" +
                        "    <parameterMetadata>\n" +
                        "        <parameters>\n" +
                        "            <orientation>0.0</orientation>\n" +
                        "            <pixelSizeX>0.04</pixelSizeX>\n" +
                        "            <crs>EPSG:4326</crs>\n" +
                        "            <pixelSizeY>0.02</pixelSizeY>\n" +
                        "            <resamplingName>Nearest</resamplingName>\n" +
                        "            <includeTiePointGrids>true</includeTiePointGrids>\n" +
                        "        </parameters>\n" +
                        "        <parameterFile>params.xml</parameterFile>\n" +
                        "        <parameterXml><![CDATA[<parameters>\n" +
                        "    <pixelSizeX>0.04</pixelSizeX>\n" +
                        "    <pixelSizeY>0.02</pixelSizeY>\n" +
                        "    <crs>EPSG:4326</crs>\n" +
                        "</parameters>]]></parameterXml>\n" +
                        "    </parameterMetadata>\n" +
                        "\n" +
                        "    <operatorMetadata>\n" +
                        "        <operatorName>Reproject</operatorName>\n" +
                        "        <operatorVersion>1.0</operatorVersion>\n" +
                        "        <operatorClass>class org.esa.snap.core.gpf.common.reproject.ReprojectionOp</operatorClass>\n" +
                        "    </operatorMetadata>\n" +
                        "\n" +
                        "    <extraMetadata>\n" +
                        "        <processingCenter>BC</processingCenter>\n" +
                        "        <softwareName>BEAM</softwareName>\n" +
                        "    </extraMetadata>\n" +
                        "</metadata>",
                context.writers.get("20120607-CHL-1D-op-metadata.xml").toString());


        assertEquals("<html>\n" +
                        "<body>\n" +
                        "Size of MERIS: 10 x 10 pixels<br/>\n" +
                        "Size of projected_MERIS: 251 x 501 pixels<br/>\n" +
                        "Extra data:<br/>\n" +
                        "processingCenter = BC<br/>\n" +
                        "softwareName = BEAM<br/>\n" +
                        "</body>\n" +
                        "</html>",
                context.writers.get("20120607-CHL-1D-op-metadata.html").toString());
    }

    @Test
    public void testTemplateMergingWithGraphXml() throws Exception {

        String metadataPath = "test.properties";
        String parameterPath = "params.txt";
        File templateDir = getTemplateDir();

        TestProductIOPlugIn.INSTANCE.getSourceProducts().put(sourceFile, createSourceProduct());

        final String graphXml = "" +
                "<graph id=\"testGraph\">\n" +
                "    <version>1.0</version>\n" +
                "    <node id=\"testNode\">\n" +
                "        <operator>Reproject</operator>\n" +
                "        <sources>\n" +
                "            <source>${src}</source>\n" +
                "        </sources>\n" +
                "        <parameters>\n" +
                "            <pixelSizeX>${pixelSizeX}</pixelSizeX>\n" +
                "            <pixelSizeY>${pixelSizeY}</pixelSizeY>\n" +
                "            <crs>EPSG:4326</crs>\n" +
                "        </parameters>\n" +
                "    </node>\n" +
                "</graph>\n";
        context.textFiles.put("graph.xml", graphXml);

        context.textFiles.put("test-metadata.xml",
                "<metadata>\n" +
                "    <product>\n" +
                "        <name>test-product</name>\n" +
                "    </product>\n" +
                "</metadata>");

        context.textFiles.put(metadataPath, "" +
                "processingCenter = BC\n" +
                "softwareName = BEAM\n");
        context.textFiles.put(parameterPath, "" +
                "pixelSizeX = 0.04\n" +
                "pixelSizeY = 0.02\n");

        tool.run("graph.xml",
                "-t", targetFile.getPath(),
                "-p", parameterPath,
                "-m", metadataPath,
                "-v", templateDir.getPath(),
                "-Ssrc=" + sourceFile);

        final Map<Object, Product> targetProducts = TestProductIOPlugIn.INSTANCE.getTargetProducts();
        assertTrue(targetProducts.containsKey(targetFile));


        assertNotNull(context.writers.get("20120607-CHL-1D-graph-metadata.xml"));
        assertNotNull(context.writers.get("20120607-CHL-1D-graph-metadata.html"));

        assertEquals("<metadata>\n" +
                        "\n" +
                        "    <source>\n" +
                        "        <name>MERIS</name>\n" +
                        "        <width>10</width>\n" +
                        "        <height>10</height>\n" +
                        "    </source>\n" +
                        "\n" +
                        "    <sources>\n" +
                        "        <product>\n" +
                        "            <src>MERIS</src>\n" +
                        "        </product>\n" +
                        "        <metadata>\n" +
                        "                <metadata>\n" +
                        "    <product>\n" +
                        "        <name>test-product</name>\n" +
                        "    </product>\n" +
                        "</metadata>\n" +
                        "        </metadata>\n" +
                        "    </sources>\n" +
                        "\n" +
                        "    <target>\n" +
                        "        <name>projected_MERIS</name>\n" +
                        "        <width>251</width>\n" +
                        "        <height>501</height>\n" +
                        "    </target>\n" +
                        "\n" +
                        "    <parameterMetadata>\n" +
                        "        <parameters>\n" +
                        "            <pixelSizeX>0.04</pixelSizeX>\n" +
                        "            <pixelSizeY>0.02</pixelSizeY>\n" +
                        "            <sourceProducts></sourceProducts>\n" +
                        "            <src>ReadOp@src</src>\n" +
                        "        </parameters>\n" +
                        "        <parameterFile>params.txt</parameterFile>\n" +
                        "        <parameterFileContent><![CDATA[pixelSizeX = 0.04\n" +
                        "pixelSizeY = 0.02]]></parameterFileContent>\n" +
                        "    </parameterMetadata>\n" +
                        "\n" +
                        "    <graphMetadata>\n" +
                        "        <graphFile>graph.xml</graphFile>\n" +
                        "        <graphXml><![CDATA[<graph id=\"testGraph\">\n" +
                        "    <version>1.0</version>\n" +
                        "    <node id=\"testNode\">\n" +
                        "        <operator>Reproject</operator>\n" +
                        "        <sources>\n" +
                        "            <source>${src}</source>\n" +
                        "        </sources>\n" +
                        "        <parameters>\n" +
                        "            <pixelSizeX>${pixelSizeX}</pixelSizeX>\n" +
                        "            <pixelSizeY>${pixelSizeY}</pixelSizeY>\n" +
                        "            <crs>EPSG:4326</crs>\n" +
                        "        </parameters>\n" +
                        "    </node>\n" +
                        "</graph>]]></graphXml>\n" +
                        "        <graphNodeIds>\n" +
                        "            <node>testNode</node>\n" +
                        "            <node>ReadOp@src</node>\n" +
                        "            <node>WriteOp@testNode</node>\n" +
                        "        </graphNodeIds>\n" +
                        "    </graphMetadata>\n" +
                        "\n" +
                        "    <extraMetadata>\n" +
                        "        <processingCenter>BC</processingCenter>\n" +
                        "        <softwareName>BEAM</softwareName>\n" +
                        "    </extraMetadata>\n" +
                        "</metadata>",
                context.writers.get("20120607-CHL-1D-graph-metadata.xml").toString());

        assertEquals("<html>\n" +
                        "<body>\n" +
                        "Size of MERIS: 10 x 10 pixels<br/>\n" +
                        "Size of projected_MERIS: 251 x 501 pixels<br/>\n" +
                        "Extra data:<br/>\n" +
                        "processingCenter = BC<br/>\n" +
                        "softwareName = BEAM<br/>\n" +
                        "</body>\n" +
                        "</html>",
                context.writers.get("20120607-CHL-1D-graph-metadata.html").toString());
    }


    @Test
    public void testSourceFileDidNotExist() throws Exception {

        String metadataPath = "test.properties";
        String parameterPath = "params.txt";
        File templateDir = getTemplateDir();

        String sourceFilePath = "c:\\dummy";
        TestProductIOPlugIn.INSTANCE.getSourceProducts().put(sourceFilePath, createSourceProduct());
        final String graphXml = "" +
                "<graph id=\"testGraph\">\n" +
                "    <version>1.0</version>\n" +
                "    <node id=\"testNode\">\n" +
                "        <operator>Reproject</operator>\n" +
                "        <sources>\n" +
                "            <source>${src}</source>\n" +
                "        </sources>\n" +
                "        <parameters>\n" +
                "            <pixelSizeX>${pixelSizeX}</pixelSizeX>\n" +
                "            <pixelSizeY>${pixelSizeY}</pixelSizeY>\n" +
                "            <crs>EPSG:4326</crs>\n" +
                "        </parameters>\n" +
                "    </node>\n" +
                "</graph>\n";
        context.textFiles.put("graph.xml", graphXml);

        context.textFiles.put("test-metadata.xml",
                "<metadata>\n" +
                        "    <product>\n" +
                        "        <name>test-product</name>\n" +
                        "    </product>\n" +
                        "</metadata>");

        context.textFiles.put(metadataPath, "" +
                "processingCenter = BC\n" +
                "softwareName = BEAM\n");
        context.textFiles.put(parameterPath, "" +
                "pixelSizeX = 0.04\n" +
                "pixelSizeY = 0.02\n");

        try {
            tool.run("graph.xml",
                    "-t", targetFile.getPath(),
                    "-p", parameterPath,
                    "-m", metadataPath,
                    "-v", templateDir.getPath(),
                    "-Ssrc=" + sourceFilePath);

            fail("'file' parameter must exist");
        } catch (Exception ignore) {

        }


    }

    private File getTemplateDir() throws URISyntaxException {
        URL templateUrl = getClass().getResource("graph-metadata.xml.vm");
        return new File(templateUrl.toURI()).getParentFile();
    }

    private Product createSourceProduct() throws FactoryException, TransformException {
        Product sourceProduct = new Product("MERIS", "MARCO", 10, 10);
        sourceProduct.addBand("x", "5.1");
        sourceProduct.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 0, 0, 1, 1));
        return sourceProduct;
    }

}
