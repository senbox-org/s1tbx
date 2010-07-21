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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.dataio.avhrr.calibration.IrRadianceCalibrator;
import org.esa.beam.dataio.avhrr.calibration.IrTemperatureCalibrator;
import org.esa.beam.dataio.avhrr.calibration.ReflectanceFactorCalibrator;
import org.esa.beam.dataio.avhrr.calibration.VisibleRadianceCalibrator;
import org.esa.beam.framework.datamodel.ProductData;

public class NoaaFile extends AvhrrFile implements AvhrrConstants {

	private static final int BITFIELD_OFFSET = 12;

	private ImageInputStream inputStream;

	private int firstDataRecordOffset;

	private int bitsPerPixel;
	
	private int dataRecordLength;
	
	private boolean hasCloudBand;

	private ArsHeader arsHeader;

	private GeneralInformationHeader giHeader;

	private DSQIndicatorHeader dsqHeader;

	private RadianceConversionHeader radianceConv;

	private NavigationHeader navigationHeader;

	public NoaaFile(ImageInputStream imageInputStream) {
		inputStream = imageInputStream;
	}

	@Override
    public void readHeader() throws IOException {
		hasCloudBand = false;
		int headerOffset = 0;

		// 1KByte more is enough
		final byte[] headerData = new byte[ARS_LENGTH + 1024];
		inputStream.readFully(headerData);

		String first3 = new String(headerData, 0, 3);
		if (!DataSetCreationSite.hasDatasetCreationSite(first3)) {
			InputStream arsStream = new ByteArrayInputStream(headerData, 0,
					ARS_LENGTH);
			arsHeader = new ArsHeader(arsStream);
			headerOffset = ARS_LENGTH;
		}

		InputStream giStream = new ByteArrayInputStream(headerData,
				headerOffset + GI_OFFSET, GI_LENGTH);
		giHeader = new GeneralInformationHeader(giStream);

		InputStream dsqStream = new ByteArrayInputStream(headerData,
				headerOffset + DSQI_OFFSET, DSQI_LENGTH);
		dsqHeader = new DSQIndicatorHeader(dsqStream);

		InputStream rcStream = new ByteArrayInputStream(headerData,
				headerOffset + RC_OFFSET, RC_LENGTH);
		radianceConv = new RadianceConversionHeader(rcStream);

		InputStream navStream = new ByteArrayInputStream(headerData,
				headerOffset + NAV_OFFSET, NAV_LENGTH);
		navigationHeader = new NavigationHeader(navStream);

		if (giHeader.getFormatVersion() >= 4) {
			int cloudStatus = 0;
			synchronized (inputStream) {
				inputStream.seek(headerOffset + 988);
				cloudStatus = inputStream.readUnsignedShort();
			}
			if(cloudStatus == 1) {
				hasCloudBand = true;
			}
		}
		calculateHeaderLength();
		detectFileType();
		analyzeScanLineBitfield();
	}

	@Override
    public String getProductName() {
		return giHeader.getDataSetName();
	}

	@Override
    public ProductData.UTC getStartDate() {
		return giHeader.getStartDate();
	}

	@Override
    public ProductData.UTC getEndDate() {
		return giHeader.getEndDate();
	}

	@Override
    public List getMetaData() {
		List metaDataList = new ArrayList();

		if (arsHeader != null) {
			metaDataList.add(arsHeader.getMetadata());
		}
		metaDataList.add(giHeader.getMetadata());
		metaDataList.add(dsqHeader.getMetadata());
		metaDataList.add(radianceConv.getMetadata());
		metaDataList.add(navigationHeader.getMetadata());

		return metaDataList;
	}

	@Override
    public BandReader createVisibleRadianceBandReader(int channel) {
		final VisibleRadianceCalibrator calibrator = new VisibleRadianceCalibrator(
				channel);
		calibrator.setHeaderConstants(radianceConv.getEquivalentWidth(channel),
				radianceConv.getSolarIrradiance(channel), navigationHeader
						.getEarthSunDistanceRatio());
		return createCountReader(channel, calibrator);
	}

	@Override
    public BandReader createIrRadianceBandReader(int channel) {
		final IrRadianceCalibrator calibrator = new IrRadianceCalibrator(
				channel);
		calibrator.setFormatVersion(giHeader.getFormatVersion());
		return createCountReader(channel, calibrator);
	}

	@Override
    public BandReader createIrTemperatureBandReader(int channel) {
		final IrTemperatureCalibrator calibrator = new IrTemperatureCalibrator(
				channel);
		calibrator.setFormatVersion(giHeader.getFormatVersion());
		calibrator.setHeaderConstants(radianceConv.getConstant1(channel),
				radianceConv.getConstant2(channel), radianceConv
						.getCentralWavenumber(channel));
		return createCountReader(channel, calibrator);
	}
	
	@Override
    public BandReader createReflectanceFactorBandReader(int channel) {
		final ReflectanceFactorCalibrator calibrator = new ReflectanceFactorCalibrator(
				channel);
		return createCountReader(channel, calibrator);
	}
	
	private CountReader createCountReader(int channel, Calibrator calibrator) {
		CountReader countReader = null;
		if (bitsPerPixel == 8) {
			countReader = new CountReader8Bit(channel, this, inputStream, calibrator);
		} else if (bitsPerPixel == 16) {
			countReader = new CountReader16Bit(channel, this, inputStream, calibrator);
		} else if (bitsPerPixel == 10) {
			countReader = new CountReader10Bit(channel, this, inputStream, calibrator);
		}
		return countReader;
	}

    @Override
    public String[] getTiePointNames() {
        return new String[]{SZA_DS_NAME, VZA_DS_NAME, DAA_DS_NAME, LAT_DS_NAME, LON_DS_NAME};
    }

    @Override
    public float[][] getTiePointData() throws IOException {
		final int gridHeight = getProductHeight() / TP_SUB_SAMPLING_Y + 1;
		final int numTiePoints = TP_GRID_WIDTH * gridHeight;

		float[][] tiePointData = new float[5][numTiePoints];
		final int numRawAngles = TP_GRID_WIDTH * 3;
		final int numRawLatLon = TP_GRID_WIDTH * 2;

		short[] rawAngles = new short[numRawAngles];
		int[] rawLatLon = new int[numRawLatLon];

		int targetIndex = 0;
		int targetIncr = 1;
		if (northbound) {
			targetIndex = TP_GRID_WIDTH * ((productHeight / TP_SUB_SAMPLING_Y) + 1) - 1;
			targetIncr = -1;
		}

		for (int scanLine = 0; scanLine < productHeight; scanLine += TP_SUB_SAMPLING_Y) {
			final int scanLineOffset = getScanLineOffset(scanLine);
			synchronized (inputStream) {
				inputStream.seek(scanLineOffset + 328);
				inputStream.readFully(rawAngles, 0, numRawAngles);
			}
			synchronized (inputStream) {
				inputStream.seek(scanLineOffset + 640);
				inputStream.readFully(rawLatLon, 0, numRawLatLon);
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
		return firstDataRecordOffset + (rawY * dataRecordLength);
	}
	
	@Override
    public int getFlagOffset(int rawY) {
		return getScanLineOffset(rawY) + 24;
	}

	private void detectFileType() throws IOException {
		final long fileLength = inputStream.length();
		final int dataRecords = dsqHeader.getDataRecordCounts();
		final int dataRecordSize = (int) ((fileLength - firstDataRecordOffset) / dataRecords);

		if (dataRecordSize == CountReader8Bit.DATA_RECORD_LENGTH) {
			bitsPerPixel = 8;
		} else if (dataRecordSize == CountReader16Bit.DATA_RECORD_LENGTH) {
			bitsPerPixel = 16;
		} else if (dataRecordSize == CountReader10Bit.DATA_RECORD_LENGTH) {
			bitsPerPixel = 10;
		} else {
			throw new IOException("Unsupported AVHRR data record size: "
					+ dataRecordSize);
		}
		giHeader.setBitsPerPixel(bitsPerPixel);
		dataRecordLength = dataRecordSize;
		
		int toSkip = (dataRecords % TP_SUB_SAMPLING_Y) - 1;
		if (toSkip < 0) {
			toSkip += TP_SUB_SAMPLING_Y;
		}
		productWidth = SCENE_RASTER_WIDTH;
		productHeight = (dataRecords - toSkip);
	}

	private void calculateHeaderLength() throws IOException {
		firstDataRecordOffset = 0;
		int headerOffset = 0;
		if (arsHeader != null) {
			headerOffset = ARS_LENGTH;
		}
		final int headerRecords = giHeader.getHeaderRecordCount();
		for (int i = 0; i < AvhrrConstants.HEADER_LENGTHS.length; i++) {
			final int headerLength = headerOffset
					+ (HEADER_LENGTHS[i] * headerRecords);
			synchronized (inputStream) {
				inputStream.seek(headerLength + 2);
				final int year = inputStream.readUnsignedShort();
				final int day = inputStream.readUnsignedShort();
				if (year > 1975 && year < 2025 && day >= 0 && day < 367) {
					firstDataRecordOffset = headerLength;
				}
			}
		}
		if (firstDataRecordOffset == 0) {
			throw new IOException("Failed to detect AVHRR header length");
		}
	}

	public int readScanLineBitField(int line) throws IOException {
		final int dataSetOffset = getScanLineOffset(line) + BITFIELD_OFFSET;
		final int bitField;
		synchronized (inputStream) {
			inputStream.seek(dataSetOffset);
			bitField = inputStream.readUnsignedShort();
		}
		return bitField;
	}

	private void analyzeScanLineBitfield() throws IOException {
		final int first = readScanLineBitField(0);
		final int last = readScanLineBitField(getProductHeight() - 1);

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

	public static boolean canOpenFile(File file) throws IOException {
		ImageInputStream inputStream = new FileImageInputStream(file);
		try {
			byte[] header = new byte[ARS_LENGTH + 3];
			if (inputStream.length() < header.length) {
				return false;
			}
			inputStream.readFully(header);
			String first3 = new String(header, 0, 3);
			if (DataSetCreationSite.hasDatasetCreationSite(first3)) {
				// System.out.println("AVHRR without ARS Header detected");
				return true;
			}
			String last3 = new String(header, ARS_LENGTH, 3);
			if (DataSetCreationSite.hasDatasetCreationSite(last3)) {
				// System.out.println("AVHRR with ARS Header detected");
				return true;
			}
		} finally {
			inputStream.close();
		}
		return false;
	}
	
	public boolean hasCloudBand() {
		return hasCloudBand;
	}
}
