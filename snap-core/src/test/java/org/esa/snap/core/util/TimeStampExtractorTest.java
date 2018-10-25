package org.esa.snap.core.util;

import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

public class TimeStampExtractorTest {

    @Test
    public void testInit() throws Exception {
        new TimeStampExtractor("yyyyMMdd", "*${startDate}*.dim");
        new TimeStampExtractor("yyyyMMdd", "*${startDate}*${endDate}.dim");
        new TimeStampExtractor("yyyyMMdd", "*${endDate}.dim");
        new TimeStampExtractor("yyyyMMMdd", "*${endDate}.dim");
        try {
            new TimeStampExtractor("yyyyMMdd", "something.dim");
            fail();
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void testExtractTimeStamps_1() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${startDate}*.dim");
        final ProductData.UTC[] timeStamps = extractor.extractTimeStamps("20111103_est_wac_wew_1200.dim");
        assertEquals(timeStamps.length, 2);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(),
                timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_2() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "*_${startDate}_*.dim");
        ProductData.UTC[] timeStamps = extractor.extractTimeStamps("leading_characters_20111103_est_wac_wew_1200.dim");
        assertEquals(2, timeStamps.length);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(),
                timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_3() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${startDate}_${endDate}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("20110917_20110923_bas_wac_acr_1200.dim");
        assertEquals(2, dateRange.length);
        assertEquals(ProductData.UTC.parse("2011-09-17", "yyyy-MM-dd").getAsDate().getTime(),
                dateRange[0].getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2011-09-23", "yyyy-MM-dd").getAsDate().getTime(),
                dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_4() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMM", "${startDate}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("201106_bas_wac_acr_1200.dim");
        assertEquals(2, dateRange.length);
        assertEquals(ProductData.UTC.parse("2011-06", "yyyy-MM").getAsDate().getTime(),
                dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_5_hh() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd_hh_mm_ss", "*${startDate}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("something20110601_12_53_10_bas_wac_acr_1200.dim");
        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());

        assertEquals("2011-06-01\t00:53:10", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }

    @Test
    public void testExtractTimeStamps_5_HH() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd_HH_mm_ss", "*${startDate}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("something20110601_12_53_10_bas_wac_acr_1200.dim");
        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());

        assertEquals("2011-06-01\t12:53:10", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }

    @Test
    public void testExtractTimeStamps_6() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "*${startDate}*.dim");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps(
                "something__20110601_bas_wac_acr_1200__20110602___something.dim");
        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
        assertEquals("2011-06-02\t00:00:00", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));
    }

    @Test
    public void testExtractTimeStamps_7() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd_hhmmss", "*${startDate}*");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("MER_RR__2CNACR20070123_101652_000000072055_00008_25613_0000.nc");

        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
        assertEquals("2007-01-23\t10:16:52", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }

    @Test
    public void testExtractTimeStamps_8() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd_hhmmss", "*${endDate}*");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("MER_RR__2CNACR20070123_101652_000000072055_00008_25613_0000.nc");

        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
        assertEquals("2007-01-23\t10:16:52", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }
    @Test
    public void testExtractTimeStamps_9() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMddTHHmmss", "*${startDate}_*_V*");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("S2A_OPER_PRD_MSIL1C_PDMC_20160429T163731_R093_V20150818T092006_20150818T092006.SAFE");

        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
        assertEquals("2016-04-29\t16:37:31", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }

    @Test
    public void testExtractTimeStamps_WithMonthAbreviation() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyy-MMM-dd_hhmmss", "*${endDate}*");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps(
                "MER_RR__2CNACR2012-Oct-23_101652_blahblah.nc");

        assertEquals(2, dateRange.length);
        assertEquals(ProductData.UTC.parse("2012-10-23:10:16:52", "yyyy-MM-dd:hh:mm:ss").getAsDate().getTime(),
                dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_withMonthNameToSkip() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMM", "LC8......${startDate}*.nc");
        final ProductData.UTC[] timeStamps = extractor.extractTimeStamps("LC81070352013NovLGN01_L2.nc");
        assertEquals(timeStamps.length, 2);
        assertEquals(ProductData.UTC.parse("2013-Nov", "yyyy-MMM").getAsDate().getTime(), timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test
    public void testExtractTimeStamps_withDOY() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyDDD", "A${startDate}.L3m_DAY_SST_4");
        ProductData.UTC[] dateRange = extractor.extractTimeStamps("A2010159.L3m_DAY_SST_4");
        assertEquals(2, dateRange.length);
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
        assertEquals("2010-06-08\t00:00:00", ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss").format(dateRange[0].getAsDate()));

    }

    @Test
    public void testExtractTimeStamps_withDoyAndNumbersToSkip() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyDDD", "LC8......${startDate}*.nc");
        final ProductData.UTC[] timeStamps = extractor.extractTimeStamps("LC81070352013132LGN01_L2.nc");
        assertEquals(timeStamps.length, 2);
        assertEquals(ProductData.UTC.parse("2013-05-12", "yyyy-MM-dd").getAsDate().getTime(), timeStamps[0].getAsDate().getTime());
        assertEquals(timeStamps[0].getAsDate().getTime(), timeStamps[1].getAsDate().getTime());
    }

    @Test(expected = ValidationException.class)
    public void testExtractTimeStamps_badFilename() throws ParseException, ValidationException {
        final TimeStampExtractor extractor = new TimeStampExtractor("yyyyMMdd", "${startDate}*.dim");
        extractor.extractTimeStamps("something20110603.dim");
    }

    @Test
    public void testDateInterpretationPatternValidator() throws Exception {
        final TimeStampExtractor.DateInterpretationPatternValidator validator = new TimeStampExtractor.DateInterpretationPatternValidator();
        validator.validateValue(null, "yyyy");
        validator.validateValue(null, "yyyyMM");
        validator.validateValue(null, "yyyyMMM");
        validator.validateValue(null, "yyyyMMdd");
        validator.validateValue(null, "yyyyMMMdd");
        validator.validateValue(null, "yyyyMMdd_hh:mm:ss");
        validator.validateValue(null, "yyyyMMMdd_hh:mm:ss");
        validator.validateValue(null, "yyyyMMMdd_HH:mm:ss");
        validator.validateValue(null, "yyyyDDD");

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
        validator.validateValue(null, "MER_${startDate}.nc");
        validator.validateValue(null, "*${startDate}.nc");
        validator.validateValue(null, "?${startDate}.nc");
        try {
            validator.validateValue(null, "MER_${startDate}_${endDate}_${endDate}.nc");
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
            validator.validateValue(null, "MER\\_${startDate}.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        try {
            validator.validateValue(null, "MER\\_${startDate}_${endDate}.nc");
            fail();
        } catch (ValidationException expected) {
            // ok
        }
    }
}
