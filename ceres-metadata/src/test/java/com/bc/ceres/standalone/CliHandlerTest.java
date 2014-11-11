package com.bc.ceres.standalone;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class CliHandlerTest {

    private CliHandler cliHandler;
    private String[] command;

    @Before
    public void setUp() throws Exception {
        command = new String[]{
                "-v", "template1=/my/template/dir/veltemp.vm.txt", "-v", "template2=/my/template/dir/veltemp.vm.xml",
                "-t", "/my/out/product.n1",
                "-S", "source1=foo/baa/prod.N1", "-S", "source2=foo/bah/MER.N1", "-S", "source3=foo/bar/ATS.N1",
                "-m", "meta1=/root/dir/global-metadata.txt", "-m", "meta2=/root/dir/lut.properties",
                "var1", "var2", "var3 var3a"};
        cliHandler = new CliHandler(command);
    }

    @Test
    public void testCreateOptions() throws Exception {
        Options options = cliHandler.createOptions();

        Option option = options.getOption("-v");
        assertNotNull(option);
        assertTrue(option.isRequired());
        assertNull(option.getValue());
        assertEquals("template>=<filePath", option.getArgName());
        assertEquals("The absolute path of the velocity templates (*.vm). Could be several given by key-value-pairs.", option.getDescription());

        option = options.getOption("-t");
        assertNotNull(option);
        assertTrue(option.isRequired());
        assertNull(option.getValue());
        assertEquals("filePath", option.getArgName());
        String desc = "The absolute item path (e.g. a product), the metadata file will be placed next to the item. It gets the name " +
                "'itemName-templateName.templateSuffix'. Refer to as $targetPath in velocity templates. If the targetPath is a " +
                "directory, the metadata file will get the name of the velocity template without the suffix *.vm";
        assertEquals(desc, option.getDescription());

        option = options.getOption("-S");
        assertNotNull(option);
        assertFalse("source item paths are optional", option.isRequired());
        assertNull(option.getValue());
        assertEquals("source>=<filePath", option.getArgName());
        desc = "Optional. The absolute path and name of the source items. Could be several given by key-value-pairs. In the velocity " +
                "templates the key will give you the content of the associated metadata file(s). The reference $sourcePaths holds a " +
                "map of the input item paths. The reference $sourceMetadata holds a map with all source-metadata, which can be referenced " +
                "by their key. ($sourceMetadata.get(\"source\").get(\"metadata_xml\").content";
        assertEquals(desc, option.getDescription());

        option = options.getOption("-m");
        assertNotNull(option);
        assertFalse("static metadata are optional", option.isRequired());
        assertNull(option.getValue());
        assertEquals("myKey>=<filePath", option.getArgName());
        desc = "Optional. The absolute path and name of text file(s) (e.g. global metadata, LUTs) to be included as ceres-metadata - Resource. " +
                "Refer to as $myKey in velocity templates. ($myKey.content; $myKey.map.get(\"key\"), if it was a *.properties file " +
                "or $myKey.path)";
        assertEquals(desc, option.getDescription());
    }

    @Test
    public void testParseTemplateFiles() throws Exception {
        HashMap<String, String> templateFiles = cliHandler.fetchTemplateFiles();
        assertEquals(2, templateFiles.size());
        assertEquals("/my/template/dir/veltemp.vm.txt", templateFiles.get("template1"));
        assertEquals("/my/template/dir/veltemp.vm.xml", templateFiles.get("template2"));
    }

    @Test
    public void testParseTemplateFilesWithIllegalArgumentFormat() throws Exception {
        command = new String[]{"-v", "/a/path/temp1.vm.txt", "-t", "bla/"};
        cliHandler = new CliHandler(command);
        try {
            cliHandler.fetchTemplateFiles();
            fail("Do not reach this statement.");
        } catch (ParseException e) {
            fail("Not this exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Pattern for values of the option -v is: key=value", expected.getMessage());
        }
    }

    @Test
    public void testParseSourceItemFiles() throws Exception {
        HashMap<String, String> sourceItemFiles = cliHandler.fetchSourceItemFiles();
        assertEquals(3, sourceItemFiles.size());
        assertEquals("foo/baa/prod.N1", sourceItemFiles.get("source1"));
        assertEquals("foo/bah/MER.N1", sourceItemFiles.get("source2"));
        assertEquals("foo/bar/ATS.N1", sourceItemFiles.get("source3"));
    }

    @Test
    public void testFetchSourceItemFiles() throws Exception {
        command = new String[]{
                "-v", "template1=/my/template/dir/veltemp.vm.txt",
                "-t", "/my/out/product.n1"};
        cliHandler = new CliHandler(command);

        HashMap<String, String> sourceItemFiles = cliHandler.fetchSourceItemFiles();
        assertNotNull(sourceItemFiles);
        assertEquals(0, sourceItemFiles.size());
    }

    @Test
    public void testParseTargetItemFile() throws Exception {
        String targetItemFile = cliHandler.fetchTargetItemFile();
        assertEquals("/my/out/product.n1", targetItemFile);
    }

    @Test
    public void testParseStaticMetadataTextFiles() throws Exception {
        HashMap<String, String> metadataFile = cliHandler.fetchGlobalMetadataFiles();
        assertEquals(2, metadataFile.size());
        assertEquals("/root/dir/global-metadata.txt", metadataFile.get("meta1"));
        assertEquals("/root/dir/lut.properties", metadataFile.get("meta2"));
    }

    @Test
    public void testParseArguments() throws Exception {
        String[] arguments = cliHandler.fetchArguments();
        assertEquals(3, arguments.length);
        assertEquals("var1", arguments[0]);
        assertEquals("var2", arguments[1]);
        assertEquals("var3 var3a", arguments[2]);
    }

    @Test
    public void testPrintUsage() throws Exception {
//        cliHandler.printUsage();
    }
}
