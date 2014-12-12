/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.IrRadianceCalibrator;
import org.esa.beam.dataio.avhrr.calibration.IrTemperatureCalibrator;
import org.esa.beam.dataio.avhrr.calibration.ReflectanceFactorCalibrator;
import org.esa.beam.dataio.avhrr.calibration.VisibleRadianceCalibrator;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Represent a single NOAA AVHRR file.
 */
public class KlmAvhrrFile extends AvhrrFile implements AvhrrConstants {

    private final File file;
    private CompoundData noaaData;
    private DataContext context;
    private boolean hasCloudBand = false;
    private ProductFormat productFormat;
    private boolean hasArsHeader;

    public KlmAvhrrFile(File file) {
        this.file = file;
    }

    @Override
    public void readHeader() throws IOException {
        KlmFormatDetector detector = new KlmFormatDetector(file);
        Assert.state(detector.canDecode());
        productFormat = detector.getProductFormat();
        hasArsHeader = detector.hasArsHeader();
        detector.dispose();

        Debug.trace("ProductFormat=" + productFormat);
        Debug.trace("hasArsHeader=" + hasArsHeader);

        int blockSize = productFormat.getBlockSize();
        int dataRecordCount = getDataRecordCount(blockSize);
        CompoundType type = KlmTypes.getFileType(hasArsHeader, productFormat, dataRecordCount);

        DataFormat dataFormat = new DataFormat(type, ByteOrder.BIG_ENDIAN);
        context = dataFormat.createContext(file, "r");
        noaaData = context.getData();
        int tpSubsampling = productFormat.getProductDimension().getTpSubsampling();
        int toSkip = (dataRecordCount % tpSubsampling) - 1;
        if (toSkip < 0) {
            toSkip += tpSubsampling;
        }
        productWidth = productFormat.getProductDimension().getProductWidth();
        productHeight = (dataRecordCount - toSkip);

        if (getFormatVersion() >= 4 &&
            getHeader().getInt("CLAVR_STATUS") == 1) {
            hasCloudBand = true;
        }
        analyzeScanLineBitfield();
    }

    int getScanlineBitfield(int yIndex) throws IOException {
        return getDataRecord(yIndex).getInt("SCANLINE_BIT_FIELD");
    }

    private void analyzeScanLineBitfield() throws IOException {
        final int first = getScanlineBitfield(0);
        final int last = getScanlineBitfield(getProductHeight() - 1);

        final boolean isFirstSouthBound = ((first & (1 << 15)) >> 15) == 1;
        final boolean isLastSouthBound = ((last & (1 << 15)) >> 15) == 1;

        if (isFirstSouthBound && isLastSouthBound) {
            northbound = false;
        } else {
            northbound = !isFirstSouthBound && !isLastSouthBound;
        }

        final int firstChannel3ab = first & 3;
        final int lastChannel3ab = last & 3;
        if (firstChannel3ab == 1 && lastChannel3ab == 1) {
            channel3ab = AvhrrConstants.CH_3A;
        } else if (firstChannel3ab == 0 && lastChannel3ab == 0) {
            channel3ab = AvhrrConstants.CH_3B;
        } else {
            channel3ab = -1;
        }
    }

    private int getDataRecordCount(long blockSize) throws IOException {
        long realFileSize = file.length() - blockSize;
        if (hasArsHeader) {
            realFileSize -= AvhrrConstants.ARS_LENGTH;
        }
        return (int) (realFileSize / blockSize);
    }

    CompoundData getHeader() throws IOException {
        return noaaData.getCompound("HeaderRecord");
    }

    int getFormatVersion() throws IOException {
        return getHeader().getCompound("FILE_IDENTIFICATION").getInt("NOAA_LEVEL_1B_FORMAT_VERSION_NUMBER");
    }

    CompoundData getDataRecord(int yIndex) throws IOException {
        return noaaData.getSequence("DataRecord").getCompound(yIndex);
    }

    ProductFormat getProductFormat() {
        return productFormat;
    }

    @Override
    public String getProductName() throws IOException {
        SequenceData sequence = getHeader().getCompound("FILE_IDENTIFICATION").getSequence("DATA_SET_NAME");
        return HeaderWrapper.getAsString(sequence);
    }

    @Override
    public ProductData.UTC getStartDate() throws IOException {
        CompoundData compound = getHeader().getCompound("FILE_IDENTIFICATION").getCompound("START_OF_DATA_SET");
        ProductData.UTC date = HeaderWrapper.createDate(compound);
        return date;
    }

    @Override
    public ProductData.UTC getEndDate() throws IOException {
        CompoundData compound = getHeader().getCompound("FILE_IDENTIFICATION").getCompound("END_OF_DATA_SET");
        ProductData.UTC date = HeaderWrapper.createDate(compound);
        return date;
    }

    @Override
    public void addMetaData(MetadataElement metadataRoot) throws IOException {
        if (hasArsHeader) {
            HeaderWrapper headerWrapper = new HeaderWrapper(noaaData.getCompound(0));
            metadataRoot.addElement(headerWrapper.getAsMetadataElement());
        }
        HeaderWrapper headerWrapper = new HeaderWrapper(getHeader());
        metadataRoot.addElement(headerWrapper.getAsMetadataElement());
    }

    @Override
    public BandReader createVisibleRadianceBandReader(int channel) throws IOException {
        final VisibleRadianceCalibrator calibrator = new VisibleRadianceCalibrator(channel);
        String channelString = CH_STRINGS[channel].toUpperCase();
        calibrator.setHeaderConstants(
                HeaderWrapper.getValue(getHeader().getCompound("RADIANCE_CONVERSION"),
                                       String.format("CHANNEL_%s_EQUIVALENT_WIDTH", channelString)),
                HeaderWrapper.getValue(getHeader().getCompound("RADIANCE_CONVERSION"),
                                       String.format("CHANNEL_%s_SOLAR_IRRADIANCE", channelString)),
                HeaderWrapper.getValue(getHeader().getCompound("NAVIGATION"), "EARTH_SUN_DISTANCE_RATIO")
        );
        return productFormat.createCountReader(channel, this, calibrator);
    }

    @Override
    public BandReader createIrRadianceBandReader(int channel) throws IOException {
        final IrRadianceCalibrator calibrator = new IrRadianceCalibrator(channel);
        calibrator.setFormatVersion(getFormatVersion());
        return productFormat.createCountReader(channel, this, calibrator);
    }

    @Override
    public BandReader createIrTemperatureBandReader(int channel) throws IOException {
        final IrTemperatureCalibrator calibrator = new IrTemperatureCalibrator(channel);
        calibrator.setFormatVersion(getFormatVersion());
        String channelString = CH_STRINGS[channel].toUpperCase();
        calibrator.setHeaderConstants(
                HeaderWrapper.getValue(getHeader().getCompound("RADIANCE_CONVERSION"),
                                       String.format("CHANNEL_%s_CONSTANT_1", channelString)),
                HeaderWrapper.getValue(getHeader().getCompound("RADIANCE_CONVERSION"),
                                       String.format("CHANNEL_%s_CONSTANT_2", channelString)),
                HeaderWrapper.getValue(getHeader().getCompound("RADIANCE_CONVERSION"),
                                       String.format("CHANNEL_%s_CENTRAL_WAVENUMBER", channelString))
        );
        return productFormat.createCountReader(channel, this, calibrator);
    }

    @Override
    public BandReader createReflectanceFactorBandReader(int channel) {
        final ReflectanceFactorCalibrator calibrator = new ReflectanceFactorCalibrator(
                channel);
        return productFormat.createCountReader(channel, this, calibrator);
    }

    @Override
    public BandReader createFlagBandReader() {
        return new FlagReader(this);
    }

    @Override
    public BandReader createCloudBandReader() {
        return new CloudReader(this);
    }

    @Override
    public String[] getTiePointNames() {
        return new String[]{SZA_DS_NAME, VZA_DS_NAME, DAA_DS_NAME, LAT_DS_NAME, LON_DS_NAME};
    }

    @Override
    public float[][] getTiePointData() throws IOException {
        final int tiePointSubsampling = getTiePointSubsampling();
        final int gridHeight = getProductHeight() / tiePointSubsampling + 1;
        final int numTiePoints = TP_GRID_WIDTH * gridHeight;

        float[][] tiePointData = new float[5][numTiePoints];
        final int numRawAngles = TP_GRID_WIDTH * 3;
        final int numRawLatLon = TP_GRID_WIDTH * 2;

        short[] rawAngles = new short[numRawAngles];
        int[] rawLatLon = new int[numRawLatLon];

        int targetIndex = 0;
        int targetIncr = 1;
        if (northbound) {
            targetIndex = TP_GRID_WIDTH * ((productHeight / tiePointSubsampling) + 1) - 1;
            targetIncr = -1;
        }

        for (int scanLine = 0; scanLine < productHeight; scanLine += tiePointSubsampling) {
            CompoundData dataRecord = getDataRecord(scanLine);
            SequenceData angularRelationships = dataRecord.getSequence("ANGULAR_RELATIONSHIPS");
            for (int i = 0; i < numRawAngles; i++) {
                rawAngles[i] = angularRelationships.getShort(i);
            }
            SequenceData earthLocation = dataRecord.getSequence("EARTH_LOCATION");
            for (int i = 0; i < numRawLatLon; i++) {
                rawLatLon[i] = earthLocation.getInt(i);
            }

            for (int scanPoint = 0; scanPoint < AvhrrConstants.TP_GRID_WIDTH; scanPoint++) {
                tiePointData[0][targetIndex] = rawAngles[scanPoint * 3] * 1E-2f;
                tiePointData[1][targetIndex] = rawAngles[scanPoint * 3 + 1] * 1E-2f;
                tiePointData[2][targetIndex] = rawAngles[scanPoint * 3 + 2] * 1E-2f;
                tiePointData[3][targetIndex] = rawLatLon[scanPoint * 2] * 1E-4f;
                tiePointData[4][targetIndex] = rawLatLon[scanPoint * 2 + 1] * 1E-4f;
                targetIndex += targetIncr;
            }
        }
        return tiePointData;
    }

    @Override
    public int getScanLineOffset(int rawY) {
        return 0;// unused here
    }

    @Override
    public int getFlagOffset(int rawY) {
        return 0;// unused here
    }

    @Override
    public int getTiePointTrimX() {
        return productFormat.getProductDimension().getTpTrimX();
    }

    @Override
    public int getTiePointSubsampling() {
        return productFormat.getProductDimension().getTpSubsampling();
    }

    @Override
    public boolean hasCloudBand() {
        return hasCloudBand;
    }

    @Override
    public void dispose() {
        if (context != null) {
            context.dispose();
            context = null;
        }
        noaaData = null;
    }

    public static boolean canDecode(File file) {
        KlmFormatDetector formatDetector = null;
        try {
            formatDetector = new KlmFormatDetector(file);
            return formatDetector.canDecode();
        } catch (Throwable e) {
            return false;
        } finally {
            if (formatDetector != null) {
                formatDetector.dispose();
            }
        }
    }
}
