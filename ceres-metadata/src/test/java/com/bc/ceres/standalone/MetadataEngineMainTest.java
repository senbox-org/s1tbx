package com.bc.ceres.standalone;

import com.bc.ceres.metadata.MetadataResourceEngine;
import com.bc.ceres.metadata.SimpleFileSystemMock;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

public class MetadataEngineMainTest {

    private MetadataEngineMain metadataEngineMain;

    @Before
    public void setUp() throws Exception {
        MetadataResourceEngine metadataResourceEngine = new MetadataResourceEngine(new SimpleFileSystemMock());
        metadataEngineMain = new MetadataEngineMain(metadataResourceEngine);
    }

    @Test
    public void testProcessMetadata_usingXPath() throws Exception {
        SimpleFileSystemMock simpleFileSystemMock = new SimpleFileSystemMock();
        metadataEngineMain = new MetadataEngineMain(new MetadataResourceEngine(simpleFileSystemMock));

        String[] args = {"-m", "staticKey=static-metadata.xml",
                "-v", "template1=/mine.xml.vm", "-v", "template2=/yours.txt.vm",
                "-t", "/root/foo"};
        metadataEngineMain.setCliHandler(new CliHandler(args));

        String velocityTemplate1 = "" +
                "<EX_GeographicBoundingBox>\n" +
                "    <westBoundLongitude>$xpath.run(\"//west/@att\", $staticKey)</westBoundLongitude>\n" +
                "    <eastBoundLongitude>$xpath.run(\"//east\", $staticKey)</eastBoundLongitude>\n" +
                "    <southBoundLatitude>$xpath.run(\"//south\", $staticKey)</southBoundLatitude>\n" +
                "    <northBoundLatitude>$xpath.run(\"//north\", $staticKey)</northBoundLatitude>\n" +
                "</EX_GeographicBoundingBox>\n";

        String velocityTemplate2 = "$xpath.run(\"//south\", $staticKey)";

        String staticMetadata = "" +
                "<metadata>" +
                "    <north>4.5</north>" +
                "    <west att=\"bla\">1.0</west>\n" +
                "    <south>3.2</south>" +
                "    <some>other</some>" +
                "    <east>1.3</east>\n" +
                "</metadata>";

        simpleFileSystemMock.setReader("/mine.xml.vm", new StringReader(velocityTemplate1));
        simpleFileSystemMock.setReader("/yours.txt.vm", new StringReader(velocityTemplate2));
        simpleFileSystemMock.setReader("static-metadata.xml", new StringReader(staticMetadata));

        StringWriter metadataResult1 = new StringWriter();
        simpleFileSystemMock.setWriter("/root/foo/mine.xml", metadataResult1);
        StringWriter metadataResult2 = new StringWriter();
        simpleFileSystemMock.setWriter("/root/foo/yours.txt", metadataResult2);

        //execution
        metadataEngineMain.processMetadata();

        assertFalse(metadataResult1.toString().isEmpty());
        assertEquals("" +
                "<EX_GeographicBoundingBox>\n" +
                "    <westBoundLongitude>bla</westBoundLongitude>\n" +
                "    <eastBoundLongitude>1.3</eastBoundLongitude>\n" +
                "    <southBoundLatitude>3.2</southBoundLatitude>\n" +
                "    <northBoundLatitude>4.5</northBoundLatitude>\n" +
                "</EX_GeographicBoundingBox>", metadataResult1.toString());

        assertFalse(metadataResult2.toString().isEmpty());
        assertEquals("3.2", metadataResult2.toString());
    }

    @Test
    public void testProcessMetadata() throws Exception {
        SimpleFileSystemMock simpleFileSystem = new SimpleFileSystemMock();
        metadataEngineMain = new MetadataEngineMain(new MetadataResourceEngine(simpleFileSystem));

        String[] args = {"-m", "dunkel=/my/metadata.properties", "-m", "hell=/my/lut.properties",
                "-v", "template1=/my-template.xml.vm", "-v", "template2=/yours.txt.vm",
                "-S", "source1=source/path/tsm-1.dim", "-S", "source2=source/path/tsm-2.N1", "-S", "source3=source/path/tsm-3.hdf",
                "-t", "/my/chl-a.N1",
                "Hello", "world"};
        metadataEngineMain.setCliHandler(new CliHandler(args));

        String velocityTemplate = "" +
                "$commandLineArgs.get(0) $commandLineArgs.get(1). " +
                "$dunkel.getContent(). " +
                "Output item path: $targetPath. " +
                "The source metadata: " +
                "1) $sourceMetadata.get(\"source1\").get(\"metadata_txt\").content " +
                "2) $sourceMetadata.get(\"source2\").get(\"blubber_xm\").content " +
                "3) $sourceMetadata.get(\"source3\").get(\"report_txt\").content " +
                "4) $sourceMetadata.get(\"source3\").get(\"report_xml\").content. " +
                "A source path: $sourcePaths.get(\"source1\")." +
                "$hell.map.get(\"2643\")";

        String velocityTemplate2 = "" +
                "<metadata>\n" +
                "    <sources>\n" +
                "        #foreach ($sourcePath in $sourcePaths)\n" +
                "            <source>$sourcePath</source>\n" +
                "        #end\n" +
                "    </sources>\n" +
                "    <target>$targetPath</target>\n" +
                "    <additional>$commandLineArgs.get(0) $commandLineArgs.get(1)</additional>\n" +
                "    <2643>$hell.map.get(\"2643\")</2643>\n" +
                "</metadata>";

        simpleFileSystem.setReader("/my/metadata.properties", new StringReader("my.key=my value"));
        simpleFileSystem.setReader("/my/lut.properties", new StringReader("2643=WGS 84 / UTM"));
        simpleFileSystem.setReader("/my-template.xml.vm", new StringReader(velocityTemplate));
        simpleFileSystem.setReader("/yours.txt.vm", new StringReader(velocityTemplate2));
        simpleFileSystem.setDirectoryList("source/path",
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
        metadataEngineMain.processMetadata();

        assertFalse(metadataResult.toString().isEmpty());
        assertFalse(metadataResultXml.toString().isEmpty());

        assertEquals("Hello world. my.key=my value. Output item path: /my/chl-a.N1. " +
                "The source metadata: 1) source 1 text 2) source 2 text 3) source 3-txt text 4) source 3-xml text. " +
                "A source path: source/path/tsm-1.dim." +
                "WGS 84 / UTM", metadataResult.toString());

        assertEquals("" +
                "<metadata>\n" +
                "    <sources>\n" +
                "                    <source>source/path/tsm-3.hdf</source>\n" +
                "                    <source>source/path/tsm-2.N1</source>\n" +
                "                    <source>source/path/tsm-1.dim</source>\n" +
                "            </sources>\n" +
                "    <target>/my/chl-a.N1</target>\n" +
                "    <additional>Hello world</additional>\n" +
                "    <2643>WGS 84 / UTM</2643>\n" +
                "</metadata>", metadataResultXml.toString());
    }
}
