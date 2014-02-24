package org.esa.beam.dataio.hico;

import org.junit.Test;

import static org.junit.Assert.*;

public class HicoFilenameTest {

    @Test
    public void testCreate() throws Exception {
        HicoFilename hicoFilename = HicoFilename.create("iss.2011246.0903.143918.L1B.LakeSchaalsee.v03.7863.20110906143420.100m.hico.hdr");
        assertNotNull(hicoFilename);
        assertEquals("03-SEP-2011 14:39:18.000000", hicoFilename.getAcquisitionUTC().format());
        assertEquals("06-SEP-2011 14:34:20.000000", hicoFilename.getL0UTC().format());
        assertEquals("hico", hicoFilename.getFileType());
        assertEquals("L1B", hicoFilename.getProcessingLevel());
        assertEquals("v03", hicoFilename.getProcessingVersion());
        assertEquals("7863", hicoFilename.getSceneID());
        assertEquals("100m", hicoFilename.getSpatialResolution());
        assertEquals("LakeSchaalsee", hicoFilename.getTarget());
        assertEquals("iss.2011246.0903.143918.L1B.LakeSchaalsee.v03.7863.20110906143420.100m.", hicoFilename.getProductBase());
    }
}
