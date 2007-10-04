package org.esa.beam.framework.gpf.main;

import junit.framework.TestCase;

public class CommandLineTest extends TestCase {
    public void testArgsCloned() {
        String[] args = new String[]{"MapProj", "ProjTarget.dim", "UnProjSource.dim"};
        CommandLine line = new CommandLine(args);
        assertNotNull(line.getArgs());
        assertTrue(args != line.getArgs());
        assertEquals(3, line.getArgs().length);
        assertEquals("MapProj", line.getArgs()[0]);
        //assertEquals("MapProj", line.getOperatorName());

    }

    public void testMinimumOp() {
        CommandLine line = new CommandLine(new String[]{"MapProj", "target.dim", "source.dim"});
        assertEquals("MapProj", line.getOperatorName());
        assertEquals(null, line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getOutputFormatName());
        assertEquals("source.dim", line.getSourceFilepath("sourceProduct1"));
        assertEquals("source.dim", line.getSourceFilepath("sourceProduct"));
        assertEquals("source.dim", line.getSourceFilepath(0));
    }

    public void testMinimumGraph() {
        CommandLine line = new CommandLine(new String[]{"./map-proj.xml", "target.dim", "source.dim"});
        assertEquals(null, line.getOperatorName());
        assertEquals("./map-proj.xml", line.getGraphFilepath());
        assertEquals("target.dim", line.getTargetFilepath());
        assertEquals("BEAM-DIMAP", line.getOutputFormatName());
        assertEquals("source.dim", line.getSourceFilepath("sourceProduct1"));
        assertEquals("source.dim", line.getSourceFilepath("sourceProduct"));
        assertEquals("source.dim", line.getSourceFilepath(0));
    }

    public void testFormatDetection() {
        CommandLine line = new CommandLine(new String[]{"MapProj", "target.dim", "source.dim"});
        assertEquals("BEAM-DIMAP", line.getOutputFormatName());
// todo - set calsspath so that Writers are registered
//        line = new CommandLine(new String[]{"MapProj", "target.h5", "source.dim"});
//        assertEquals("HDF-5", line.getOutputFormatName());
//        line = new CommandLine(new String[]{"MapProj", "target.tif", "source.dim"});
//        assertEquals("GeoTIFF", line.getOutputFormatName());
    }

    public void testFormatOption() {
        CommandLine line;
        line = new CommandLine(new String[]{"MapProj", "target.h5", "-f", "HDF-5", "source.dim"});
        assertEquals("HDF-5", line.getOutputFormatName());
        line = new CommandLine(new String[]{"MapProj", "-f", "GeoTIFF", "target.tif", "source.dim"});
        assertEquals("GeoTIFF", line.getOutputFormatName());
        line = new CommandLine(new String[]{"MapProj", "-f", "BEAM-DIMAP", "target.dim", "source.dim"});
        assertEquals("BEAM-DIMAP", line.getOutputFormatName());
    }


    public void testParameterOptions() {
        CommandLine line = new CommandLine(new String[]{
                "MapProj",
                "-PpixelSizeX=0.02",
                "-PpixelSizeY=0.03",
                "-PpixelOffsetX=0.5",
                "-PpixelOffsetY=1.0",
                "target.dim",
                "source.dim",
        });
        assertEquals("0.02", line.getParameterValue("pixelSizeX"));
        assertEquals("0.03", line.getParameterValue("pixelSizeY"));
        assertEquals("0.5", line.getParameterValue("pixelOffsetX"));
        assertEquals("1.0", line.getParameterValue("pixelOffsetY"));
    }

    public void testSourceOptions() {
        CommandLine line = new CommandLine(new String[]{
                "MapProj",
                "-SndviProduct=./data/NDVI.dim",
                "-ScloudProduct=./data/cloud-mask.dim",
                "-Pthreshold=5.0",
                "target.dim",
                "source.dim",
        });
        assertEquals("./data/NDVI.dim", line.getSourceFilepath("ndviProduct"));
        assertEquals("./data/cloud-mask.dim", line.getSourceFilepath("cloudProduct"));
    }
}

