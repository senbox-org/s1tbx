package org.esa.beam.meris.radiometry.calibration;

import org.esa.beam.dataio.envisat.DSD;
import org.esa.beam.dataio.envisat.Field;
import org.esa.beam.dataio.envisat.MerisRacProductFile;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.dataio.envisat.Record;
import org.esa.beam.dataio.envisat.RecordReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that the auxiliary data needed for the radiometric calibration adhere
 * to the specification and can be read.
 *
 * The auxiliary data format is specified in MERIS ESL Document PO-TN-MEL-GS-0003, pp. 6.6-1 - 6.6-7.
 *
 * @author Ralf Quast
 */
public class AuxiliaryDataTest {

    private static final int FR_ELEMENT_COUNT = 5 * 740;
    private static final int RR_ELEMENT_COUNT = 5 * 185;
    private static final String[] PRODUCT_FILE_NAMES = new String[]{
            // new files from ACRI
            "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000",
            "MER_RAC_AXVACR20091023_105043_20020429_041400_20021224_121445",
            // old files from ESA
            "MER_RAC_AXVIEC20050708_135224_20020321_193100_20021224_121445",
            "MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000",
            "MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000",
            "MER_RAC_AXVIEC20061009_084736_20061009_220000_20161009_220000",
    };

    private List<ProductFile> productFileList;

    @Test
    public void gainFR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.5
        assertDatasetAndFieldProperties("Gain_FR", 15, new String[]{"gain"}, new int[]{FR_ELEMENT_COUNT});
    }

    @Test
    public void gainRR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.6
        assertDatasetAndFieldProperties("Gain_RR", 15, new String[]{"gain"}, new int[]{RR_ELEMENT_COUNT});
    }

    @Test
    public void degradationFR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.12
        assertDatasetAndFieldProperties("Degradation_FR", 15,
                                        new String[]{"dsr_time", "beta", "gamma", "delta"},
                                        new int[]{1, FR_ELEMENT_COUNT, FR_ELEMENT_COUNT, FR_ELEMENT_COUNT});
    }

    @Test
    public void degradationRR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.13
        assertDatasetAndFieldProperties("Degradation_RR", 15,
                                        new String[]{"dsr_time", "beta", "gamma", "delta"},
                                        new int[]{1, RR_ELEMENT_COUNT, RR_ELEMENT_COUNT, RR_ELEMENT_COUNT});
    }

    private void assertDatasetAndFieldProperties(String datasetName,
                                                 int recordCount,
                                                 String[] fieldNames,
                                                 int[] fieldElementCounts) throws IOException {
        for (final ProductFile productFile : productFileList) {
            System.out.println("productFile = " + productFile.getFile().getName());

            final DSD dsd = productFile.getDSD(datasetName);
            assertNotNull(dsd);

            final RecordReader recordReader = productFile.getRecordReader(datasetName);
            assertNotNull(recordReader);
            assertEquals(recordCount, recordReader.getNumRecords());

            final Record record = recordReader.readRecord();
            assertNotNull(record);

            for (int i = 0; i < fieldNames.length; i++) {
                final Field field = record.getField(fieldNames[i]);
                assertNotNull(field);
                assertEquals(fieldElementCounts[i], field.getNumElems());
            }
        }
    }

    @Before
    public void initProductFiles() throws URISyntaxException, IOException {
        productFileList = new ArrayList<ProductFile>(6);
        for (final String productFileName : PRODUCT_FILE_NAMES) {
            final URL url = getClass().getResource(productFileName);
            final URI uri = url.toURI();
            final File file = new File(uri);
            final ProductFile productFile = new MerisRacProductFile(file);
            productFileList.add(productFile);
        }
    }

    @After
    public void closeProductFiles() throws IOException {
        for (final ProductFile productFile : productFileList) {
            productFile.close();
        }
    }
}
