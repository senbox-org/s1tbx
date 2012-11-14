package org.esa.beam.opendap.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tonio Fincke
 */
public class PatternProviderTest {

    @Test
    public void testRecognizePatterns_non_valid() throws Exception {
        assertEquals(0, PatternProvider.recommendPatterns("no_valid_pattern").size());
    }

    @Test
    public void testRecommendPatterns_firstPattern() throws Exception {
        List<String[]> patternCombinations = PatternProvider.recommendPatterns("sth__20100101:200101.nc");
        assertTrue(patternCombinations.size() >= 3);

        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd:hhmmss", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMM", "*${startDate}*"});
    }

    @Test
    public void testRecommendPatterns_secondPattern_twoDates() throws Exception {
        List<String[]> patternCombinations = PatternProvider.recommendPatterns("sth__20100101_192345_20110101_192345.nc");
        assertTrue(patternCombinations.size() >= 6);
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd_hhmmss", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd_hhmmss", "*${startDate}*${endDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd", "*${startDate}*${endDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMM", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMM", "*${startDate}*${endDate}*"});
    }

    @Test
    public void testRecommendPatterns_variousPossiblePatterns() throws Exception {
        List<String[]> patternCombinations = PatternProvider.recommendPatterns("sth__20100101192345_99999999.nc");
        assertTrue(patternCombinations.size() >= 7);
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd_hhmmss", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMddhhmmss", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMddhh", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMMdd", "*${startDate}*${endDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMM", "*${startDate}*"});
        containAssertEquals(patternCombinations, new String[]{"yyyyMM", "*${startDate}*${endDate}*"});
    }

    private static void containAssertEquals(List<String[]> containingList, String[] expectedStrings) {
        boolean expectedIsContained = false;
        for (String[] actualStrings : containingList) {
            if (actualStrings[0].equals(expectedStrings[0]) &&
                    actualStrings[1].equals(expectedStrings[1]) &&
                    actualStrings.length == expectedStrings.length &&
                    actualStrings.length == 2) {
                expectedIsContained = true;
                break;
            }
        }
        assertEquals("Expected String[] " + expectedStrings.toString() + " could not be found in list", true, expectedIsContained);
    }

}
