package com.bc.ceres.standalone;

import com.bc.ceres.metadata.MetadataEngine;
import com.bc.ceres.metadata.SimpleFileSystemMock;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

public class MetadataEngineMainTest {

    private MetadataEngineMain metadataEngineMain;

    @Before
    public void setUp() throws Exception {
        MetadataEngine metadataEngine = new MetadataEngine(new SimpleFileSystemMock());
        metadataEngineMain = new MetadataEngineMain(metadataEngine);
    }

    @Test
    public void testProcessMetadata() throws Exception {
        SimpleFileSystemMock simpleFileSystem = new SimpleFileSystemMock();
        metadataEngineMain = new MetadataEngineMain(new MetadataEngine(simpleFileSystem));

        String[] args = {"Hello", "world"};
        System.setProperty("metadata", "/my/metadata.properties");
        System.setProperty("template1", "/my-template.xml.vm");
        System.setProperty("template2", "/yours.txt.vm");
        System.setProperty("source1", "source/path/tsm-1.dim");
        System.setProperty("source2", "source/path/tsm-2.N1");
        System.setProperty("source3", "source/path/tsm-3.hdf");
        System.setProperty("output", "/my/chl-a.N1");

        String template = "$commandLineArgs.get(0) $commandLineArgs.get(1). " +
                "$metadata.getContent(). " +
                "Output item path: $system.getProperty(\"output\"). " +
                "The source metadata: " +
                "1) $source1.get(\"metadata.txt\").getContent() " +
                "2) $source2.get(\"blubber.xm\").getContent() " +
                "3) $source3.get(\"report.txt\").getContent() " +
                "4) $source3.get(\"report.xml\").getContent().";

        String template2 = "<metadata>\n" +
                "    <sources>\n" +
                "        #foreach ($sourcePath in $sourcePaths)\n" +
                "            <source>$sourcePath</source>\n" +
                "        #end\n" +
                "    </sources>\n" +
                "    <target>$system.getProperty(\"output\")</target>\n" +
                "    <additional>$commandLineArgs.get(0) $commandLineArgs.get(1)</additional>\n" +
                "</metadata>";

        simpleFileSystem.setReader("/my/metadata.properties", new StringReader("my.key=my value"));
        simpleFileSystem.setReader("/my-template.xml.vm", new StringReader(template));
        simpleFileSystem.setReader("/yours.txt.vm", new StringReader(template2));
        simpleFileSystem.setList("source/path",
                "tsm-1.dim", "tsm-1.data", "tsm-1-metadata.txt",
                "tsm-2.N1", "tsm-2-blubber.xm",
                "tsm-3.hdf", "tsm-3-report.txt", "tsm-3-report.xml");
        simpleFileSystem.setReader("source/path/tsm-1-metadata.txt", new StringReader("source 1 text"));
        simpleFileSystem.setReader("source/path/tsm-2-blubber.xm", new StringReader("source 2 text"));
        simpleFileSystem.setReader("source/path/tsm-3-report.txt", new StringReader("source 3-txt text"));
        simpleFileSystem.setReader("source/path/tsm-3-report.xml", new StringReader("source 3-xml text"));
        StringWriter metadataResult = new StringWriter();
        StringWriter metadataResultXml = new StringWriter();
        simpleFileSystem.setWriter("/my/chl-a-my-template.xml", metadataResult);
        simpleFileSystem.setWriter("/my/chl-a-yours.txt", metadataResultXml);

        //execution
        metadataEngineMain.processMetadata(args);

        assertFalse(metadataResult.toString().isEmpty());
        assertFalse(metadataResultXml.toString().isEmpty());

        assertEquals("Hello world. my.key=my value. Output item path: /my/chl-a.N1. " +
                "The source metadata: 1) source 1 text 2) source 2 text 3) source 3-txt text 4) source 3-xml text.", metadataResult.toString());
        assertEquals("<metadata>\n" +
                "    <sources>\n" +
                "                    <source>source/path/tsm-3.hdf</source>\n" +
                "                    <source>source/path/tsm-2.N1</source>\n" +
                "                    <source>source/path/tsm-1.dim</source>\n" +
                "            </sources>\n" +
                "    <target>/my/chl-a.N1</target>\n" +
                "    <additional>Hello world</additional>\n" +
                "</metadata>", metadataResultXml.toString());
    }

    @Test
    public void testFetchSourcePaths() throws Exception {
        metadataEngineMain = new MetadataEngineMain(new MetadataEngine(new SimpleFileSystemMock()));
        System.setProperty("any", "not retrieved");
        System.setProperty("any key", "also not retrieved");
        System.setProperty("source1", "bla/path");
        System.setProperty("source2", "blub/path");
        System.setProperty("source3", "bli/path");

        HashMap<String, String> sources = metadataEngineMain.fetchProperty(MetadataEngineMain.KEY_SOURCE);

        assertEquals(3, sources.size());
        assertEquals("bla/path", sources.get("source1"));
        assertEquals("blub/path", sources.get("source2"));
        assertEquals("bli/path", sources.get("source3"));
    }
}
