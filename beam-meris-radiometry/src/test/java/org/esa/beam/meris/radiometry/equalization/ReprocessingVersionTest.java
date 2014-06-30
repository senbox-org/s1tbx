package org.esa.beam.meris.radiometry.equalization;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import static org.esa.beam.meris.radiometry.equalization.ReprocessingVersion.*;
import static org.junit.Assert.*;

public class ReprocessingVersionTest {


    //        // RR
//        MER_RAC_AXVIEC20050606_071652_20021224_121445_20041213_220000    // before Repro2
//        MER_RAC_AXVIEC20050607_071652_20021224_121445_20041213_220000    // first Repro2
//        MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000    // last Repro2
//        MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000    // first Repro3
    @Test
    public void testParseReprocessingVersion_RR() {
        assertEquals(ReprocessingVersion.AUTO_DETECT,
                     detectReprocessingVersion("MER_RAC_AXVIEC20050606_071652_20021224_121445_20041213_220000", true));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVIEC20050607_071652_20021224_121445_20041213_220000",
                                                                                   true));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVIEC20070302", true));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000",
                                                                                   true));
        assertEquals(ReprocessingVersion.REPROCESSING_3, detectReprocessingVersion("MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000",
                                                                                   true));
        assertEquals(ReprocessingVersion.REPROCESSING_3, detectReprocessingVersion("MER_RAC_AXVACR20111008",
                                                                                   true));
    }

    //        // FR
//        MER_RAC_AXVIEC20050707_135806_20041213_220000_20141213_220000    // before Repro2
//        MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000    // first Repro2
//        MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000    // last Repro2
//        MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000    // first Repro3
    @Test
    public void testParseReprocessingVersion_FR() {
        assertEquals(ReprocessingVersion.AUTO_DETECT, detectReprocessingVersion("MER_RAC_AXVIEC20050707_135806_20041213_220000_20141213_220000",
                                                                                false));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000",
                                                                                   false));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVIEC20070302", false));
        assertEquals(ReprocessingVersion.REPROCESSING_2, detectReprocessingVersion("MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000",
                                                                                   false));
        assertEquals(ReprocessingVersion.REPROCESSING_3, detectReprocessingVersion("MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000",
                                                                                   false));
        assertEquals(ReprocessingVersion.REPROCESSING_3, detectReprocessingVersion("MER_RAC_AXVACR20111008",
                                                                                   false));
    }


    @Test
    public void testAutoDetectFromProduct() {
        Product product = new Product("dummy", "barz", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement dsd = new MetadataElement("DSD");
        MetadataElement dsd23 = new MetadataElement("DSD.23");
        dsd.addElement(dsd23);
        root.addElement(dsd);

        dsd23.setAttributeString("DATASET_NAME", "RADIOMETRIC_CALIBRATION_FILE");
        dsd23.setAttributeString("FILE_NAME", "MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000");
        assertEquals(ReprocessingVersion.REPROCESSING_2, autoDetect(product));

        dsd23.setAttributeString("DATASET_NAME", "Radiometric calibration");
        dsd23.setAttributeString("FILE_NAME", "MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000");

        assertEquals(ReprocessingVersion.REPROCESSING_3, autoDetect(product));
    }

}