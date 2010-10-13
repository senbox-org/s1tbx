package org.esa.beam.meris.radiometry.calibration;

import org.esa.beam.dataio.envisat.Field;
import org.esa.beam.dataio.envisat.MerisRacProductFile;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.dataio.envisat.Record;
import org.esa.beam.dataio.envisat.RecordReader;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CalibrationAlgorithm {

    private static final int B = 15;

    private final Resolution resolution;
    private final double cntJD;

    private final double[][] oldGains;
    private final double[][] newGains;

    public CalibrationAlgorithm(Resolution resolution, double cntJD,
                                InputStream sourceRacStream, InputStream targetRacStream) throws IOException {
        this.resolution = resolution;
        this.cntJD = cntJD;

        oldGains = new double[B][resolution.getPixelCount()];
        newGains = new double[B][resolution.getPixelCount()];

        initGains(new MemoryCacheImageInputStream(sourceRacStream), oldGains);
        initGains(new MemoryCacheImageInputStream(targetRacStream), newGains);
    }

    public double calibrate(int bandIndex, int detectorIndex, double radiance) {
        return newGains[bandIndex][detectorIndex] / oldGains[bandIndex][detectorIndex] * radiance;
    }

    private void initGains(ImageInputStream inputStream, double[][] gains) throws IOException {
        final double[][] betas = new double[B][resolution.getPixelCount()];
        final double[][] gammas = new double[B][resolution.getPixelCount()];
        final double[][] deltas = new double[B][resolution.getPixelCount()];
        final double[] refJDs = new double[B];

        ProductFile productFile = null;
        try {
            productFile = new MerisRacProductFile(inputStream);
            read(productFile, "Gain", "gain", gains);
            read(productFile, "Degradation", "beta", betas);
            read(productFile, "Degradation", "gamma", gammas);
            read(productFile, "Degradation", "delta", deltas);
            readJD(productFile, "Degradation", "dsr_time", refJDs);
        } catch (Exception e) {
            throw new IOException("Cannot read auxiliary data", e);
        } finally {
            if (productFile != null) {
                productFile.close();
            }
        }
        for (int b = 0; b < B; b++) {
            degradeGains(gains[b], betas[b], gammas[b], deltas[b], refJDs[b]);
        }
    }

    private void degradeGains(double[] gains, double[] betas, double[] gammas, double[] deltas, double refJD) {
        for (int i = 0; i < resolution.getPixelCount(); i++) {
            gains[i] /= (1.0 - betas[i] * (1.0 - gammas[i] * Math.exp(deltas[i] * (refJD - cntJD))));
        }
    }

    private void read(ProductFile file, String datasetName, String fieldName, double[][] fieldData) throws
                                                                                                    IOException {
        final RecordReader recordReader = file.getRecordReader(datasetName + "_" + resolution.name());
        for (int b = 0; b < B; b++) {
            final Record record = recordReader.readRecord(b);
            final ProductData productData = record.getField(fieldName).getData();
            for (int i = 0; i < resolution.getPixelCount(); i++) {
                fieldData[b][i] = productData.getElemDoubleAt(i);
            }
        }
    }

    private void readJD(ProductFile file, String datasetName, String fieldName, double[] refJDs) throws IOException {
        final RecordReader recordReader = file.getRecordReader(datasetName + "_" + resolution.name());
        for (int b = 0; b < B; b++) {
            final Record record = recordReader.readRecord(b);
            final Field field = record.getField(fieldName);
            final int days = field.getElemInt(0);
            final int seconds = field.getElemInt(1);
            final int microseconds = field.getElemInt(2);
            refJDs[b] = microseconds / 1000000 + seconds / 86400 + days;
        }
    }
}
