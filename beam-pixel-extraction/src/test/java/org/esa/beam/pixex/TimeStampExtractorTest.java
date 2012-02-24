package org.esa.beam.pixex;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

public class TimeStampExtractorTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testExtractTimeStamps_1() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${date}*.dim");
        final ProductData.UTC[] timeStamps = extractor.extractTimeStamps("20111103_est_wac_wew_1200.dim");
        assertEquals(2, timeStamps.length);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(), timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_2() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", ".*_${date}_*.dim");
        ProductData.UTC[] timeStamps = extractor.extractTimeStamps("leading_characters_20111103_est_wac_wew_1200.dim");
        assertEquals(timeStamps.length, 2);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(), timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_3() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${date}_${date}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("20110917_20110923_bas_wac_acr_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-09-17", "yyyy-MM-dd").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2011-09-23", "yyyy-MM-dd").getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_4() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMM", "${date}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("201106_bas_wac_acr_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-06", "yyyy-MM").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_5() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd_hh_mm_ss", "*${date}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("something20110601_12_53_10_bas_wac_acr_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-06-01-12-53-10", "yyyy-MM-dd-hh-mm-ss").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test(expected = ValidationException.class)
    public void testExtractTimeStamps_badFilename() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${date}*.dim");
        extractor.extractTimeStamps("something20110603.dim");
    }

    @Test
    public void testDateInterpretationPatternValidator() throws Exception {
        final TimeStampExtractor.DateInterpretationPatternValidator validator = new TimeStampExtractor.DateInterpretationPatternValidator();
        validator.validateValue(null, "yyyy");
        validator.validateValue(null, "yyyyMM");
        validator.validateValue(null, "yyyyMMdd");
        validator.validateValue(null, "yyyyMMdd_hh:mm:ss");
        try {
            validator.validateValue(null, "yyy");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "yyyyMMX");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "yyMMdd:hh");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "yyyyMMddyyyy");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
    }

    @Test
    public void testFilenameInterpretationPatternValidator() throws Exception {
        final TimeStampExtractor.FilenameInterpretationPatternValidator validator = new TimeStampExtractor.FilenameInterpretationPatternValidator();
        validator.validateValue(null, "MER_${date}.nc");
        validator.validateValue(null, "*${date}.nc");
        validator.validateValue(null, "?${date}.nc");
        try {
            validator.validateValue(null, "MER_${date}_${date}_${date}.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "MER.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "MER\\_${date}.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "MER\\_${date}_${date}.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
    }
}
