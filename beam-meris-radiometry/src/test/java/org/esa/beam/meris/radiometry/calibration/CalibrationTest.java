package org.esa.beam.meris.radiometry.calibration;

import org.esa.beam.dataio.envisat.MerisRacProductFile;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.dataio.envisat.Record;
import org.esa.beam.dataio.envisat.RecordReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class CalibrationTest {

    private Calibration calibration;

    @Test
    public void calibration() {
        final int bandIndex = 0;
        final int detectorIndex = 1;
        final double radiance = 1.0;
        assertTrue(radiance == calibration.calibrate(bandIndex, detectorIndex, radiance));
    }

    @Before
    public void initializeCalibration() throws URISyntaxException, IOException {
        calibration = new Calibration(Calibration.Resolution.FR,
                                      "MER_RAC_AXVIEC20050708_135224_20020321_193100_20021224_121445",
                                      "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000");
    }

    private static class Calibration {

        enum Resolution {

            FR(740 * 5),
            RR(185 * 5);

            private final int pixelCount;

            private Resolution(int pixelCount) {
                this.pixelCount = pixelCount;
            }

            private int getPixelCount() {
                return pixelCount;
            }
        }

        private static final int B = 15;

        private final Resolution resolution;

        final float[][] oldGains = new float[B][];
        final float[][] newGains = new float[B][];
        final float[][] oldBetas = new float[B][];
        final float[][] newBetas = new float[B][];
        final float[][] oldGammas = new float[B][];
        final float[][] newGammas = new float[B][];
        final float[][] oldDeltas = new float[B][];
        final float[][] newDeltas = new float[B][];

        final double[] oldRefTimes = new double[B];
        final double[] newRefTimes = new double[B];

        public Calibration(Resolution resolution,
                           String oldRadiometricCorrectionFileName,
                           String newRadiometricCorrectionFileName) throws URISyntaxException, IOException {
            this.resolution = resolution;
            read(oldRadiometricCorrectionFileName, oldGains, oldBetas, oldGammas, oldDeltas, oldRefTimes);
            read(newRadiometricCorrectionFileName, newGains, newBetas, newGammas, newDeltas, newRefTimes);
        }

        private void read(String radiometricCorrectionFileName,
                          float[][] gains,
                          float[][] betas,
                          float[][] gammas,
                          float[][] deltas,
                          double[] times) throws URISyntaxException, IOException {
            final URL url = getClass().getResource(radiometricCorrectionFileName);
            final URI uri = url.toURI();
            final File file = new File(uri);
            final ProductFile auxFile = new MerisRacProductFile(file);
            try {
                read(auxFile, "Gain", "gain", gains);
                read(auxFile, "Degradation", "beta", betas);
                read(auxFile, "Degradation", "gamma", gammas);
                read(auxFile, "Degradation", "delta", deltas);
                read(auxFile, "Degradation", "dsr_time", times);
            } finally {
                auxFile.close();
            }
        }

        private void read(ProductFile file, String dataset, String field, float[][] data) throws IOException {
            final RecordReader recordReader = file.getRecordReader(dataset + "_" + resolution.name());
            for (int b = 0; b < B; b++) {
                final Record record = recordReader.readRecord(b);
                data[b] = (float[]) record.getField(field).getElems();
            }
        }

        private void read(ProductFile file, String dataset, String field, double[] data) throws IOException {
            final RecordReader recordReader = file.getRecordReader(dataset + "_" + resolution.name());
            for (int b = 0; b < B; b++) {
                final Record record = recordReader.readRecord(b);
                data[b] = record.getField(field).getElemDouble(0);
            }
        }

        public double calibrate(int bandIndex, int detectorIndex, double radiance) {
            return radiance;
        }
    }
}
