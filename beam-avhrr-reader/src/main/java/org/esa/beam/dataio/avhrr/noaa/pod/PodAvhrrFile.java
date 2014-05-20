package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.HeaderUtil;
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
 * Represents a NOAA POD AVHRR HRPT file.
 *
 * @author Ralf Quast
 */
final class PodAvhrrFile implements VideoDataProvider, Validator, CalibrationCoefficientsProvider {

    private static final int PRODUCT_WIDTH = 2001;
    private static final int TIE_POINT_SUB_SAMPLING = 40;
    private static final int TIE_POINT_GRID_WIDTH = 51;

    private static final double SLOPE_SCALE_FACTOR = PodTypes.getSlopeMetadata().getScalingFactor();
    private static final double INTERCEPT_SCALE_FACTOR = PodTypes.getInterceptMetadata().getScalingFactor();

    private final File file;
    private final DataContext context;
    private final CompoundData data;
    private final int dataRecordsIndex;
    private final int videoDataIndex;
    private final int qualityDataIndex;
    private final int calibrationCofficientsIndex;
    private final Map<Band, BandReader> bandReaderMap;

    static boolean canDecode(File file) {
        return new PodFormatDetector().canDecode(file);
    }

    PodAvhrrFile(File file) throws FileNotFoundException {
        this.file = file;

        final DataFormat dataFormat = new DataFormat(PodTypes.HRPT_TYPE, ByteOrder.BIG_ENDIAN);
        context = dataFormat.createContext(file, "r");
        data = context.getData();
        dataRecordsIndex = PodTypes.HRPT_TYPE.getMemberIndex("DATA_RECORDS");
        videoDataIndex = PodTypes.DATA_RECORD_TYPE.getMemberIndex("VIDEO_DATA");
        qualityDataIndex = PodTypes.DATA_RECORD_TYPE.getMemberIndex("QUALITY_INDICATORS");
        calibrationCofficientsIndex = PodTypes.DATA_RECORD_TYPE.getMemberIndex("CALIBRATION_COEFFICIENTS");
        bandReaderMap = new HashMap<>(15);
    }

    @Override
    public SequenceData getVideoData(int i) throws IOException {
        return getDataRecord(i).getSequence(videoDataIndex);
    }

    @Override
    public boolean isValid(int i) throws IOException {
        return (getDataRecord(i).getInt(qualityDataIndex) & 0b11001100) == 0;
    }

    @Override
    public SequenceData getCalibrationCoefficients(int i) throws IOException {
        return getDataRecord(i).getSequence(calibrationCofficientsIndex);
    }

    @Override
    public double getSlopeScaleFactor() {
        return SLOPE_SCALE_FACTOR;
    }

    @Override
    public double getInterceptScaleFactor() {
        return INTERCEPT_SCALE_FACTOR;
    }

    void dispose() throws IOException {
        context.dispose();
    }

    BandReader getBandReader(Band band) {
        return bandReaderMap.get(band);
    }

    Product createProduct() throws IOException {
        final String productName = file.getName();
        final String productType = "NOAA_POD_AVHRR_HRPT";
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

        addAlbedoBand(product, 0);
        addAlbedoBand(product, 1);
        addRadianceBand(product, 2);
        addRadianceBand(product, 3);
        addRadianceBand(product, 4);

        addTiePointGridsAndGeoCoding(product);

        final CompoundData startTimeCode = getDataRecord(0).getCompound("TIME_CODE");
        final ProductData.UTC startTime = toUTC(startTimeCode);
        product.setStartTime(startTime);

        final CompoundData endTimeCode = getDataRecord(productHeight - 1).getCompound("TIME_CODE");
        final ProductData.UTC endTime = toUTC(endTimeCode);
        product.setEndTime(endTime);

        return product;
    }

    // package public for testing only
    static ProductData.UTC toUTC(CompoundData timeCode) throws IOException {
        final int yearDay = timeCode.getUShort(0);
        final int year = 1900 + (yearDay >> 9);
        final int dayOfYear = yearDay & 0b0000000111111111;
        final int millisInDay = timeCode.getInt(1);

        return HeaderUtil.createUTCDate(year, dayOfYear, millisInDay);
    }

    private Band addCountsBand(Product product, int channelIndex) {
        final BandReader bandReader = BandReaderFactory.createCountBandReader(channelIndex, this, this);
        return addBand(product, bandReader, channelIndex);
    }

    private Band addAlbedoBand(Product product, int channelIndex) {
        final BandReader bandReader = BandReaderFactory.createAlbedoBandReader(channelIndex, this, this, this);
        return addBand(product, bandReader, channelIndex);
    }

    private Band addRadianceBand(Product product, int channelIndex) {
        final BandReader bandReader = BandReaderFactory.createRadianceBandReader(channelIndex, this, this, this);
        return addBand(product, bandReader, channelIndex);
    }

    private Band addBand(Product product, BandReader bandReader, int channelIndex) {
        final Band band = product.addBand(bandReader.getBandName(), bandReader.getDataType());

        band.setScalingFactor(bandReader.getScalingFactor());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setSpectralBandIndex(channelIndex + 1);

        bandReaderMap.put(band, bandReader);

        return band;
    }

    private void addTiePointGridsAndGeoCoding(Product product) throws IOException {
        final int rasterHeight = product.getSceneRasterHeight();
        final int gridHeight = rasterHeight / TIE_POINT_SUB_SAMPLING + 1;
        final int tiePointCount = TIE_POINT_GRID_WIDTH * gridHeight;
        float[][] gridData = new float[3][tiePointCount];

        for (int tiePointIndex = 0, y = 0; y < rasterHeight; y += TIE_POINT_SUB_SAMPLING) {
            final int[] rawAngles = new int[TIE_POINT_GRID_WIDTH];
            final int[] rawLat = new int[TIE_POINT_GRID_WIDTH];
            final int[] rawLon = new int[TIE_POINT_GRID_WIDTH];
            final CompoundData dataRecord = getDataRecord(y);
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

            for (int pointIndex = 0; pointIndex < TIE_POINT_GRID_WIDTH; pointIndex++) {
                gridData[0][tiePointIndex] = (float) (rawAngles[pointIndex] * solarZenithAnglesScaleFactor);
                gridData[1][tiePointIndex] = (float) (rawLat[pointIndex] * latScaleFactor);
                gridData[2][tiePointIndex] = (float) (rawLon[pointIndex] * lonScaleFactor);
                tiePointIndex += 1;
            }
        }

        final TiePointGrid latGrid = addTiePointGrid(product, AvhrrConstants.LAT_DS_NAME,
                                                     PodTypes.getLatMetadata().getUnits(),
                                                     gridHeight, gridData[1]);
        final TiePointGrid lonGrid = addTiePointGrid(product, AvhrrConstants.LON_DS_NAME,
                                                     PodTypes.getLonMetadata().getUnits(),
                                                     gridHeight, gridData[2]);
        addTiePointGrid(product, AvhrrConstants.SZA_DS_NAME, PodTypes.getSolarZenithAnglesMetadata().getUnits(),
                        gridHeight, gridData[0]);

        // TODO: inverse approximation in tie point geo-coding works incorrect with wide-swath data rq-20140520
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    private TiePointGrid addTiePointGrid(Product product, String gridName, String units, int gridHeight,
                                         float[] gridData) {
        final TiePointGrid grid = new TiePointGrid(gridName,
                                                   TIE_POINT_GRID_WIDTH,
                                                   gridHeight,
                                                   0.5f, 0.5f,
                                                   TIE_POINT_SUB_SAMPLING,
                                                   TIE_POINT_SUB_SAMPLING, gridData);
        grid.setUnit(units);
        product.addTiePointGrid(grid);
        return grid;
    }

    private CompoundData getDataRecord(int i) throws IOException {
        return data.getSequence(dataRecordsIndex).getCompound(i);
    }
}
