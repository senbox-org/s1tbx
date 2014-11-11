package com.bc.ceres.standalone;

import com.bc.ceres.metadata.MetadataResourceEngine;
import com.bc.ceres.metadata.SimpleFileSystem;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class MetadataEngineMainTest {

    private MetadataEngineMain metadataEngineMain;

    @Test
    public void testProcessMetadata_usingXPath() throws Exception {
        SimpleFileSystem fileSystem = Mockito.mock(SimpleFileSystem.class);
        metadataEngineMain = new MetadataEngineMain(new MetadataResourceEngine(fileSystem));

        String[] args = {
                "-m", "staticKey=static-metadata.xml",
                "-v", "template1=/mine.xml.vm", "-v", "template2=/yours.txt.vm",
                "-t", "/root/foo"
        };
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

        Mockito.when(fileSystem.createReader("/mine.xml.vm")).thenReturn(new StringReader(velocityTemplate1));
        Mockito.when(fileSystem.createReader("/yours.txt.vm")).thenReturn(new StringReader(velocityTemplate2));
        Mockito.when(fileSystem.createReader("static-metadata.xml")).thenReturn(
                new StringReader(staticMetadata));

        StringWriter metadataResult1 = new StringWriter();
        Mockito.when(fileSystem.createWriter("/root/foo/mine.xml")).thenReturn(metadataResult1);

        StringWriter metadataResult2 = new StringWriter();
        Mockito.when(fileSystem.createWriter("/root/foo/yours.txt")).thenReturn(metadataResult2);

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
        SimpleFileSystem fileSystem = Mockito.mock(SimpleFileSystem.class);
        metadataEngineMain = new MetadataEngineMain(new MetadataResourceEngine(fileSystem));

        String[] args = {
                "-m",
                "dunkel=/my/metadata.properties",
                "-m",
                "hell=/my/lut.properties",
                "-v",
                "template1=/my-template.xml.vm",
                "-v",
                "template2=/yours.txt.vm",
                "-S",
                "source1=source/path/tsm-1.dim",
                "-S",
                "source2=source/path/tsm-2.N1",
                "-S",
                "source3=source/path/tsm-3.hdf",
                "-t",
                "/my/chl-a.N1",
                "Hello",
                "world"
        };
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

        Mockito.when(fileSystem.createReader("/my/metadata.properties")).thenReturn(
                new StringReader("my.key=my value"));
        Mockito.when(fileSystem.createReader("/my/lut.properties")).thenReturn(new StringReader("2643=WGS 84 / UTM"));
        Mockito.when(fileSystem.createReader("/my-template.xml.vm")).thenReturn(new StringReader(velocityTemplate));
        Mockito.when(fileSystem.createReader("/yours.txt.vm")).thenReturn(new StringReader(velocityTemplate2));
        Mockito.when(fileSystem.list("source/path")).thenReturn(new String[]{
                "tsm-1.dim", "tsm-1.data", "tsm-1-metadata.txt",
                "tsm-2.N1", "tsm-2-blubber.xm",
                "tsm-3.hdf", "tsm-3-report.txt", "tsm-3-report.xml"
        });

        Mockito.when(fileSystem.createReader("source/path/tsm-1-metadata.txt")).thenReturn(new StringReader("source 1 text"));
        Mockito.when(fileSystem.createReader("source/path/tsm-2-blubber.xm")).thenReturn(new StringReader("source 2 text"));
        Mockito.when(fileSystem.createReader("source/path/tsm-3-report.txt")).thenReturn(new StringReader("source 3-txt text"));
        Mockito.when(fileSystem.createReader("source/path/tsm-3-report.xml")).thenReturn(
                new StringReader("source 3-xml text"));
        StringWriter metadataResult = new StringWriter();
        StringWriter metadataResultXml = new StringWriter();
        Mockito.when(fileSystem.createWriter("/my/chl-a-my-template.xml")).thenReturn(metadataResult);
        Mockito.when(fileSystem.createWriter("/my/chl-a-yours.txt")).thenReturn(metadataResultXml);

        Mockito.when(fileSystem.isFile(Matchers.anyString())).thenReturn(true);

        //execution
        metadataEngineMain.processMetadata();

        assertFalse(metadataResult.toString().isEmpty());
        assertEquals("Hello world. my.key=my value. Output item path: /my/chl-a.N1. " +
                "The source metadata: 1) source 1 text 2) source 2 text 3) source 3-txt text 4) source 3-xml text. " +
                "A source path: source/path/tsm-1.dim." +
                "WGS 84 / UTM", metadataResult.toString());

        String metadataResultXmlString = metadataResultXml.toString();
        assertFalse(metadataResultXmlString.isEmpty());
        assertTrue(metadataResultXmlString.contains("<source>source/path/tsm-3.hdf</source>"));
        assertTrue(metadataResultXmlString.contains("<source>source/path/tsm-2.N1</source>"));
        assertTrue(metadataResultXmlString.contains("<source>source/path/tsm-1.dim</source>"));
        assertTrue(metadataResultXmlString.contains("<target>/my/chl-a.N1</target>"));
        assertTrue(metadataResultXmlString.contains("<additional>Hello world</additional>"));
        assertTrue(metadataResultXmlString.contains("<2643>WGS 84 / UTM</2643>"));
    }
}
