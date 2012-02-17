package org.esa.beam.pixex;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.beam.framework.datamodel.ProductData;

public class TimeStampExtractor {

    private static final String legalDateTimeCharMatcher = "[yMdhms:_\\.-]+";

    private static final String datePlaceholder = "${date}";
    private static final String dph = datePlaceholder;

    private static final String dphMatcher = "(\\$\\{date\\})";
    private static final String legalFilenameCharMatcher = "[\\?\\*\\w\\. -]*";

    private static final String aSign = "[\\w\\. -]";
    private static final String exactlyOneTimes = "{1}";
    private static final String anyTimes = "*";

    private static final String questionSignMeaning = aSign + exactlyOneTimes;
    private static final String starSignMeaning = aSign + anyTimes;

    private static final String yearMatcher = "(yyyy)";
    private static final String monthMatcher = "(MM)";
    private static final String dayMatcher = "(\\d\\d)";
    private static final String hourMatcher = "(hh)";
    private static final String minuteMatcher = "(mm)";
    private static final String secondMatcher = "(ss)";

    private final String datePattern;
    private final String filenamePattern;

    public TimeStampExtractor(String dateInterpretationPattern, String filenameInterpretationPattern) {
        datePattern = dateInterpretationPattern;
        filenamePattern = filenameInterpretationPattern;
        init();
    }

    public ProductData.UTC[] extractTimeStamp(String name) {
        throw new IllegalStateException("Not implemented now");
    }

    private void init() {
        if (countOf(dph).in(filenamePattern) == 1) {
            final int datePos = filenamePattern.indexOf(dph);
            String prefix = filenamePattern.substring(0, datePos);
            String suffix = filenamePattern.substring(datePos + dph.length());
            prefix = replaceStarAnQuestionSign(prefix);
            suffix = replaceStarAnQuestionSign(suffix);
            final String matcherExpression = prefix + getDateMatcher() + suffix;
        }
    }

    private String replaceStarAnQuestionSign(String string) {
        string = string.replaceAll("\\*", starSignMeaning);
        string = string.replaceAll("\\?", questionSignMeaning);
        return string;
    }

    public String getDateMatcher() {
        String dateMatcher = datePattern.replaceAll(yearMatcher, yearMatcher);
        dateMatcher = dateMatcher.replaceAll(monthMatcher, monthMatcher);
        dateMatcher = dateMatcher.replaceAll(dayMatcher, dayMatcher);
        dateMatcher = dateMatcher.replaceAll(hourMatcher, hourMatcher);
        dateMatcher = dateMatcher.replaceAll(minuteMatcher, minuteMatcher);
        dateMatcher = dateMatcher.replaceAll(secondMatcher, secondMatcher);
        return dateMatcher;
   }

    public static class DateInterpretationPatternValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String pattern = ((String) value).trim();
            if (pattern.length() < 4) {
                throw new ValidationException("Value of dateInterpretationPattern must at least contain 4 Characters");
            }
            if (!pattern.matches(legalDateTimeCharMatcher)) {
                throw new ValidationException("Value of dateInterpretationPattern contains illegal charachters.\n" +
                        "Valid characters are: 'y' 'M' 'd' 'h' 'm' 's' ':' '_' '-' '.'");
            }
            if (!pattern.contains("yyyy")) {
                throw new ValidationException("Value of dateInterpretationPattern must contain 'yyyy' as year placeholder.");
            }
//            if (!pattern.contains("MM")) {
//                throw new ValidationException("Value of dateInterpretationPattern must contain 'MM' as month placeholder.");
//            }
            if (countOf("yyyy").in(pattern) > 1
                    || countOf("MM").in(pattern) > 1
                    || countOf("dd").in(pattern) > 1
                    || countOf("hh").in(pattern) > 1
                    || countOf("mm").in(pattern) > 1
                    || countOf("ss").in(pattern) > 1
                    ) {
                throw new ValidationException("Value of dateInterpretationPattern can contain each of character sequences " +
                        "('yyyy', 'MM', 'dd', 'hh', 'mm', 'ss') only once.");
            }
        }
    }

    public static class FilenameInterpretationPatternValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String pattern = ((String) value).trim();
            if (!pattern.contains(dph)) {
                throw new ValidationException("Value of filenameInterpretationPattern must contain a date placeholder '" + dph + "' at least once.");
            }
            final int count = countOf(dph).in(pattern);
            if (count > 2) {
                throw new ValidationException("Value of filenameInterpretationPattern can contain a date placeholder '" + dph + "' twice maximally.");
            }
            if (count == 2) {
                if (!pattern.matches(legalFilenameCharMatcher + dphMatcher + legalFilenameCharMatcher + dphMatcher + legalFilenameCharMatcher)) {
                    throw new ValidationException("Value of filenameInterpretationPattern contains illegal characters.\n" +
                            "legal characters are a-zA-Z0-9_-*.?${}");
                }
            } else {
                if (!pattern.matches(legalFilenameCharMatcher + dphMatcher + legalFilenameCharMatcher)) {
                    throw new ValidationException("Value of filenameInterpretationPattern contains illegal characters.\n" +
                            "legal characters are a-zA-Z0-9_-*.?${}");
                }
            }
        }

    }

    private static CountOf countOf(String countString) {
        return new CountOf(countString);
    }

    private static class CountOf {
        private final String countString;

        private CountOf(String countString) {
            this.countString = countString;
        }

        private int in(String string) {
            int count = 0;
            int fromIndex = 0;
            while (string.indexOf(countString, fromIndex) != -1) {
                fromIndex = string.indexOf(countString, fromIndex) + 1;
                count++;
            }
            return count;
        }
    }
}
