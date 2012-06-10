package org.esa.beam.framework.gpf.main;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphProcessingObserver;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class CommandLineToolTemplateTest {

    private CommandLineTool tool;
    private TestCommandLineContext context;

    @Test
    public void testTemplateMerging() throws Exception {
        String metadataPath = "test.properties";
        String sourcePath = new File("test.nc").getAbsolutePath();
        Product sourceProduct = new Product("source", "TEST", 10, 10);
        String targetPath = "20120607-CHL-1D.dim";

        URL templateUrl = getClass().getResource("metadata.xml.vm");
        File templateDir = new File(templateUrl.toURI()).getParentFile();
        String templatePath = templateDir.getPath();

        context.products.put(sourcePath, sourceProduct);
        context.textFiles.put(metadataPath, "p1=Hallo?\np2=Haha!");

        tool.run("Reproject",
                 "-t", targetPath,
                 "-m", metadataPath,
                 "-v", templatePath,
                 sourcePath);

        assertNotNull(context.writers.get("./20120607-CHL-1D-metadata.xml"));
        assertNotNull(context.writers.get("./20120607-CHL-1D-metadata.html"));

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
                             "        <p1>Hallo?</p1>\n" +
                             "        <p2>Haha!</p2>\n" +
                             "    </extra>\n" +
                             "</metadata>",
                     context.writers.get("./20120607-CHL-1D-metadata.xml").toString());

        assertEquals("<html>\n" +
                             "<body>\n" +
                             "Size of source: 10 x 10 pixels<br/>\n" +
                             "Size of target: 10 x 10 pixels<br/>\n" +
                             "Extra data:<br/>\n" +
                             "p1 = Hallo?<br/>\n" +
                             "p2 = Haha!<br/>\n" +
                             "</body>\n" +
                             "</html>",
                     context.writers.get("./20120607-CHL-1D-metadata.html").toString());
    }

    @Before
    public void setUp() throws Exception {
        context = new TestCommandLineContext() {

            @Override
            public Graph readGraph(String filePath, Map<String, String> templateVariables) throws GraphException, IOException {
                throw new IllegalStateException("readGraph() not implemented");
            }

            @Override
            public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
                throw new IllegalStateException("executeGraph() not implemented");
            }
        };
        tool = new CommandLineTool(context);
    }

}
