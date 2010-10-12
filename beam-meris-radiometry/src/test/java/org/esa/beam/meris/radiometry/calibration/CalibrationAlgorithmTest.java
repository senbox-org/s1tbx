package org.esa.beam.meris.radiometry.calibration;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class CalibrationAlgorithmTest {

    @Test
    public void calibration() throws IOException, URISyntaxException {
        final File oldRacFile = getResourceAsFile("MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000");
        final File newRacFile = getResourceAsFile("MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000");
        final CalibrationAlgorithm calibrationAlgorithm =
                new CalibrationAlgorithm(Resolution.RR, 1247.4, oldRacFile, newRacFile);

        // from exported pixel in validation product
        final double sourceRadiance = 119.61037;
        final double targetRadiance = 120.02557;
        assertEquals(targetRadiance, calibrationAlgorithm.calibrate(0, 759, sourceRadiance), 1.0e-4);
    }

    private File getResourceAsFile(String fileName) throws URISyntaxException, IOException {
        final URL url = getClass().getResource(fileName);
        final URI uri = url.toURI();
        return new File(uri);
    }
}
