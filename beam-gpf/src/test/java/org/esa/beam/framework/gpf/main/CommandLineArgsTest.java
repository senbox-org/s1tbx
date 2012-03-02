/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.gpf.main;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.util.SortedMap;

public class CommandLineArgsTest extends TestCase {

    private static final int K = 1024;
    private static final int M = 1024 * 1024;

    public void testArgsCloned() throws Exception {
        String[] args = new String[]{"Reproject", "ProjTarget.dim", "UnProjSource.dim"};
        CommandLineArgs lineArgs = new CommandLineArgs(args);
        assertNotNull(lineArgs.getArgs());
        assertTrue(args != lineArgs.getArgs());
        assertEquals(3, lineArgs.getArgs().length);
        assertEquals("Reproject", lineArgs.getArgs()[0]);
    }

    public void testNoArgsRequestsHelp() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{});
        lineArgs.parseArguments();
        assertEquals(true, lineArgs.isHelpRequested());
    }

    public void testHelpOption() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"Reproject", "-h"});
        lineArgs.parseArguments();
        assertEquals(true, lineArgs.isHelpRequested());
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilePath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpOnly() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"Reproject"});
        lineArgs.parseArguments();
        assertEquals(false, lineArgs.isHelpRequested());
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilePath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    public void testOpWithSource() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"Reproject", "source.dim"});
        lineArgs.parseArguments();
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilePath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals(3, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
    }

    public void testOpWithTargetAndSource() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"Reproject", "-t", "output.dim", "source.dim"});
        lineArgs.parseArguments();
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertEquals(null, lineArgs.getGraphFilePath());
        assertEquals("output.dim", lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals(3, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
    }

    public void testMinimumGraph() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"./map-proj.xml", "source.dim"});
        lineArgs.parseArguments();
        assertEquals(null, lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilePath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals("source.dim", map.get("sourceProduct"));
        assertEquals("source.dim", map.get("sourceProduct.1"));
        assertEquals("source.dim", map.get("sourceProduct1")); // test for backward compatibility
    }

    public void testGraphOnly() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"./map-proj.xml"});
        lineArgs.parseArguments();
        lineArgs.parseArguments();
        assertEquals(null, lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilePath());
        assertEquals(CommandLineTool.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
    }

    public void testFormatDetection() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{"Reproject", "-t", "target.dim", "source.dim"});
        lineArgs.parseArguments();
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"Reproject", "source.dim"});
        lineArgs.parseArguments();
        assertEquals(CommandLineTool.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
    }

    public void testFormatOption() throws Exception {
        CommandLineArgs lineArgs;
        lineArgs = new CommandLineArgs(new String[]{"Reproject", "-t", "target.h5", "-f", "HDF-5", "source.dim"});
        lineArgs.parseArguments();
        assertEquals("HDF-5", lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"Reproject", "-f", "GeoTIFF", "-t", "target.tif", "source.dim"});
        lineArgs.parseArguments();
        assertEquals("GeoTIFF", lineArgs.getTargetFormatName());
        lineArgs = new CommandLineArgs(new String[]{"Reproject", "-f", "BEAM-DIMAP", "-t", "target.dim", "source.dim"});
        lineArgs.parseArguments();
        assertEquals("BEAM-DIMAP", lineArgs.getTargetFormatName());
    }


    public void testParameterOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "-PpixelSizeX=0.02",
                "-PpixelSizeY=0.03",
                "-PpixelOffsetX=0.5",
                "-PpixelOffsetY=1.0",
                "source.dim",
        });
        lineArgs.parseArguments();
        SortedMap<String, String> parameterMap = lineArgs.getParameterMap();
        assertEquals("0.02", parameterMap.get("pixelSizeX"));
        assertEquals("0.03", parameterMap.get("pixelSizeY"));
        assertEquals("0.5", parameterMap.get("pixelOffsetX"));
        assertEquals("1.0", parameterMap.get("pixelOffsetY"));
    }

    public void testParameterFileOption() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
                "-p",
                "param.properties",
        });
        lineArgs.parseArguments();
        assertEquals("param.properties", lineArgs.getParametersFilePath());
    }

    public void testClearCacheAfterRowWrite() throws Exception {
        CommandLineArgs lineArgs;

        // test default value
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "-x",
        });

        lineArgs.parseArguments();
        assertEquals(true, lineArgs.isClearCacheAfterRowWrite());
    }

    public void testJAIOptions() throws Exception {
        CommandLineArgs lineArgs;

        // test default value
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
        });
        lineArgs.parseArguments();
        assertEquals(512 * M, lineArgs.getTileCacheCapacity());
        assertEquals(CommandLineTool.DEFAULT_TILE_SCHEDULER_PARALLELISM, lineArgs.getTileSchedulerParallelism());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
                "-c",
                "16M",
        });
        lineArgs.parseArguments();
        assertEquals(16 * M, lineArgs.getTileCacheCapacity());
        assertEquals(CommandLineTool.DEFAULT_TILE_SCHEDULER_PARALLELISM, lineArgs.getTileSchedulerParallelism());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
                "-q",
                "1",
                "-c",
                "16000K",
        });
        lineArgs.parseArguments();
        assertEquals(16000 * K, lineArgs.getTileCacheCapacity());
        assertEquals(1, lineArgs.getTileSchedulerParallelism());

        // test some valid value
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
                "-c",
                "16000002",
                "-q",
                "3"
        });
        lineArgs.parseArguments();
        assertEquals(16000002, lineArgs.getTileCacheCapacity());
        assertEquals(3, lineArgs.getTileSchedulerParallelism());

        // test zero
        lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "source.dim",
                "-c",
                "0",
                "-q",
                "10"
        });
        lineArgs.parseArguments();
        assertEquals(0, lineArgs.getTileCacheCapacity());
        assertEquals(10, lineArgs.getTileSchedulerParallelism());

        // test zero or less
        try {
            lineArgs = new CommandLineArgs(new String[]{
                    "Reproject",
                    "source.dim",
                    "-c",
                    "-6",
            });
            lineArgs.parseArguments();
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("negative"));
        }
    }

    public void testTargetOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "./map-proj.xml",
                "-Tndvi=./out/ndviProduct.dim",
                "-Tsnow=./out/snowMask.dim",
                "source.dim",
        });
        lineArgs.parseArguments();
        SortedMap<String, String> targetMap = lineArgs.getTargetFilePathMap();
        assertNotNull(targetMap);
        assertEquals(2, targetMap.size());
        assertEquals("./out/ndviProduct.dim", targetMap.get("ndvi"));
        assertEquals("./out/snowMask.dim", targetMap.get("snow"));
        assertEquals(null, lineArgs.getTargetFilePath());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertEquals(3, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
    }

    public void testSourceOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "-SndviProduct=./inp/NDVI.dim",
                "-ScloudProduct=./inp/cloud-mask.dim",
                "-Pthreshold=5.0",
                "source.dim",
        });
        lineArgs.parseArguments();
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndviProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("cloudProduct"));
    }

    public void testMultiSourceOptions() throws Exception {
        CommandLineArgs lineArgs = new CommandLineArgs(new String[]{
                "Reproject",
                "-Sndvi=./inp/NDVI.dim",
                "./inp/cloud-mask.dim",
                "source.dim",
                "input.dim",
        });
        lineArgs.parseArguments();
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
        assertEquals("source.dim", sourceMap.get("sourceProduct.2"));
        assertEquals("source.dim", sourceMap.get("sourceProduct2")); // test for backward compatibility
        assertEquals("input.dim", sourceMap.get("sourceProduct.3"));
        assertEquals("input.dim", sourceMap.get("sourceProduct3")); // test for backward compatibility
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndvi"));
    }

    public void testUsageText() throws Exception {
        String usageText = CommandLineUsage.getUsageText();
        assertNotNull(usageText);
        assertTrue(usageText.length() > 10);
    }

    public void testUsageTextForOperator() throws Exception {
        final String opName = "TestOpName";
        final String opDesc = "Creates a thing";
        final String srcProdAlias = "wasweissich";
        final String paramDefaultValue = "24.5";
        final String paramUnit = "Zwetschken";
        final String paramDesc = "Wert Beschreibung";
        @OperatorMetadata(alias = opName, description = opDesc)
        class TestOp extends Operator {

            @Parameter(defaultValue = paramDefaultValue, unit = paramUnit, description = paramDesc)
            double value;

            @SourceProduct(alias = srcProdAlias)
            Object sourceProduct;

            @TargetProduct
            Object targetProduct;

            @Override
            public void initialize() throws OperatorException {
            }
        }
        class TestSpi extends OperatorSpi {

            public TestSpi() {
                super(TestOp.class);
            }
        }

        final TestSpi testSpi = new TestSpi();

        final GPF gpf = GPF.getDefaultInstance();
        final OperatorSpiRegistry spiRegistry = gpf.getOperatorSpiRegistry();
        spiRegistry.addOperatorSpi(testSpi);
        assertSame(testSpi, spiRegistry.getOperatorSpi(opName));
        String usageText = CommandLineUsage.getUsageTextForOperator(opName);
        assertNotNull(usageText);
        assertTrue(usageText.contains(opName));
        assertTrue(usageText.contains(opDesc));
        assertTrue(usageText.contains(srcProdAlias));
        assertTrue(usageText.contains(paramDefaultValue));
        assertTrue(usageText.contains(paramDesc));
        assertTrue(usageText.contains(paramUnit));
    }

    public void testFailures() {
        testFailure(new String[]{"Reproject", "-p"}, "Option argument missing");
        testFailure(new String[]{"Reproject", "-f"}, "Option argument missing");
        testFailure(new String[]{"Reproject", "-t", "out.tammomat"}, "Output format unknown");
        testFailure(new String[]{"Reproject", "-ö"}, "Unknown option '-ö'");
        testFailure(new String[]{"Reproject", "-P=9"}, "Empty identifier");
        testFailure(new String[]{"Reproject", "-Pobelix10"}, "Missing '='");
        testFailure(new String[]{"Reproject", "-Tsubset=subset.dim",}, "Only valid with a given graph XML");
    }

    private void testFailure(String[] args, String reason) {
        try {
            CommandLineArgs commandLineArgs = new CommandLineArgs(args);
            commandLineArgs.parseArguments();
            fail("Exception expected for reason: " + reason);
        } catch (Exception e) {
            // ok
        }
    }
}

