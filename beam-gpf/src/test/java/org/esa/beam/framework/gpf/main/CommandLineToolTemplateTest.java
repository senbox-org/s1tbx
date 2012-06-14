package org.esa.beam.framework.gpf.main;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Norman Fomferra
 */
public class CommandLineToolTemplateTest {

    private CommandLineTool tool;
    private TestCommandLineContext context;

    @Test
    public void testTemplateMergingWithOpName() throws Exception {
        String metadataPath = "test.properties";
        String sourcePath = new File("test.nc").getAbsolutePath();
        Product sourceProduct = new Product("source", "TEST", 10, 10);
        String targetPath = "20120607-CHL-1D.dim";

        URL templateUrl = getClass().getResource("op-metadata.xml.vm");
        File templateDir = new File(templateUrl.toURI()).getParentFile();
        String templatePath = templateDir.getPath();

        context.products.put(sourcePath, sourceProduct);
        context.textFiles.put(metadataPath, "processingCenter=BC\nsoftwareName=BEAM");

        tool.run("Reproject",
                 "-t", targetPath,
                 "-m", metadataPath,
                 "-v", templatePath,
                 sourcePath);

        assertNotNull(context.writers.get("./20120607-CHL-1D-op-metadata.xml"));
        assertNotNull(context.writers.get("./20120607-CHL-1D-op-metadata.html"));

        assertEquals("<metadata>\n" +
                             "    <source>\n" +
                             "        <name>source</name>\n" +
                             "        <width>10</width>\n" +
                             "        <height>10</height>\n" +
                             "    </source>\n" +
                             "    <target>\n" +
                             "        <name>target</name>\n" +
                             "        <width>10</width>\n" +
                             "        <height>10</height>\n" +
                             "    </target>\n" +
                             "    <operator>Reproject</operator>\n" +
                             "    <extra>\n" +
                             "        <processingCenter>BC</processingCenter>\n" +
                             "        <softwareName>BEAM</softwareName>\n" +
                             "    </extra>\n" +
                             "</metadata>",
                     context.writers.get("./20120607-CHL-1D-op-metadata.xml").toString());

        assertEquals("<html>\n" +
                             "<body>\n" +
                             "Size of source: 10 x 10 pixels<br/>\n" +
                             "Size of target: 10 x 10 pixels<br/>\n" +
                             "Extra data:<br/>\n" +
                             "processingCenter = BC<br/>\n" +
                             "softwareName = BEAM<br/>\n" +
                             "</body>\n" +
                             "</html>",
                     context.writers.get("./20120607-CHL-1D-op-metadata.html").toString());
    }

    @Test
    public void testTemplateMergingWithGraphXml() throws Exception {
        String metadataPath = "test.properties";
        File sourceFile = new File("test.testdata").getAbsoluteFile();
        Product sourceProduct = new Product("MERIS", "MARCO", 10, 10);
        sourceProduct.addBand("x", "5.1");
        sourceProduct.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 0, 0, 1, 1));
        File targetFile = new File("20120607-CHL-1D.testdata");

        URL templateUrl = getClass().getResource("graph-metadata.xml.vm");
        File templateDir = new File(templateUrl.toURI()).getParentFile();
        String templatePath = templateDir.getPath();

        TestProductIOPlugIn.INSTANCE.getSourceProducts().put(sourceFile, sourceProduct);

        context.textFiles.put(metadataPath, "processingCenter=BC\nsoftwareName=BEAM");
        context.textFiles.put("graph.xml",
                              "" +
                                      "<graph id=\"testGraph\">\n" +
                                      "    <version>1.0</version>\n" +
                                      "    <node id=\"testNode\">\n" +
                                      "        <operator>Reproject</operator>\n" +
                                      "        <sources>\n" +
                                      "            <source>${f}</source>\n" +
                                      "        </sources>\n" +
                                      "        <parameters>\n" +
                                      "            <crs>" + DefaultGeographicCRS.WGS84.toString() + "</crs>\n" +
                                      "        </parameters>\n" +
                                      "    </node>\n" +
                                      "</graph>\n" +
                                      "\n");

        tool.run("graph.xml",
                 "-t", targetFile.getPath(),
                 "-m", metadataPath,
                 "-v", templatePath,
                 "-Sf=" + sourceFile);

        final Map<Object, Product> targetProducts = TestProductIOPlugIn.INSTANCE.getTargetProducts();
        assertTrue(targetProducts.containsKey(targetFile));


        assertNotNull(context.writers.get("./20120607-CHL-1D-graph-metadata.xml"));
        assertNotNull(context.writers.get("./20120607-CHL-1D-graph-metadata.html"));

        assertEquals("<metadata>\n" +
                             "    <source>\n" +
                             "        <name>MERIS</name>\n" +
                             "        <width>10</width>\n" +
                             "        <height>10</height>\n" +
                             "    </source>\n" +
                             "    <sources>\n" +
                             "        <f>MERIS</f>\n" +
                             "    </sources>\n" +
                             "    <target>\n" +
                             "        <name>projected_MERIS</name>\n" +
                             "        <width>11</width>\n" +
                             "        <height>11</height>\n" +
                             "    </target>\n" +
                             "    <graph>\n" +
                             "        <node>testNode</node>\n" +
                             "        <node>ReadProduct$f</node>\n" +
                             "        <node>WriteProduct$testNode</node>\n" +
                             "    </graph>\n" +
                             "    <extra>\n" +
                             "        <processingCenter>BC</processingCenter>\n" +
                             "        <softwareName>BEAM</softwareName>\n" +
                             "    </extra>\n" +
                             "</metadata>",
                     context.writers.get("./20120607-CHL-1D-graph-metadata.xml").toString());

        assertEquals("<html>\n" +
                             "<body>\n" +
                             "Size of MERIS: 10 x 10 pixels<br/>\n" +
                             "Size of projected_MERIS: 11 x 11 pixels<br/>\n" +
                             "Extra data:<br/>\n" +
                             "processingCenter = BC<br/>\n" +
                             "softwareName = BEAM<br/>\n" +
                             "</body>\n" +
                             "</html>",
                     context.writers.get("./20120607-CHL-1D-graph-metadata.html").toString());
    }

    @Before
    public void setUp() throws Exception {
        context = new TestCommandLineContext() {
//            @Override
//            public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
//                final GraphContext graphContext = new GraphContext(graph);
//                observer.graphProcessingStarted(graphContext);
//                observer.graphProcessingStopped(graphContext);
//            }
        };
        tool = new CommandLineTool(context);

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

}
