package org.esa.snap.core.datamodel;

import java.util.Map;
import java.util.TreeMap;
import javax.media.jai.UnpackedImageData;
import org.esa.snap.core.util.math.DoubleList;

public class QualitativeStxOp extends StxOp {

    public final static String NO_MAJORITY_CLASS = "";

    private final Map<String, Integer> membersPerClass;
    private int totalNumClassMembers;
    private String majorityClass;
    private String secondMajorityClass;
    private ClassCounter classCounter;

    public QualitativeStxOp() {
        super("Qualitative");
        membersPerClass = new TreeMap<>();
        totalNumClassMembers = 0;
        this.majorityClass = NO_MAJORITY_CLASS;
        this.secondMajorityClass = NO_MAJORITY_CLASS;
    }

    public int getNumberOfMembers(String className) {
        if (membersPerClass.containsKey(className)) {
            return membersPerClass.get(className);
        }
        return 0;
    }

    public String getMajorityClass() {
        if (majorityClass.equals(NO_MAJORITY_CLASS)) {
            determineMajorityClass();
        }
        return majorityClass;
    }

    public String getSecondMajorityClass() {
        if (majorityClass.equals(NO_MAJORITY_CLASS)) {
            determineMajorityClass();
        }
        return secondMajorityClass;
    }

    private void determineMajorityClass() {
        int maxNumClassMembers = 0;
        int secondMaxNumClassMembers = 0;
        for (Map.Entry<String, Integer> entry : membersPerClass.entrySet()) {
            if (entry.getValue() > maxNumClassMembers) {
                secondMajorityClass = majorityClass;
                secondMaxNumClassMembers = maxNumClassMembers;
                majorityClass = entry.getKey();
                maxNumClassMembers = entry.getValue();
            } else if (entry.getValue() > secondMaxNumClassMembers) {
                secondMajorityClass = entry.getKey();
                secondMaxNumClassMembers = entry.getValue();
            }
        }
    }

    public int getTotalNumClassMembers() {
        return totalNumClassMembers;
    }

    public String[] getClassNames() {
        return membersPerClass.keySet().toArray(new String[0]);
    }

    public void determineClassCounterType(Band band) {
        if (band.isIndexBand()) {
            classCounter = new IndexCodingClassCounter(band.getIndexCoding());
        } else if (band.isFlagBand()) {
            classCounter = new FlagCodingClassCounter(band.getFlagCoding());
        } else {
            classCounter = new DefaultClassCounter();
        }
    }

    @Override
    public void accumulateData(UnpackedImageData dataPixels,
                               UnpackedImageData maskPixels) {

        // Do not change this code block without doing the same changes in HistogramStxOp.java and SummaryStxOp.java
        // {{ Block Start

        final DoubleList values = asDoubleList(dataPixels);

        final int dataPixelStride = dataPixels.pixelStride;
        final int dataLineStride = dataPixels.lineStride;
        final int dataBandOffset = dataPixels.bandOffsets[0];

        byte[] mask = null;
        int maskPixelStride = 0;
        int maskLineStride = 0;
        int maskBandOffset = 0;
        if (maskPixels != null) {
            mask = maskPixels.getByteData(0);
            maskPixelStride = maskPixels.pixelStride;
            maskLineStride = maskPixels.lineStride;
            maskBandOffset = maskPixels.bandOffsets[0];
        }

        final int width = dataPixels.rect.width;
        final int height = dataPixels.rect.height;

        int dataLineOffset = dataBandOffset;
        int maskLineOffset = maskBandOffset;

        // }} Block End

        for (int y = 0; y < height; y++) {
            int dataPixelOffset = dataLineOffset;
            int maskPixelOffset = maskLineOffset;
            for (int x = 0; x < width; x++) {
                if (mask == null || mask[maskPixelOffset] != 0) {
                    int value = (int) values.getDouble(dataPixelOffset);
                    classCounter.count(value);
                    totalNumClassMembers++;
                }
                dataPixelOffset += dataPixelStride;
                maskPixelOffset += maskPixelStride;
            }
            dataLineOffset += dataLineStride;
            maskLineOffset += maskLineStride;
        }
    }

    private void putIntValue(int value) {
        String key = Integer.toString(value);
        if (!membersPerClass.containsKey(key)) {
            membersPerClass.put(key, 1);
        } else {
            membersPerClass.put(key, membersPerClass.get(key) + 1);
        }
    }

    private interface ClassCounter {

        void count(int value);

    }

    private class IndexCodingClassCounter implements ClassCounter {

        private final String[] indexNames;
        private final int[] indexValues;
        private final int numAttributes;

        IndexCodingClassCounter(IndexCoding indexCoding) {
            indexNames = indexCoding.getIndexNames();
            numAttributes = indexCoding.getNumAttributes();
            indexValues = new int[numAttributes];
            for (int i = 0; i < numAttributes; i++) {
                indexValues[i] = indexCoding.getIndexValue(indexNames[i]);
            }
        }

        @Override
        public void count(int value) {
            for (int i = 0; i < numAttributes; i++) {
                if (value == indexValues[i]) {
                    String indexName = indexNames[i];
                    if (!membersPerClass.containsKey(indexName)) {
                        membersPerClass.put(indexName, 1);
                    } else {
                        membersPerClass.put(indexName, membersPerClass.get(indexName) + 1);
                    }
                    return;
                }
            }
            putIntValue(value);
        }

    }

    private class FlagCodingClassCounter implements ClassCounter {
        private final FlagCoding flagCoding;
        private final String[] flagNames;
        private final int numAttributes;

        private final int[] flagValues;

        FlagCodingClassCounter(FlagCoding flagCoding) {
            this.flagCoding = flagCoding;
            flagNames = flagCoding.getFlagNames();
            numAttributes = flagCoding.getNumAttributes();
            flagValues = new int[numAttributes];
            for (int i = 0; i < numAttributes; i++) {
                flagValues[i] = flagCoding.getFlagMask(flagNames[i]);
            }
        }

        @Override
        public void count(int value) {
            for (int i = 0; i < numAttributes; i++) {
                if ((value & flagValues[i]) != 0) {
                    String flagName = flagNames[i];
                    if (!membersPerClass.containsKey(flagName)) {
                        membersPerClass.put(flagName, 1);
                    } else {
                        membersPerClass.put(flagName, membersPerClass.get(flagName) + 1);
                    }
                }
            }
        }

    }

    private class DefaultClassCounter implements ClassCounter {

        @Override
        public void count(int value) {
            putIntValue(value);
        }
    }

}
