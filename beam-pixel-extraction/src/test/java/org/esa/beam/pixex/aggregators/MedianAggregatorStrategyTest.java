package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.DefaultRecord;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MedianAggregatorStrategyTest {

    private MedianAggregatorStrategy strategy;

    @Before
    public void setUp() throws Exception {
        strategy = new MedianAggregatorStrategy();
    }

    @Test
    public void testGetValueForOddNumberOfIntegerBandValues() {
        Number[] oddNumberOfIntegerBandValues = new Integer[]{1, 1, 2, 3, 5, 8, 13, 21, 34};
        assertEquals(5F, strategy.getMedian(oddNumberOfIntegerBandValues), 0.0001);
    }

    @Test
    public void testGetValueForEvenNumberOfIntegerBandValues() {
        Number[] evenNumberOfIntegerBandValues = new Integer[]{1, 2, 3, 5, 8, 13};
        assertEquals(4F, strategy.getMedian(evenNumberOfIntegerBandValues), 0.0001);
    }

    @Test
    public void testGetValueForOddNumberOfFloatBandValues() {
        Number[] oddNumberOfFloatBandValues = new Float[]{2.5f, 3.5f, 5.5f, 8.5f, 13.5f, 21.5f, 34.5f};
        assertEquals(8.5F, strategy.getMedian(oddNumberOfFloatBandValues), 0.0001);
    }

    @Test
    public void testGetValueForEvenNumberOfFloatBandValues() {
        Number[] evenNumberOfFloatBandValues = new Float[]{3.5f, 5.5f, 8.5f, 13.5f};
        assertEquals(7F, strategy.getMedian(evenNumberOfFloatBandValues), 0.0001);
    }

    @Test
    public void testGetValuesForOneValue() {
        Number[] evenNumberOfFloatBandValues = new Float[]{3.5f};
        assertEquals(3.5F, strategy.getMedian(evenNumberOfFloatBandValues), 0.0001);
    }

    @Test
    public void testGetValues() throws Exception {
        final Float[] valuesForBand1 = {
                1F, 2F, 3F, 4F, 5F, 6F
        };

        final Number[][] numbers = new Number[1][];
        numbers[0] = valuesForBand1;

        final DefaultRecord record = new DefaultRecord(numbers);

        assertEquals(1, strategy.getValues(record, 0).length);
        assertEquals(3.5F, strategy.getValues(record, 0)[0], 0.0001);
    }

    @Test
    public void testGetValueCount() throws Exception {
        assertEquals(1, strategy.getValueCount());
    }

    @Test
    public void testGetSuffixes() throws Exception {
        assertEquals(1, strategy.getSuffixes().length);
        assertEquals("median", strategy.getSuffixes()[0]);
    }
}
