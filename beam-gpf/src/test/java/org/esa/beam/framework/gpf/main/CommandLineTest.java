package org.esa.beam.framework.gpf.main;

import junit.framework.TestCase;

import java.util.SortedMap;

public class CommandLineTest extends TestCase {

    public void testArgsCloned() throws Exception {
        String[] args = new String[]{"MapProj", "ProjTarget.dim", "UnProjSource.dim"};
        CommandLine line = new CommandLine(args);
        assertNotNull(line.getArgs());
        assertTrue(args != line.getArgs());
        assertEquals(3, line.getArgs().length);
        assertEquals("MapProj", line.getArgs()[0]);
    }

    public void testNoArgsRequestsHelp() throws Exception {
        CommandLine line = new CommandLine(new String[]{});
        assertEquals(true, line.isHelpRequested());
    }

    public void testHelpOption() throws Exception {
        CommandLine line = new CommandLine(new String[]{"MapProj", "-h"});
        assertEquals(true, line.isHelpRequested());
        assertEquals("MapProj", line.getOperatorName());
        assertEquals(null, line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> map = line.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpOnly() throws Exception {
        CommandLine line = new CommandLine(new String[]{"MapProj"});
        assertEquals(false, line.isHelpRequested());
        assertEquals("MapProj", line.getOperatorName());
        assertEquals(null, line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> map = line.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpWithSource() throws Exception {
        CommandLine line = new CommandLine(new String[]{"MapProj", "source.dim"});
        assertEquals("MapProj", line.getOperatorName());
        assertEquals(null, line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> sourceMap = line.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testOpWithTargetAndSource() throws Exception {
        CommandLine line = new CommandLine(new String[]{"MapProj", "-t", "output.dim", "source.dim"});
        assertEquals("MapProj", line.getOperatorName());
        assertEquals(null, line.getGraphFilepath());
        assertEquals("output.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> sourceMap = line.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testMinimumGraph() throws Exception {
        CommandLine line = new CommandLine(new String[]{"./map-proj.xml", "source.dim"});
        assertEquals(null, line.getOperatorName());
        assertEquals("./map-proj.xml", line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> map = line.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals("source.dim", map.get("sourceProduct"));
        assertEquals("source.dim", map.get("sourceProduct1"));
    }

    public void testGraphOnly() throws Exception {
        CommandLine line = new CommandLine(new String[]{"./map-proj.xml"});
        assertEquals(null, line.getOperatorName());
        assertEquals("./map-proj.xml", line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        SortedMap<String, String> map = line.getSourceFilepathMap();
        assertNotNull(map);
    }

    public void testFormatDetection() throws Exception {
        CommandLine line = new CommandLine(new String[]{"MapProj", "-t", "target.dim", "source.dim"});
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
        line = new CommandLine(new String[]{"MapProj", "source.dim"});
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
// todo - set calsspath so that Writers are registered
//        line = new CommandLine(new String[]{"MapProj", "target.h5", "source.dim"});
//        assertEquals("HDF-5", line.getOutputFormatName());
//        line = new CommandLine(new String[]{"MapProj", "target.tif", "source.dim"});
//        assertEquals("GeoTIFF", line.getOutputFormatName());
    }

    public void testFormatOption() throws Exception {
        CommandLine line;
        line = new CommandLine(new String[]{"MapProj", "-t", "target.h5", "-f", "HDF-5", "source.dim"});
        assertEquals("HDF-5", line.getTargetFormatName());
        line = new CommandLine(new String[]{"MapProj", "-f", "GeoTIFF", "-t", "target.tif", "source.dim"});
        assertEquals("GeoTIFF", line.getTargetFormatName());
        line = new CommandLine(new String[]{"MapProj", "-f", "BEAM-DIMAP", "-t", "target.dim", "source.dim"});
        assertEquals("BEAM-DIMAP", line.getTargetFormatName());
    }


    public void testParameterOptions() throws Exception {
        CommandLine line = new CommandLine(new String[]{
                "MapProj",
                "-PpixelSizeX=0.02",
                "-PpixelSizeY=0.03",
                "-PpixelOffsetX=0.5",
                "-PpixelOffsetY=1.0",
                "source.dim",
        });
        SortedMap<String, String> parameterMap = line.getParameterMap();
        assertEquals("0.02", parameterMap.get("pixelSizeX"));
        assertEquals("0.03", parameterMap.get("pixelSizeY"));
        assertEquals("0.5", parameterMap.get("pixelOffsetX"));
        assertEquals("1.0", parameterMap.get("pixelOffsetY"));
    }

    public void testParameterFileOption() throws Exception {
        CommandLine line = new CommandLine(new String[]{
                "MapProj",
                "source.dim",
                "-p",
                "param.properties",
        });
        assertEquals("param.properties", line.getParameterFilepath());
    }

    public void testTargetOptions() throws Exception {
        CommandLine line = new CommandLine(new String[]{
                "./map-proj.xml",
                "-Tndvi=./out/ndviProduct.dim",
                "-Tsnow=./out/snowMask.dim",
                "source.dim",
        });
        SortedMap<String, String> targetMap = line.getTargetFilepathMap();
        assertNotNull(targetMap);
        assertEquals(2, targetMap.size());
        assertEquals("./out/ndviProduct.dim", targetMap.get("ndvi"));
        assertEquals("./out/snowMask.dim", targetMap.get("snow"));
        assertEquals(null, line.getTargetFilepath());
        SortedMap<String, String> sourceMap = line.getSourceFilepathMap();
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testSourceOptions() throws Exception {
        CommandLine line = new CommandLine(new String[]{
                "MapProj",
                "-SndviProduct=./inp/NDVI.dim",
                "-ScloudProduct=./inp/cloud-mask.dim",
                "-Pthreshold=5.0",
                "source.dim",
        });
        SortedMap<String, String> sourceMap = line.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndviProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("cloudProduct"));
    }

    public void testUsageText() throws Exception {
        String usageText = CommandLine.getUsageText();
        assertNotNull(usageText);
        assertTrue(usageText.length() > 10);
    }

    public void testFailures() {
        testFailure(new String[]{"MapProj", "-p"}, "Option argument missing");
        testFailure(new String[]{"MapProj", "-f"}, "Option argument missing");
        testFailure(new String[]{"MapProj", "-t", "out.tammomat"}, "Output format unknown");
        testFailure(new String[]{"MapProj", "-ö"}, "Unknown option '-ö'");
        testFailure(new String[]{"MapProj", "-P=9"}, "Empty identifier");
        testFailure(new String[]{"MapProj", "-Pobelix10"}, "Missing '='");
        testFailure(new String[]{"MapProj", "-Tsubset=subset.dim",}, "Only valid with a given graph XML");
    }

    private void testFailure(String[] args, String reason) {
        try {
            new CommandLine(args);
            fail("Exception expected for reason: " + reason);
        } catch (Exception e) {
            // ok
        }
    }
}

