package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.AggregatedNumber;
import org.esa.snap.pixex.calvalus.ma.DefaultRecord;
import org.esa.snap.pixex.calvalus.ma.Record;
import org.esa.snap.pixex.calvalus.ma.RecordAggregator;
import org.esa.snap.pixex.calvalus.ma.RecordTransformer;

abstract class AbstractAggregatorStrategy implements AggregatorStrategy {

    static final String NUM_PIXELS_SUFFIX = "num_pixels";

    @Override
    public int getValueCount() {
        return getSuffixes().length;
    }

    protected AggregatedNumber getAggregatedNumber(Record record, int rasterIndex) {
        final RecordTransformer recordAggregator = new RecordAggregator(-1, -1.0);
        final Number[][] attributeValues = (Number[][]) record.getAttributeValues();

        Object[] newAttributeValues = new Object[attributeValues.length];

        for (int i = 0; i < attributeValues.length; i++) {
            Number[] attributeValue = attributeValues[i];
            if (attributeValue instanceof Float[]) {
                newAttributeValues[i] = toPrimitiveArray((Float[]) attributeValue);
            } else {
                newAttributeValues[i] = toPrimitiveArray((Integer[]) attributeValue);
            }
        }

        final DefaultRecord defaultRecord = new DefaultRecord(record.getLocation(), record.getTime(),
                                                              newAttributeValues);
        final Record transformedRecord = recordAggregator.transform(defaultRecord);
        return (AggregatedNumber) transformedRecord.getAttributeValues()[rasterIndex];
    }

    private int[] toPrimitiveArray(Integer[] attributeValue) {
        final int[] result = new int[attributeValue.length];
        for (int i = 0; i < attributeValue.length; i++) {
            result[i] = attributeValue[i];
        }
        return result;
    }

    private float[] toPrimitiveArray(Float[] attributeValue) {
        final float[] result = new float[attributeValue.length];
        for (int i = 0; i < attributeValue.length; i++) {
            result[i] = attributeValue[i];
        }
        return result;
    }

}
