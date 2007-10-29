package org.esa.beam.framework.gpf.main;

import junit.framework.TestCase;

import java.util.SortedMap;

public class CommandLineArgsTest extends TestCase {
    private static final int K = 1024;
    private static final int M = 1024 * 1024;

    public void testArgsCloned() throws Exception {
        String[] args = new String[]{"MapProj", "ProjTarget.dim", "UnProjSource.dim"};
        CommandLineArgs lineArgs = new CommandLineArgs(args);
        assertNotNull(lineArgs.getArgs());
        assertTrue(args != lineArgs.getArgs());
        assertEquals(3, lineArgs.getArgs().length);
        assertEquals("MapProj", lineArgs.getArgs()[0]);
    }

    public void testNoArgsRequestsHelp() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{});
        assertEquals(true, lineArgs.isHelpRequested());
    }

    public void testHelpOption() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"MapProj", "-h"});
        assertEquals(true, lineArgs.isHelpRequested());
        assertEquals("MapProj", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilepath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpOnly() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"MapProj"});
        assertEquals(false, lineArgs.isHelpRequested());
        assertEquals("MapProj", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilepath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpWithSource() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"MapProj", "source.dim"});
        assertEquals("MapProj", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilepath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testOpWithTargetAndSource() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"MapProj", "-t", "output.dim", "source.dim"});
        assertEquals("MapProj", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilepath());
        assertEquals("output.dim", lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testMinimumGraph() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"./map-proj.xml", "source.dim"});
        assertEquals(null, lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilepath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilepathMap();
        assertNotNull(map);
        assertEquals("source.dim", map.get("sourceProduct"));
        assertEquals("source.dim", map.get("sourceProduct1"));
    }

    public void testGraphOnly() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"./map-proj.xml"});
        assertEquals(null, lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilepath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilepath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilepathMap();
        assertNotNull(map);
    }

    public void testFormatDetection() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"MapProj", "-t", "target.dim", "source.dim"});
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"MapProj", "source.dim"});
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
    }

    public void testFormatOption() throws Exception {
        CommandLineArgs lineArgs;
        lineArgs = new CommandLineArgs(new String[]{"MapProj", "-t", "target.h5", "-f", "HDF-5", "source.dim"});
        assertEquals("HDF-5", lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"MapProj", "-f", "GeoTIFF", "-t", "target.tif", "source.dim"});
        assertEquals("GeoTIFF", lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"MapProj", "-f", "BEAM-DIMAP", "-t", "target.dim", "source.dim"});
        assertEquals("BEAM-DIMAP", lineArgs.getTargetFormatName());
    }


    public void testParameterOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "-PpixelSizeX=0.02",
                "-PpixelSizeY=0.03",
                "-PpixelOffsetX=0.5",
                "-PpixelOffsetY=1.0",
                "source.dim",
        });
        SortedMap<String, String> parameterMap = lineArgs.getParameterMap();
        assertEquals("0.02", parameterMap.get("pixelSizeX"));
        assertEquals("0.03", parameterMap.get("pixelSizeY"));
        assertEquals("0.5", parameterMap.get("pixelOffsetX"));
        assertEquals("1.0", parameterMap.get("pixelOffsetY"));
    }

    public void testParameterFileOption() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
                "-p",
                "param.properties",
        });
        assertEquals("param.properties", lineArgs.getParameterFilepath());
    }

    public void testTileCacheOption() throws Exception {
        CommandLineArgs lineArgs;
        final long maxMem = Runtime.getRuntime().maxMemory();

        // test default value
        lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
        });
        assertEquals(Math.max(512 * M, maxMem / (2 * M)), lineArgs.getTileCacheCapacity());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
                "-c",
                "16M",
        });
        assertEquals(16 * M, lineArgs.getTileCacheCapacity());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
                "-c",
                "16000K",
        });
        assertEquals(16000 * K, lineArgs.getTileCacheCapacity());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
                "-c",
                "16000000",
        });
        assertEquals(16000000, lineArgs.getTileCacheCapacity());

        // test 100% is within range
        lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "source.dim",
                "-c",
                (maxMem / M) + "",
        });
        assertEquals(maxMem / M, lineArgs.getTileCacheCapacity());

        // test min value
        try {
            new CommandLineArgs(new String[]{
                    "MapProj",
                    "source.dim",
                    "-c",
                    "0",
            });
            fail("Exception expected (interval?)");
        } catch (Exception e) {
        }

        // test max value exceeded
        try {
            new CommandLineArgs(new String[]{
                    "MapProj",
                    "source.dim",
                    "-c",
                    (maxMem / M + 1) + "M",
            });
            fail("Exception expected, (interval?)");
        } catch (Exception e) {
        }
    }

    public void testTargetOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "./map-proj.xml",
                "-Tndvi=./out/ndviProduct.dim",
                "-Tsnow=./out/snowMask.dim",
                "source.dim",
        });
        SortedMap<String, String> targetMap = lineArgs.getTargetFilepathMap();
        assertNotNull(targetMap);
        assertEquals(2, targetMap.size());
        assertEquals("./out/ndviProduct.dim", targetMap.get("ndvi"));
        assertEquals("./out/snowMask.dim", targetMap.get("snow"));
        assertEquals(null, lineArgs.getTargetFilepath());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilepathMap();
        assertEquals(2, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1"));
    }

    public void testSourceOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "MapProj",
                "-SndviProduct=./inp/NDVI.dim",
                "-ScloudProduct=./inp/cloud-mask.dim",
                "-Pthreshold=5.0",
                "source.dim",
        });
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilepathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndviProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("cloudProduct"));
    }

    public void testUsageText() throws Exception {
        String usageText = CommandLineUsage.getUsageText();
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
            new CommandLineArgs(args);
            fail("Exception expected for reason: " + reason);
        } catch (Exception e) {
            // ok
        }
    }
}

