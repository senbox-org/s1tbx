package org.esa.beam.dataio.chris;

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.util.io.CsvReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ChrisFile {

    private File file;
    private int fileId = HDFConstants.FAIL;
    private int sdId = HDFConstants.FAIL;
    private Sds rciImageSds;
    private Sds maskSds;
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
        if (fileId != HDFConstants.FAIL) {
            throw new IllegalStateException("already open");
        }

        try {
            fileId = HDF4Lib.Hopen(file.getPath(), HDFConstants.DFACC_RDONLY);
            sdId = HDF4Lib.SDstart(file.getPath(), HDFConstants.DFACC_RDONLY);
            HDF4Lib.Vstart(fileId);
            globalAttributes = readGlobalAttributes(sdId);
            rciImageSds = openRciSds(sdId);
            maskSds = openMaskSds(sdId);
            modeInfo = readModeInfo(fileId);
            gainInfoMap = readGainInfo(fileId);
        } catch (HDFException e) {
            try {
                close();
            } catch (IOException ignored) {
                // ignore
            }
            final IOException ioe = new IOException(MessageFormat.format("Failed to open CHRIS file ''{0}''", file));
            ioe.initCause(e);

            throw ioe;
        }

        determineProcessingVersion();
        determineSceneRasterHeight();
        determineFlipping();
        determineScanLineLayout();
    }

    public void close() throws IOException {
        if (fileId == HDFConstants.FAIL) {
            return;
        }
        try {
            if (globalAttributes != null) {
                globalAttributes.clear();
            }
            if (rciImageSds != null) {
                HDF4Lib.SDendaccess(rciImageSds.sdsId);
            }
            if (maskSds != null) {
                HDF4Lib.SDendaccess(maskSds.sdsId);
            }
            HDF4Lib.SDend(this.sdId);
            HDF4Lib.Vend(this.fileId);
            HDF4Lib.Hclose(this.fileId);
        } catch (HDFException e) {
            final IOException ioe = new IOException(MessageFormat.format("Failed to close CHRIS file ''{0}''", file));
            ioe.initCause(e);
            throw ioe;
        } finally {
            rciImageSds = null;
            maskSds = null;
            modeInfo = null;
            sdId = HDFConstants.FAIL;
            fileId = HDFConstants.FAIL;
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
        return rciImageSds.dimSizes[0];
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

            HDF4Lib.SDreaddata(rciImageSds.sdsId, start, stride, count, data);
            if (flipped) {
                flipImage(data, width);
            }
        } catch (HDFException e) {
            final IOException ioe = new IOException(
                    MessageFormat.format("Failed to read data from band #{0} of ''{1}''", bandIndex + 1,
                                         rciImageSds.name));
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

            HDF4Lib.SDreaddata(maskSds.sdsId, start, stride, count, mask);
            if (flipped) {
                flipImage(mask, width);
            }
        } catch (HDFException e) {
            final IOException ioe = new IOException(
                    "Failed to read data from band #" + (bandIndex + 1) + " of '" + maskSds.name + "'");
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

    private static float[][] readModeInfo(int fileId) throws HDFException {
        int refNo = HDF4Lib.VSfind(fileId, ChrisConstants.VS_NAME_MODE_INFO);
        if (refNo == 0) {
            return new float[0][];
        }
        int vdataId = HDF4Lib.VSattach(fileId, refNo, "r");
        try {
            int[] numRecordsBuffer = new int[1];
            HDF4Lib.VSQuerycount(vdataId, numRecordsBuffer);
            int numRecords = numRecordsBuffer[0];

            float[][] modeInfo = new float[numRecords][7];
            HDF4Lib.VSsetfields(vdataId, ChrisConstants.VS_NAME_MODE_FIELDS);
            for (int i = 0; i < numRecords; i++) {
                HDF4Lib.VSseek(vdataId, i);
                HDF4Lib.VSread(vdataId, modeInfo[i]);
            }

            return modeInfo;
        } finally {
            HDFLibrary.VSdetach(vdataId);
        }
    }

    private static Map<Integer, Float> readGainInfo(int fileId) throws HDFException {
        final int refNo = HDF4Lib.VSfind(fileId, ChrisConstants.VS_NAME_GAIN_INFO);
        if (refNo == 0) {
            return new HashMap<Integer, Float>();
        }

        final int vdataId = HDF4Lib.VSattach(fileId, refNo, "r");
        try {
            final int[] recordCountBuffer = new int[1];
            HDF4Lib.VSQuerycount(vdataId, recordCountBuffer);
            final int recordCount = recordCountBuffer[0];

            final Map<Integer, Float> gainInfoMap = new HashMap<Integer, Float>(recordCount);
            final float[] record = new float[2];

            HDF4Lib.VSsetfields(vdataId, ChrisConstants.VS_NAME_GAIN_FIELDS);
            for (int i = 0; i < recordCount; i++) {
                HDF4Lib.VSseek(vdataId, i);
                HDF4Lib.VSread(vdataId, record);
                gainInfoMap.put((int) record[0], record[1]);
            }

            return gainInfoMap;
        } finally {
            HDFLibrary.VSdetach(vdataId);
        }
    }

    private static Map<String, String> readGlobalAttributes(int sdId) throws IOException {
        try {
            final int[] fileInfo = new int[16];
            HDF4Lib.SDfileinfo(sdId, fileInfo);
            Map<String, String> globalAttributes = new TreeMap<String, String>();
            int numAttributes = fileInfo[1];
            for (int i = 0; i < numAttributes; i++) {
                try {
                    collectAttribute(sdId, i, globalAttributes);
                } catch (HDFException e) {
                    // todo - collect warning here
                }
            }
            return globalAttributes;
        } catch (HDFException e) {
            final IOException ioe = new IOException("Failed to access HDF global attributes");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static void collectAttribute(int sdId, int index, Map<String, String> globalAttributes) throws
                                                                                                    HDFException {
        final String[] nameBuffer = new String[]{createEmptyString(256)};
        final int[] attributeInfo = new int[16];
        HDF4Lib.SDattrinfo(sdId, index, nameBuffer, attributeInfo);

        final String name = nameBuffer[0];
        int numberType = attributeInfo[0];
        int arrayLength = attributeInfo[1];

        if (numberType == HDFConstants.DFNT_CHAR || numberType == HDFConstants.DFNT_UCHAR8) {
            byte[] data = new byte[arrayLength];
            HDF4Lib.SDreadattr(sdId, index, data);
            globalAttributes.put(name.trim(), new String(data).trim());
        }
    }

    private static String createEmptyString(int n) {
        char[] a = new char[n];
        Arrays.fill(a, ' ');
        return new String(a);
    }

    private static Sds openSds(int sdId, String sdsName, boolean require) throws IOException {
        try {
            int sdsIdx = HDF4Lib.SDnametoindex(sdId, sdsName);
            if (sdsIdx == HDFConstants.FAIL) {
                if (require) {
                    throw new IOException(MessageFormat.format("Missing HDF dataset ''{0}''", sdsName));
                } else {
                    return null;
                }
            }

            int sdsId = HDF4Lib.SDselect(sdId, sdsIdx);

            String[] nameBuffer = new String[]{createEmptyString(256)};
            int[] dimSizes = new int[16];
            int[] sdsInfo = new int[16];
            HDF4Lib.SDgetinfo(sdsId, nameBuffer, dimSizes, sdsInfo);

            int numDims = sdsInfo[0];
            int dataType = sdsInfo[1];
            int[] dimSizesCopy = new int[numDims];
            System.arraycopy(dimSizes, 0, dimSizesCopy, 0, numDims);

            return new Sds(sdsName, sdsId, dataType, dimSizesCopy);
        } catch (HDFException e) {
            final IOException ioe = new IOException(
                    MessageFormat.format("Failed to access HDF dataset ''{0}''", sdsName));
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static Sds openRciSds(int sdId) throws IOException {
        final Sds sds = openSds(sdId, ChrisConstants.SDS_NAME_RCI_IMAGE, true);

        if (sds.dimSizes.length != 3) {
            throw new IOException("Wrong number of dimensions, expected 3");
        }
        if (sds.dataType != HDFConstants.DFNT_INT32) {
            throw new IOException("Wrong data type, 32-bit integer expected");
        }

        return sds;
    }

    private static Sds openMaskSds(int sdId) throws IOException {
        final Sds sds = openSds(sdId, ChrisConstants.SDS_NAME_MASK, false);

        if (sds != null) {
            if (sds.dimSizes.length != 3) {
                throw new IOException("Wrong number of dimensions, expected 3");
            }
            if (sds.dataType != HDFConstants.DFNT_INT16) {
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
        final int sdsNumLines = rciImageSds.dimSizes[1];

        sceneRasterHeight = Math.min(mphNumLines, sdsNumLines);

        globalAttributes.put(ChrisConstants.ATTR_NAME_NUMBER_OF_GROUND_LINES, Integer.toString(sceneRasterHeight));
    }

    private void determineScanLineLayout() {
        final String mode = getGlobalAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE).substring(0, 1);

        if (!(scanLineLayoutMap == null || mode == null || scanLineLayoutMap.get(mode) == null)) {
            scanLineLayout = scanLineLayoutMap.get(mode);
        } else {
            scanLineLayout = new ScanLineLayout(0, rciImageSds.dimSizes[2], 0);
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


    private static class Sds {

        final String name;
        final int sdsId;
        final int dataType;
        final int[] dimSizes;

        public Sds(String name, int sdsId, int dataType, int[] dimSizes) {
            this.name = name;
            this.sdsId = sdsId;
            this.dataType = dataType;
            this.dimSizes = dimSizes;
        }

    }

}
