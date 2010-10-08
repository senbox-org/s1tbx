package org.esa.beam.meris.radiometry.calibration;

import org.esa.beam.dataio.envisat.DSD;
import org.esa.beam.dataio.envisat.Field;
import org.esa.beam.dataio.envisat.MerisRacProductFile;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.dataio.envisat.Record;
import org.esa.beam.dataio.envisat.RecordReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests that the auxiliary data needed for the radiometric calibration adhere
 * to the specification and can be read.
 * <p/>
 * The auxiliary data format is specified in MERIS ESL Document PO-TN-MEL-GS-0003, pp. 6.6-1 - 6.6-7.
 * <p/>
 * NOTE: tests annotated with {@code @Ignore} were merely written for investigating
 * and understanding the contents and differences in auxiliary files wrt the fields
 * needed by the degradation component of the radiometric calibration.
 * <p/>
 * NOTE: the indexing of the 'gain', 'beta', 'gamma', and 'delta' fields in the
 * auxiliary data records is gain[0..5][0..739].  The first index is the camera
 * module, while the second index is the column in the camera scan line. The
 * order of indexes specified in Document PO-TN-MEL-GS-0003 for these fields is
 * different from the actual order in the auxiliary files.
 * <p/>
 * NOTE: each record of the 'gain', 'beta', 'gamma', and 'delta' fields correspond
 * to a different band.
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
    public void validGainFR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.5
        assertValidity("Gain_FR", 15, new String[]{"gain"}, new int[]{FR_ELEMENT_COUNT});
    }

    @Test
    public void validGainRR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.6
        assertValidity("Gain_RR", 15, new String[]{"gain"}, new int[]{RR_ELEMENT_COUNT});
    }

    @Test
    public void validDegradationFR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.12
        assertValidity("Degradation_FR", 15,
                       new String[]{"dsr_time", "beta", "gamma", "delta"},
                       new int[]{1, FR_ELEMENT_COUNT, FR_ELEMENT_COUNT, FR_ELEMENT_COUNT});
    }

    @Test
    public void validDegradationRR() throws IOException {
        // Document PO-TN-MEL-GS-0003, Sect. 6.3.13
        assertValidity("Degradation_RR", 15,
                       new String[]{"dsr_time", "beta", "gamma", "delta"},
                       new int[]{1, RR_ELEMENT_COUNT, RR_ELEMENT_COUNT, RR_ELEMENT_COUNT});
    }

    @Ignore
    @Test
    public void equalGains() throws IOException {
        for (int b = 0; b < 15; b++) {
            testEquality("Gain_FR", b, "gain");
        }
    }

    @Ignore
    @Test
    public void equalBetas() throws IOException {
        for (int b = 0; b < 15; b++) {
            testEquality("Degradation_FR", 0, "beta");
        }
    }

    @Ignore
    @Test
    public void equalGammas() throws IOException {
        for (int b = 0; b < 15; b++) {
            testEquality("Degradation_FR", 0, "gamma");
        }
    }

    @Ignore
    @Test
    public void equalDeltas() throws IOException {
        for (int b = 0; b < 15; b++) {
            testEquality("Degradation_FR", 0, "delta");
        }
    }

    @Ignore
    @Test
    public void printGains() throws IOException {
        printToFile("Gain_FR", 0, "gain", new File("gains.csv"));
    }

    @Ignore
    @Test
    public void printBetas() throws IOException {
        printToFile("Degradation_FR", 0, "beta", new File("betas.csv"));
    }

    @Ignore
    @Test
    public void printGammas() throws IOException {
        printToFile("Degradation_FR", 0, "gamma", new File("gammas.csv"));
    }

    @Ignore
    @Test
    public void printDeltas() throws IOException {
        printToFile("Degradation_FR", 0, "delta", new File("deltas.csv"));
    }

    private void assertValidity(String datasetName, int recordCountExpected,
                                String[] fieldNames, int[] fieldElementCountsExpected) throws IOException {
        for (final ProductFile productFile : productFileList) {
            System.out.println("productFile = " + productFile.getFile().getName());

            final DSD dsd = productFile.getDSD(datasetName);
            assertNotNull(dsd);

            final RecordReader recordReader = productFile.getRecordReader(datasetName);
            assertNotNull(recordReader);
            assertEquals(recordCountExpected, recordReader.getNumRecords());

            final Record record = recordReader.readRecord();
            assertNotNull(record);

            for (int i = 0; i < fieldNames.length; i++) {
                final Field field = record.getField(fieldNames[i]);
                assertNotNull(field);
                assertEquals(fieldElementCountsExpected[i], field.getNumElems());
                if (!field.isIntType()) {
                    for (int k = 0; k < fieldElementCountsExpected[i]; k++) {
                        final float value = field.getElemFloat(k);
                        assertFalse(
                                MessageFormat.format("Expected valid value, actual value of ''{0}[{1}]'' is NaN",
                                                     field.getName(), k), Float.isNaN(value));
                        assertFalse(
                                MessageFormat.format("Expected valid value, actual value of ''{0}[{1}]'' is infinite",
                                                     field.getName(), k), Float.isInfinite(value));
                        // range of all fields is (0, 1], confirmed by LB 
                        assertTrue(
                                MessageFormat.format("Expected value in (0, 1], actual value of ''{0}[{1}]'' is {2}",
                                                     field.getName(), k, value), value > 0.0f && value <= 1.0f);
                    }
                }
            }
        }
    }

    private void testEquality(String datasetName, int recordIndex, String fieldName) throws IOException {
        final List<float[]> arrayList = new ArrayList<float[]>(6);
        for (final ProductFile productFile : productFileList) {
            final RecordReader recordReader = productFile.getRecordReader(datasetName);
            final Record record = recordReader.readRecord(recordIndex);
            arrayList.add((float[]) record.getField(fieldName).getElems());
        }
        for (int i = 0; i < FR_ELEMENT_COUNT; i++) {
            // values in ACR files are the same
            assertEquals(arrayList.get(0)[i], arrayList.get(1)[i], 0.0f);
            // values in IEC files are the same
            assertEquals(arrayList.get(2)[i], arrayList.get(3)[i], 0.0f);
            assertEquals(arrayList.get(3)[i], arrayList.get(4)[i], 0.0f);
            assertEquals(arrayList.get(4)[i], arrayList.get(5)[i], 0.0f);
        }
    }

    private void printToFile(String datasetName, int recordIndex, String fieldName, File file) throws IOException {
        final List<float[]> arrayList = new ArrayList<float[]>(6);
        for (final ProductFile productFile : productFileList) {
            final RecordReader recordReader = productFile.getRecordReader(datasetName);
            final Record record = recordReader.readRecord(recordIndex);
            arrayList.add((float[]) record.getField(fieldName).getElems());
        }
        final PrintStream ps = new PrintStream(file);
        try {
            for (final ProductFile productFile : productFileList) {
                ps.print(productFile.getFile().getName() + ",");
            }
            ps.println();
            for (int i = 0; i < FR_ELEMENT_COUNT; i++) {
                for (float[] array : arrayList) {
                    ps.print(array[i] + ",");
                }
                ps.println();
            }
        } finally {
            ps.close();
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
