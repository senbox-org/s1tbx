package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.dataio.avhrr.calibration.CountsCalibrator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Quast
 */
public final class PodAvhrrFile {

    private static final int PRODUCT_WIDTH = 2001;
    private static final int POINTS_PER_SCAN = 2048;
    private static final int WORDS_PER_SCAN = 3414;
    private static final int TIE_POINT_SUB_SAMPLING = 40;
    private static final int TIE_POINT_TRIM_X = 25;
    private static final int TIE_POINT_GRID_WIDTH = 51;

    private final File file;
    private final DataContext context;
    private final CompoundData data;
    private final int dataRecordsIndex;
    private final int videoDataIndex;
    private final Map<Band, BandReader> bandReaderMap;

    public static boolean canDecode(File file) {
        return new PodFormatDetector().canDecode(file);
    }

    public PodAvhrrFile(File file) throws FileNotFoundException {
        this.file = file;

        final DataFormat dataFormat = new DataFormat(PodTypes.hrptType, ByteOrder.BIG_ENDIAN);
        context = dataFormat.createContext(file, "r");
        data = context.getData();
        dataRecordsIndex = PodTypes.hrptType.getMemberIndex("DATA_RECORDS");
        videoDataIndex = PodTypes.dataRecordType.getMemberIndex("VIDEO_DATA");
        bandReaderMap = new HashMap<>(15);
    }

    public void dispose() throws IOException {
        context.dispose();
    }

    public BandReader getBandReader(Band band) {
        return bandReaderMap.get(band);
    }

    public Product createProduct() throws IOException {
        final String productName = file.getName();
        final String productType = "NOAA_POD_HRPT_AVHRR_3_L1B";
        final int dataRecordCount = data.getUShort("NUMBER_OF_SCANS");
        int toSkip = (dataRecordCount % TIE_POINT_SUB_SAMPLING) - 1;
        if (toSkip < 0) {
            toSkip += TIE_POINT_SUB_SAMPLING;
        }
        final int productHeight = (dataRecordCount - toSkip);
        final Product product = new Product(productName, productType, PRODUCT_WIDTH, productHeight);

        addCountsBand(product, 0);
        addCountsBand(product, 1);
        addCountsBand(product, 2);
        addCountsBand(product, 3);
        addCountsBand(product, 4);

        // ////////////////////////////////////////////////////////////////
        // Create the visual radiance and IR radiance bands
        /*
        addBand(createVisibleRadianceBand(CH_1));
        addBand(createVisibleRadianceBand(CH_2));
        if (channel3ab == CH_3A) {
            addBand(createVisibleRadianceBand(CH_3A));
            addBand(createZeroFilledBand(CH_3B, RADIANCE_BAND_NAME_PREFIX));
        } else if (channel3ab == CH_3B) {
            addBand(createZeroFilledBand(CH_3A, RADIANCE_BAND_NAME_PREFIX));
            addBand(createIrRadianceBand(CH_3B));
        } else {
            addBand(createVisibleRadianceBand(CH_3A));
            addBand(createIrRadianceBand(CH_3B));
        }
        addBand(createIrRadianceBand(CH_4));
        addBand(createIrRadianceBand(CH_5));

        // ////////////////////////////////////////////////////////////////
        // Create the visual reflectance and IR temperature bands

        addBand(createReflectanceFactorBand(CH_1));
        addBand(createReflectanceFactorBand(CH_2));
        if (channel3ab == CH_3A) {
            addBand(createReflectanceFactorBand(CH_3A));
            addBand(createZeroFilledBand(CH_3B, TEMPERATURE_BAND_NAME_PREFIX));
        } else if (channel3ab == CH_3B) {
            addBand(createZeroFilledBand(CH_3A, REFLECTANCE_BAND_NAME_PREFIX));
            addBand(createIrTemperatureBand(CH_3B));
        } else {
            addBand(createReflectanceFactorBand(CH_3A));
            addBand(createIrTemperatureBand(CH_3B));
        }
        addBand(createIrTemperatureBand(CH_4));
        addBand(createIrTemperatureBand(CH_5));

        // ////////////////////////////////////////////////////////////////
        // Create the inverted IR temperature bands

        addFlagCodingAndBitmaskDef();
        addCloudBand();
        product.setStartTime(avhrrFile.getStartDate());
        product.setEndTime(avhrrFile.getEndDate());
        avhrrFile.addMetaData(product.getMetadataRoot());
        */

        addTiePointGridsAndGeoCoding(product);

        return product;
    }

    private Band addCountsBand(Product product, int channelIndex) {
        final Calibrator calibrator = new CountsCalibrator(channelIndex);
        final VideoDataProvider videoDataProvider = new VideoDataProvider() {

            @Override
            public SequenceData getVideoData(int y) throws IOException {
                return PodAvhrrFile.this.getVideoData(y);
            }
        };
        final BandReader bandReader = new CountsBandReader(videoDataProvider, channelIndex, calibrator);

        return addBand(product, bandReader, channelIndex);
    }

    private Band addBand(Product product, BandReader bandReader, int channelIndex) {
        final Band band = product.addBand(bandReader.getBandName(), bandReader.getDataType());

        band.setScalingFactor(bandReader.getScalingFactor());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setSpectralBandIndex(channelIndex);
        // TODO - band.setSpectralWavelength(...);
        // TODO - band.setSpectralBandwidth(...);
        // TODO - band.setValidPixelExpression(...);
        band.setNoDataValue(0.0);
        band.setNoDataValueUsed(true);

        bandReaderMap.put(band, bandReader);

        return band;
    }

    private void addTiePointGridsAndGeoCoding(Product product) throws IOException {
        final int productHeight = product.getSceneRasterHeight();
        final int gridHeight = productHeight / TIE_POINT_SUB_SAMPLING + 1;
        final int tiePointCount = TIE_POINT_GRID_WIDTH * gridHeight;

        float[][] tiePointData = new float[3][tiePointCount];
        for (int tiePointIndex = 0, scanLine = 0; scanLine < productHeight; scanLine += TIE_POINT_SUB_SAMPLING) {
            final int[] rawAngles = new int[TIE_POINT_GRID_WIDTH];
            final int[] rawLat = new int[TIE_POINT_GRID_WIDTH];
            final int[] rawLon = new int[TIE_POINT_GRID_WIDTH];
            final CompoundData dataRecord = getDataRecord(scanLine);
            final SequenceData solarZenithAngleSequence = dataRecord.getSequence("SOLAR_ZENITH_ANGLES");
            for (int i = 0; i < TIE_POINT_GRID_WIDTH; i++) {
                rawAngles[i] = solarZenithAngleSequence.getByte(i);
            }
            final SequenceData earthLocationSequence = dataRecord.getSequence("EARTH_LOCATION");
            for (int i = 0; i < TIE_POINT_GRID_WIDTH; i++) {
                rawLat[i] = earthLocationSequence.getCompound(i).getInt(0);
                rawLon[i] = earthLocationSequence.getCompound(i).getInt(1);
            }
            final double solarZenithAnglesScaleFactor = PodTypes.getSolarZenithAnglesMetadata().getScalingFactor();
            final double latScaleFactor = PodTypes.getLatMetadata().getScalingFactor();
            final double lonScaleFactor = PodTypes.getLonMetadata().getScalingFactor();

            for (int point = 0; point < TIE_POINT_GRID_WIDTH; point++) {
                tiePointData[0][tiePointIndex] = (float) (rawAngles[point] * solarZenithAnglesScaleFactor);
                tiePointData[1][tiePointIndex] = (float) (rawLat[point] * latScaleFactor);
                tiePointData[2][tiePointIndex] = (float) (rawLon[point] * lonScaleFactor);
                tiePointIndex += 1;
            }
        }

        final TiePointGrid latGrid = addTiePointGrid(product, AvhrrConstants.LAT_DS_NAME,
                                                     PodTypes.getLatMetadata().getUnits(),
                                                     gridHeight, tiePointData[1]);
        final TiePointGrid lonGrid = addTiePointGrid(product, AvhrrConstants.LON_DS_NAME,
                                                     PodTypes.getLonMetadata().getUnits(),
                                                     gridHeight, tiePointData[2]);
        addTiePointGrid(product, AvhrrConstants.SZA_DS_NAME, PodTypes.getSolarZenithAnglesMetadata().getUnits(),
                        gridHeight, tiePointData[0]);

        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    private TiePointGrid addTiePointGrid(Product product, String name, String units, int tiePointGridHeight,
                                         float[] tiePointData) {
        final TiePointGrid grid = new TiePointGrid(name,
                                                   TIE_POINT_GRID_WIDTH,
                                                   tiePointGridHeight,
                                                   0.5f, 0.5f,
                                                   TIE_POINT_SUB_SAMPLING,
                                                   TIE_POINT_SUB_SAMPLING, tiePointData);
        grid.setUnit(units);
        product.addTiePointGrid(grid);
        return grid;
    }

    private CompoundData getDataRecord(int i) throws IOException {
        return data.getSequence(dataRecordsIndex).getCompound(i);
    }

    private SequenceData getVideoData(int i) throws IOException {
        return getDataRecord(i).getSequence(videoDataIndex);
    }

    private static class CountsBandReader implements BandReader {

        private final VideoDataProvider videoDataProvider;
        private final int channel;
        private final Calibrator calibrator;

        public CountsBandReader(VideoDataProvider videoDataProvider, int channel, Calibrator calibrator) {
            this.videoDataProvider = videoDataProvider;
            this.channel = channel;
            this.calibrator = calibrator;
        }

        @Override
        public String getBandName() {
            return calibrator.getBandName();
        }

        @Override
        public String getBandUnit() {
            return calibrator.getBandUnit();
        }

        @Override
        public String getBandDescription() {
            return calibrator.getBandDescription();
        }

        @Override
        public double getScalingFactor() {
            return 1.0;
        }

        @Override
        public int getDataType() {
            return ProductData.TYPE_FLOAT32;
        }

        @Override
        public void readBandRasterData(int sourceOffsetX,
                                       int sourceOffsetY,
                                       int sourceWidth,
                                       int sourceHeight,
                                       int sourceStepX,
                                       int sourceStepY,
                                       ProductData destBuffer, ProgressMonitor pm) throws IOException {
            final int minX = sourceOffsetX + TIE_POINT_TRIM_X;
            final int maxX = sourceOffsetX + sourceWidth - 1 + TIE_POINT_TRIM_X;
            final int minY = sourceOffsetY;
            final int maxY = sourceOffsetY + sourceHeight - 1;
            final int targetStart = 0;
            final int targetIncrement = 1;

            final float[] targetData = (float[]) destBuffer.getElems();
            final int[] videoData = new int[WORDS_PER_SCAN];
            final int[] countData = new int[POINTS_PER_SCAN];

            int targetIdx = targetStart;
            pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", maxY - minY);
            try {
                for (int y = minY; y <= maxY; y += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }

                    boolean valid = hasData(y);
                    if (valid) {
                        if (calibrator.requiresCalibrationData()) {
                            // TODO - implement
                        }
                        if (valid) {
                            readVideoData(y, videoData);
                            getCounts(videoData, countData);
                            if (valid) {
                                for (int x = minX; x <= maxX; x += sourceStepX) {
                                    targetData[targetIdx] = calibrator.calibrate(countData[x]);
                                    targetIdx += targetIncrement;
                                }
                            }
                        }
                    }
                    if (!valid) {
                        for (int x = minX; x <= maxX; x += sourceStepX) {
                            targetData[targetIdx] = AvhrrConstants.NO_DATA_VALUE;
                            targetIdx += targetIncrement;
                        }
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }

        private boolean hasData(int y) {
            return true;
        }

        private static final int TEN_BITS = 0b1111111111;

        private static final int[] FIRST = {0, 0, 0, 1, 1};
        private static final int[][] INCREMENT = {{1, 2, 2}, {2, 1, 2}, {2, 2, 1}, {1, 2, 2}, {2, 1, 2}};
        private static final int[][] SHIFT = {{20, 0, 10}, {10, 20, 0}, {0, 10, 20}, {20, 0, 10}, {10, 20, 0}};

        private void readVideoData(int y, int[] videoData) throws IOException {
            final SequenceData videoDataSequence = videoDataProvider.getVideoData(y);
            for (int i = 0; i < videoDataSequence.getElementCount(); i++) {
                videoData[i] = videoDataSequence.getInt(i);
            }
        }

        private void getCounts(int[] videoData, int[] counts) {
            final int datasetIndex = AvhrrConstants.CH_DATASET_INDEXES[channel];
            final int[] shifts = SHIFT[datasetIndex];
            final int[] increments = INCREMENT[datasetIndex];

            for (int i = 0, j = 0, rawIndex = FIRST[datasetIndex]; i < POINTS_PER_SCAN; i++) {
                counts[i] = (videoData[rawIndex] & (TEN_BITS << shifts[j])) >> shifts[j];
                rawIndex += increments[j];
                j = j == 2 ? 0 : j + 1;
            }
        }
    }

    private static interface VideoDataProvider {

        SequenceData getVideoData(int y) throws IOException;
    }
}
