package org.esa.beam.timeseries.ui.graph;

import com.bc.jexp.ParseException;
import org.esa.beam.timeseries.core.timeseries.datamodel.AxisMapping;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import org.junit.*;

import static org.junit.Assert.*;

public class TimeSeriesValidatorTest {

    private final TimeSeriesDataItem ITEM_NAN = new TimeSeriesDataItem(Day.parseDay("2012-01-11"), Double.NaN);
    private final TimeSeriesDataItem ITEM_0 = new TimeSeriesDataItem(Day.parseDay("2012-01-12"), 0);
    private final TimeSeriesDataItem ITEM_3 = new TimeSeriesDataItem(Day.parseDay("2012-01-13"), 3);
    private final TimeSeriesDataItem ITEM_4 = new TimeSeriesDataItem(Day.parseDay("2012-01-14."), 4);
    private final TimeSeriesDataItem ITEM_7 = new TimeSeriesDataItem(Day.parseDay("2012-01-15"), 7);
    private final TimeSeriesDataItem ITEM_24_5 = new TimeSeriesDataItem(Day.parseDay("2012-01-16"), 24.5);

    private TimeSeriesValidator validator;
    private AxisMapping mapping;

    @Before
    public void setUp() throws Exception {
        validator = new TimeSeriesValidator();
        mapping = new AxisMapping();
        mapping.addRasterName("alias1", "raster1");
        mapping.addRasterName("alias2", "raster2");
        mapping.addRasterName("alias1", "raster3");
        mapping.addInsituName("alias1", "insitu1");
        mapping.addInsituName("alias1", "insitu2");
        mapping.addInsituName("alias2", "insitu3");
        validator.adaptTo("key1", mapping);
    }

    @Test
    public void testValidateWithDefaultExpression() throws Exception {
        final TimeSeries unvalidatedTimeSeries = new TimeSeries("insitu1");
        unvalidatedTimeSeries.add(ITEM_NAN);
        unvalidatedTimeSeries.add(ITEM_0);
        unvalidatedTimeSeries.add(ITEM_24_5);

        final TimeSeries validatedTimeSeries = validator.validate(unvalidatedTimeSeries, "insitu1", TimeSeriesType.INSITU);

        assertSame(unvalidatedTimeSeries, validatedTimeSeries);
        assertEquals(3, validatedTimeSeries.getItemCount());
        assertEquals(ITEM_NAN, validatedTimeSeries.getDataItem(0));
        assertEquals(ITEM_0, validatedTimeSeries.getDataItem(1));
        assertEquals(ITEM_24_5, validatedTimeSeries.getDataItem(2));
    }

    @Test
    public void testValidateWithExpression() throws Exception {
        final TimeSeries unvalidatedTimeSeries = new TimeSeries("insitu1");
        unvalidatedTimeSeries.add(ITEM_NAN);
        unvalidatedTimeSeries.add(ITEM_0);
        unvalidatedTimeSeries.add(ITEM_24_5);

        assertTrue(validator.setExpression("i.insitu1", "i.insitu1 > 0"));

        final TimeSeries validatedTimeSeries = validator.validate(unvalidatedTimeSeries, "insitu1", TimeSeriesType.INSITU);

        assertNotSame(unvalidatedTimeSeries, validatedTimeSeries);
        assertEquals(1, validatedTimeSeries.getItemCount());
        assertEquals(ITEM_24_5, validatedTimeSeries.getDataItem(0));
    }


    @Test
    public void testValidateWithWrongType() throws Exception {
        final TimeSeries unvalidatedTimeSeries = new TimeSeries("raster1");

        assertTrue(validator.setExpression("r.raster1", "r.raster1 > 5"));

        try {
            validator.validate(unvalidatedTimeSeries, "raster1", TimeSeriesType.INSITU);
            fail("ParseException expected");
        } catch (ParseException e) {
            assertEquals("No variable for identifier 'i.raster1' registered.", e.getMessage());
        }
    }

    @Test
    public void testValidate() throws Exception {
        final TimeSeries unvalidatedTimeSeries = new TimeSeries("raster1");
        unvalidatedTimeSeries.add(ITEM_0);
        unvalidatedTimeSeries.add(ITEM_4);
        unvalidatedTimeSeries.add(ITEM_7);
        unvalidatedTimeSeries.add(ITEM_24_5);

        assertTrue(validator.setExpression("r.raster1", "r.raster1 > 5"));

        final TimeSeries validated = validator.validate(unvalidatedTimeSeries, "raster1", TimeSeriesType.PIN);

        assertNotSame(unvalidatedTimeSeries, validated);
        assertEquals(2, validated.getItemCount());
        assertEquals(ITEM_7, validated.getDataItem(0));
        assertEquals(ITEM_24_5, validated.getDataItem(1));
    }

    @Test()
    public void testTryToSetInvalidExpression() throws Exception {
        assertFalse(validator.setExpression("r.raster1", "raster1 groesserAls 5"));
    }

    @Test
    public void testThatValidatorIsCorrectlyInitialized() throws Exception {
        assertTrue(validator.setExpression("r.raster1", "r.raster1 > 5"));
        assertFalse(validator.setExpression("r.raster4", "r.raster4 > 5"));
    }

    @Test
    public void testThatOnlyBooleanExpressionsCanBeSet() throws Exception {
        assertTrue(validator.setExpression("r.raster1", "r.raster1 > 5"));
        assertFalse(validator.setExpression("r.raster1", "r.raster1 + r.raster1"));
    }

    @Test
    public void testRepeatedAdapting() throws Exception {
        TimeSeries validated;
        final TimeSeries series = new TimeSeries("raster1");
        series.add(ITEM_0);
        series.add(ITEM_3);
        series.add(ITEM_4);
        series.add(ITEM_7);

        assertTrue(validator.setExpression("r.raster1", "r.raster1 > 3"));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(2, validated.getItemCount());

        validator.adaptTo("key2", new AxisMapping());
        assertFalse(validator.setExpression("r.raster1", "r.raster1 > 3"));

        try {
            validator.validate(series, "raster1", TimeSeriesType.CURSOR);
            fail();
        } catch (ParseException expected) {
            assertEquals("No variable for identifier 'r.raster1' registered.", expected.getMessage());
        }

        validator.adaptTo("key1", mapping);

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(2, validated.getItemCount());

        assertTrue(validator.setExpression("r.raster1", "r.raster1 < 5"));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(3, validated.getItemCount());
    }

    @Test
    public void testInvalidIfExpressionEqualsSourceName() throws Exception {
        assertFalse(validator.setExpression("r.raster1", "r.raster1"));
    }

    @Test
    public void testEmptyExpression() throws Exception {
        TimeSeries validated;
        final TimeSeries series = new TimeSeries("raster1");
        series.add(ITEM_3);
        series.add(ITEM_4);

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(2, validated.getItemCount());

        assertTrue(validator.setExpression("r.raster1", "r.raster1 < 4"));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(1, validated.getItemCount());

        assertTrue(validator.setExpression("r.raster1", ""));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(2, validated.getItemCount());

        assertTrue(validator.setExpression("r.raster1", "r.raster1 < 4"));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(1, validated.getItemCount());

        assertTrue(validator.setExpression("r.raster1", "true"));

        validated = validator.validate(series, "raster1", TimeSeriesType.CURSOR);
        assertEquals(2, validated.getItemCount());
    }
}
