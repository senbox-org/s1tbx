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

package org.esa.beam.dataio.chris;

import org.esa.beam.util.io.CsvReader;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ChrisFile {

    private File file;
    private NetcdfFile ncFile = null;
    private Variable rciImageSds;
    private Variable maskSds;
    private Map<String, String> globalAttributes;
    private Map<Integer, Float> gainInfoMap;
    private float[][] modeInfo;
    private ScanLineLayout scanLineLayout;
    private int sceneRasterHeight;
    private boolean flipped;

    private Map<String, ScanLineLayout> scanLineLayoutMap;

    public ChrisFile(File file) {
        this.file = file;

        try {
            scanLineLayoutMap = readScanLineLayoutMap();
        } catch (IOException e) {
            // ignore
        }
    }

    public void open() throws IOException {
        if (ncFile != null) {
            throw new IllegalStateException("already open");
        }

        try {
            ncFile = NetcdfFile.open(file.getAbsolutePath());
            globalAttributes = readGlobalAttributes(ncFile);
            rciImageSds = getRciSds(ncFile);
            maskSds = getMaskSds(ncFile);
            modeInfo = readModeInfo(ncFile);
            gainInfoMap = readGainInfo(ncFile);
        } catch (Exception e) {
            try {
                close();
            } catch (IOException ignored) {
                // ignore
            }
            final IOException ioe = new IOException(MessageFormat.format("Failed to open CHRIS file ''{0}'':\n{1}", file, e.getMessage()));
            ioe.initCause(e);

            throw ioe;
        }

        determineProcessingVersion();
        determineSceneRasterHeight();
        determineFlipping();
        determineScanLineLayout();
    }

    public void close() throws IOException {
        if (ncFile == null) {
            return;
        }
        try {
            if (globalAttributes != null) {
                globalAttributes.clear();
            }
            ncFile.close();
        } catch (IOException e) {
            final IOException ioe = new IOException(MessageFormat.format("Failed to close CHRIS file ''{0}''", file));
            ioe.initCause(e);
            throw ioe;
        } finally {
            rciImageSds = null;
            maskSds = null;
            modeInfo = null;
            ncFile = null;
        }
    }

    public File getFile() {
        return file;
    }

    public int getSceneRasterWidth() {
        return scanLineLayout.imagePixelCount;
    }

    public int getSceneRasterHeight() {
        return sceneRasterHeight;
    }

    public int getSpectralBandCount() {
        return rciImageSds.getDimension(0).getLength();
    }

    public float getCutOnWavelength(int bandIndex) {
        return modeInfo[bandIndex][0];
    }

    public float getCutOffWavelength(int bandIndex) {
        return modeInfo[bandIndex][1];
    }

    public float getWavelength(int bandIndex) {
        return modeInfo[bandIndex][2];
    }

    public float getBandwidth(int bandIndex) {
        return modeInfo[bandIndex][3];
    }

    public int getGainSetting(int bandIndex) {
        return (int) modeInfo[bandIndex][4];
    }

    public float getGainValue(int bandIndex) {
        final int gainSetting = getGainSetting(bandIndex);

        if (gainInfoMap.containsKey(gainSetting)) {
            return gainInfoMap.get(gainSetting);
        }

        return Float.NaN;
    }

    public int getLowRow(int bandIndex) {
        return (int) modeInfo[bandIndex][5];
    }

    public int getHighRow(int bandIndex) {
        return (int) modeInfo[bandIndex][6];
    }

    public boolean hasMask() {
        return maskSds != null;
    }

    public void readRciData(int bandIndex,
                            int offsetX,
                            int offsetY,
                            int stepX,
                            int stepY,
                            int width,
                            int height,
                            int[] data) throws IOException {
        try {
            offsetX += scanLineLayout.leadingPixelCount;
            if (flipped) {
                offsetY = sceneRasterHeight - offsetY - height;
            }

            final int[] start = new int[]{bandIndex, offsetY, offsetX};
            final int[] stride = new int[]{1, stepY, stepX};
            final int[] count = new int[]{1, height, width};

            Section section = new Section(start, count, stride);
            Array array = rciImageSds.read(section);
            final Object storage = array.getStorage();
            System.arraycopy(storage, 0, data, 0, data.length);
            if (flipped) {
                flipImage(data, width);
            }
        } catch (InvalidRangeException e) {
            final IOException ioe = new IOException(
                    MessageFormat.format("Failed to read data from band #{0} of ''{1}''", bandIndex + 1,
                                         rciImageSds.getFullName()));
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void readMaskData(int bandIndex,
                             int offsetX,
                             int offsetY,
                             int stepX,
                             int stepY,
                             int width,
                             int height,
                             short[] mask) throws IOException {

        try {
            offsetX += scanLineLayout.leadingPixelCount;
            if (flipped) {
                offsetY = sceneRasterHeight - offsetY - height;
            }

            final int[] start = new int[]{bandIndex, offsetY, offsetX};
            final int[] stride = new int[]{1, stepY, stepX};
            final int[] count = new int[]{1, height, width};

            Section section = new Section(start, count, stride);
            Array array = maskSds.read(section);
            final Object storage = array.getStorage();
            System.arraycopy(storage, 0, mask, 0, mask.length);
            if (flipped) {
                flipImage(mask, width);
            }
        } catch (InvalidRangeException e) {
            final IOException ioe = new IOException(
                    "Failed to read data from band #" + (bandIndex + 1) + " of '" + maskSds.getFullName() + "'");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private void flipImage(int[] data, int width) {
        final int[] temp = new int[width];
        final int lastRowOffset = data.length - width;

        for (int i = 0; i < data.length / 2; i += width) {
            System.arraycopy(data, i, temp, 0, width);
            System.arraycopy(data, lastRowOffset - i, data, i, width);
            System.arraycopy(temp, 0, data, lastRowOffset - i, width);
        }
    }

    private void flipImage(short[] data, int width) {
        final short[] temp = new short[width];
        final int lastRowOffset = data.length - width;

        for (int i = 0; i < data.length / 2; i += width) {
            System.arraycopy(data, i, temp, 0, width);
            System.arraycopy(data, lastRowOffset - i, data, i, width);
            System.arraycopy(temp, 0, data, lastRowOffset - i, width);
        }
    }

    public String[] getGlobalAttributeNames() {
        Set<String> names = globalAttributes.keySet();
        return names.toArray(new String[names.size()]);
    }

    public String getGlobalAttribute(String key) {
        return globalAttributes.get(key);
    }

    public String getGlobalAttribute(String key, String defaultValue) {
        final String value = getGlobalAttribute(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public int getGlobalAttribute(String key, int defaultValue) {
        try {
            return Integer.parseInt(getGlobalAttribute(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float[][] readModeInfo(NetcdfFile ncFile) throws IOException {
        Variable modeInfoVar = ncFile.getRootGroup().findVariable(ChrisConstants.VS_NAME_MODE_INFO);
        if (modeInfoVar instanceof Structure) {
            Structure modeInfoStruct = (Structure) modeInfoVar;
            int numRecords = modeInfoStruct.getDimension(0).getLength();
            String[] fieldNames = ChrisConstants.VS_NAME_MODE_FIELDS;
            Variable[] fieldVariables = new Variable[fieldNames.length];
            for (int i = 0; i < fieldVariables.length; i++) {
                fieldVariables[i] = modeInfoStruct.findVariable(fieldNames[i]);
                if (fieldVariables[i] == null) {
                    throw new IOException("Failed to read 'Mode Info' Structure.");
                }
            }
            float[][] modeInfo = new float[numRecords][fieldNames.length];
            for (int i = 0; i < fieldNames.length; i++) {
                Array array = fieldVariables[i].read();
                for (int j = 0; j < numRecords; j++) {
                    modeInfo[j][i] = array.getFloat(j);
                }
            }
            return modeInfo;
        }
        throw new IOException("Failed to read 'Mode Info' Structure.");
    }

    private static Map<Integer, Float> readGainInfo(NetcdfFile ncFile) throws IOException {
        Variable gainInfoVar = ncFile.getRootGroup().findVariable(ChrisConstants.VS_NAME_GAIN_INFO);
        if (gainInfoVar instanceof Structure) {
            Structure gainInfoStruct = (Structure) gainInfoVar;
            int recordCount = gainInfoStruct.getDimension(0).getLength();
            Variable gainSetting = gainInfoStruct.findVariable(ChrisConstants.VS_NAME_GAIN_SETTING);
            Variable gainValue = gainInfoStruct.findVariable(ChrisConstants.VS_NAME_GAIN_VALUE);
            if (gainSetting != null && gainValue != null && recordCount > 0) {
                final Map<Integer, Float> gainInfoMap = new HashMap<Integer, Float>(recordCount);
                Array settingsArray = gainSetting.read();
                Array valuesArray = gainValue.read();
                for (int i = 0; i < recordCount; i++) {
                    gainInfoMap.put(settingsArray.getInt(i), valuesArray.getFloat(i));
                }
                return gainInfoMap;
            }
        }
        throw new IOException("Failed to read 'Gain Info' Structure.");
    }

    private static Map<String, String> readGlobalAttributes(NetcdfFile ncFile) {
        List<Attribute> globalNcAttributes = ncFile.getGlobalAttributes();
        Map<String, String> globalAttributes = new TreeMap<String, String>();
        for (Attribute attribute : globalNcAttributes) {
            globalAttributes.put(attribute.getShortName().trim(), attribute.getStringValue().trim());
        }
        return globalAttributes;
    }

    private static Variable getVariable(NetcdfFile ncFile, String sdsName, boolean require) throws IOException {
        try {
            Variable variable = ncFile.getRootGroup().findVariable(sdsName);
            if (variable == null) {
                if (require) {
                    throw new IOException(MessageFormat.format("Missing dataset ''{0}''", sdsName));
                } else {
                    return null;
                }
            }
            return variable;
        } catch (IOException e) {
            final IOException ioe = new IOException(
                    MessageFormat.format("Failed to access dataset ''{0}''", sdsName));
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static Variable getRciSds(NetcdfFile ncFile) throws IOException {
        final Variable sds = getVariable(ncFile, ChrisConstants.SDS_NAME_RCI_IMAGE, true);

        if (sds.getDimensions().size() != 3) {
            throw new IOException("Wrong number of dimensions, expected 3");
        }
        if (sds.getDataType() != DataType.INT) {
            throw new IOException("Wrong data type, 32-bit integer expected");
        }
        return sds;
    }

    private static Variable getMaskSds(NetcdfFile ncFile) throws IOException {
        final Variable sds = getVariable(ncFile, ChrisConstants.SDS_NAME_MASK, false);

        if (sds != null) {
            if (sds.getDimensions().size() != 3) {
                throw new IOException("Wrong number of dimensions, expected 3");
            }
            if (sds.getDataType() != DataType.SHORT) {
                throw new IOException("Wrong data type, 16-bit integer expected");
            }
        }
        return sds;
    }

    private void determineProcessingVersion() {
        final int numAttributes = globalAttributes.size();
        final String processingVersion;

        switch (numAttributes) {
        case 22:
            processingVersion = "3.0";
            break;
        case 23:
            processingVersion = "3.1";
            break;
        case 25:
            processingVersion = "4.0";
            break;
        case 26:
            processingVersion = "4.1";
            break;
        default:
            processingVersion = "unkown";
            break;
        }

        globalAttributes.put("Processing Version", processingVersion);
    }

    private void determineFlipping() {
        final String imageNumber = getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_NUMBER, "0");

        flipped = imageNumber.startsWith("2") || imageNumber.startsWith("3");
        if (flipped) {
            globalAttributes.put(ChrisConstants.ATTR_NAME_IMAGE_FLIPPED_ALONG_TRACK, "Yes");
        }
    }

    private void determineSceneRasterHeight() {
        final int mphNumLines = getGlobalAttribute(ChrisConstants.ATTR_NAME_NUMBER_OF_GROUND_LINES, Integer.MAX_VALUE);
        final int sdsNumLines = rciImageSds.getDimension(1).getLength();

        sceneRasterHeight = Math.min(mphNumLines, sdsNumLines);

        globalAttributes.put(ChrisConstants.ATTR_NAME_NUMBER_OF_GROUND_LINES, Integer.toString(sceneRasterHeight));
    }

    private void determineScanLineLayout() {
        final String mode = getGlobalAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE).substring(0, 1);

        if (!(scanLineLayoutMap == null || mode == null || scanLineLayoutMap.get(mode) == null)) {
            scanLineLayout = scanLineLayoutMap.get(mode);
        } else {
            scanLineLayout = new ScanLineLayout(0, rciImageSds.getDimension(2).getLength(), 0);
        }

        globalAttributes.put(ChrisConstants.ATTR_NAME_NUMBER_OF_SAMPLES,
                             Integer.toString(scanLineLayout.imagePixelCount));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ScanLineLayout> readScanLineLayoutMap() throws IOException {
        final InputStream inputStream = ChrisFile.class.getResourceAsStream("scanLineLayout.csv");
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final CsvReader csvReader = new CsvReader(inputStreamReader, new char[]{','}, true, "#");

        try {
            final List<String[]> recordList = csvReader.readStringRecords();
            final Map<String, ScanLineLayout> scanLineLayoutMap = new HashMap<String, ScanLineLayout>(
                    recordList.size());

            for (final String[] record : recordList) {
                final int leadingPixelCount = Integer.parseInt(record[1]);
                final int imagePixelCount = Integer.parseInt(record[2]);
                final int trailingPixelCount = Integer.parseInt(record[3]);

                scanLineLayoutMap.put(record[0],
                                      new ScanLineLayout(leadingPixelCount, imagePixelCount, trailingPixelCount));
            }
            return scanLineLayoutMap;
        } finally {
            try {
                csvReader.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
