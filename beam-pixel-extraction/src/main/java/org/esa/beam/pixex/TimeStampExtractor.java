package org.esa.beam.pixex;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.beam.framework.datamodel.ProductData;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeStampExtractor {

    private static final String legalDateTimeCharMatcher = "[yMdhms:_\\.-]+";

    private static final String datePlaceholder = "${date}";
    private static final String dph = datePlaceholder;

    private static final String dphMatcher = "(\\$\\{date\\})";
    private static final String legalFilenameCharMatcher = "[\\?\\*\\w\\. -]*";

    private static final String aSign = "[\\\\w\\\\. -]";
    private static final String exactlyOneTimes = "{1}";
    private static final String anyTimes = "\\*";

    private static final String questionSignMeaning = aSign + exactlyOneTimes;
    private static final String starSignMeaning = aSign + anyTimes;

    private static final String yearMatcher = "(\\\\d{4})";
    private static final String monthMatcher = "(\\\\d{2})";
    private static final String dayMatcher = "(\\\\d{2})";
    private static final String hourMatcher = "(\\\\d{2})";
    private static final String minuteMatcher = "(\\\\d{2})";
    private static final String secondMatcher = "(\\\\d{2})";

    private final String datePattern;
    private final String filenamePattern;
    private Pattern singleDatePattern;
    private Pattern doubleDatesPattern;

    private final Map<DateType, Integer> startDateGroupIndices = new HashMap<DateType, Integer>(6);
    private final Map<DateType, Integer> stopDateGroupIndices = new HashMap<DateType, Integer>(6);

    public TimeStampExtractor(String dateInterpretationPattern, String filenameInterpretationPattern) {
        datePattern = dateInterpretationPattern;
        filenamePattern = filenameInterpretationPattern;
        init();
    }

    public ProductData.UTC[] extractTimeStamps(String fileName) throws ParseException {
        final Matcher matcher;
        if(filenameHasStopTime()) {
            matcher = doubleDatesPattern.matcher(fileName);
        } else {
            matcher = singleDatePattern.matcher(fileName);
        }
        final boolean matches = matcher.matches();
        if(!matches) {
            // todo
            throw new IllegalStateException("");
        }

        final String startYearGroup = getString(matcher, DateType.YEAR, startDateGroupIndices);
        final String startMonthGroup = getString(matcher, DateType.MONTH, startDateGroupIndices);
        final String startDayGroup = getString(matcher, DateType.DAY, startDateGroupIndices);
        final String startHourGroup = getString(matcher, DateType.HOUR, startDateGroupIndices);
        final String startMinuteGroup = getString(matcher, DateType.MINUTE, startDateGroupIndices);
        final String startSecondGroup = getString(matcher, DateType.SECOND, startDateGroupIndices);

        String pattern = createPattern(startYearGroup, startMonthGroup, startDayGroup, startHourGroup, startMinuteGroup, startSecondGroup);
        final ProductData.UTC startTime = ProductData.UTC.parse(
                startYearGroup + startMonthGroup + startDayGroup + startHourGroup + startMinuteGroup + startSecondGroup, pattern);

        if(!filenameHasStopTime()) {
            return new ProductData.UTC[]{startTime, startTime};
        }

        final String stopYearGroup = getString(matcher, DateType.YEAR, stopDateGroupIndices);
        final String stopMonthGroup = getString(matcher, DateType.MONTH, stopDateGroupIndices);
        final String stopDayGroup = getString(matcher, DateType.DAY, stopDateGroupIndices);
        final String stopHourGroup = getString(matcher, DateType.HOUR, stopDateGroupIndices);
        final String stopMinuteGroup = getString(matcher, DateType.MINUTE, stopDateGroupIndices);
        final String stopSecondGroup = getString(matcher, DateType.SECOND, stopDateGroupIndices);

        pattern = createPattern(stopYearGroup, stopMonthGroup, stopDayGroup, stopHourGroup, stopMinuteGroup, stopSecondGroup);
        final ProductData.UTC stopTime = ProductData.UTC.parse(
                stopYearGroup + stopMonthGroup + stopDayGroup + stopHourGroup + stopMinuteGroup + stopSecondGroup, pattern);
        
        return new ProductData.UTC[]{startTime, stopTime};
    }

    private String createPattern(String yearGroup, String monthGroup, String dayGroup, String hourGroup, String minuteGroup, String secondGroup) {
        final StringBuilder pattern = new StringBuilder();
        if (!"".equals(yearGroup)) {
            pattern.append("yyyy");
        }
        if(!"".equals(monthGroup)) {
            pattern.append("MM");
        }
        if(!"".equals(dayGroup)) {
            pattern.append("dd");
        }
        if(!"".equals(hourGroup)) {
            pattern.append("hh");
        }
        if(!"".equals(minuteGroup)) {
            pattern.append("mm");
        }
        if(!"".equals(secondGroup)) {
            pattern.append("ss");
        }
        return pattern.toString();
    }

    private String getString(Matcher matcher, DateType dateType, Map<DateType, Integer> groupIndices) {
        if(!groupIndices.containsKey(dateType)) {
            return "";
        }
        return matcher.group(groupIndices.get(dateType));
    }

    private void createGroupIndices() {
        final int yearIndex = datePattern.indexOf("yyyy");
        final int monthIndex = datePattern.indexOf("MM");
        final int dayIndex = datePattern.indexOf("dd");
        final int hourIndex = datePattern.indexOf("hh");
        final int minuteIndex = datePattern.indexOf("mm");
        final int secondIndex = datePattern.indexOf("ss");
        int maxGroup = createGroupIndices(1, yearIndex, monthIndex, dayIndex, hourIndex, minuteIndex, secondIndex, startDateGroupIndices);
        createGroupIndices(maxGroup, yearIndex, monthIndex, dayIndex, hourIndex, minuteIndex, secondIndex, stopDateGroupIndices);
    }

    private int createGroupIndices(int offset, int yearIndex, int monthIndex, int dayIndex, int hourIndex, int minuteIndex, int secondIndex, Map<DateType, Integer> startDateGroupIndices1) {
        List<Integer> indices = new ArrayList<Integer>(6);
        indices.add(yearIndex);
        if(monthIndex != -1) {
            indices.add(monthIndex);
        }
        if(dayIndex != -1) {
            indices.add(dayIndex);
        }
        if(hourIndex != -1) {
            indices.add(0, hourIndex);
        }
        if(minuteIndex != -1) {
            indices.add(minuteIndex);
        }
        if(secondIndex != -1) {
            indices.add(secondIndex);
        }
        Collections.sort(indices);
        int position = offset;
        for (Integer index : indices) {
            if(index == yearIndex) {
                startDateGroupIndices1.put(DateType.YEAR, position);
                position++;
            } else if (index == monthIndex) {
                startDateGroupIndices1.put(DateType.MONTH, position);
                position++;
            } else if (index == dayIndex) {
                startDateGroupIndices1.put(DateType.DAY, position);
                position++;
            } else if(index == hourIndex) {
                startDateGroupIndices1.put(DateType.HOUR, position);
                position++;
            } else if(index == minuteIndex) {
                startDateGroupIndices1.put(DateType.MINUTE, position);
                position++;
            } else if(index == secondIndex) {
                startDateGroupIndices1.put(DateType.SECOND, position);
                position++;
            }
        }
        return position;
    }

    private void init() {
        createGroupIndices();
        final int startDatePos = filenamePattern.indexOf(dph);
        String prefix = filenamePattern.substring(0, startDatePos);
        prefix = replaceSpecialSigns(prefix);
        if (!filenameHasStopTime()) {
            String suffix = filenamePattern.substring(startDatePos + dph.length());
            suffix = replaceSpecialSigns(suffix);
            String matcherExpression = prefix + getDateMatcher() + suffix;
            singleDatePattern = Pattern.compile(matcherExpression);
        } else {
            final int endDatePos = filenamePattern.lastIndexOf(dph);
            String inBetween = filenamePattern.substring(startDatePos + dph.length(), endDatePos);
            String suffix = filenamePattern.substring(endDatePos + dph.length());
            inBetween = replaceSpecialSigns(inBetween);
            suffix = replaceSpecialSigns(suffix);
            String matcherExpression = prefix + getDateMatcher() + inBetween + getDateMatcher() + suffix;
            doubleDatesPattern = Pattern.compile(matcherExpression);
        }
    }

    private boolean filenameHasStopTime() {
        return countOf(dph).in(filenamePattern) == 2;
    }

    private String replaceSpecialSigns(String string) {
        String result = string.replaceAll("\\*", starSignMeaning);
        result = result.replaceAll("\\?", questionSignMeaning);
        return result;
    }

    private String getDateMatcher() {
        String dateMatcher = datePattern.replaceAll("yyyy", yearMatcher);
        dateMatcher = dateMatcher.replaceAll("MM", monthMatcher);
        dateMatcher = dateMatcher.replaceAll("dd", dayMatcher);
        dateMatcher = dateMatcher.replaceAll("hh", hourMatcher);
        dateMatcher = dateMatcher.replaceAll("mm", minuteMatcher);
        dateMatcher = dateMatcher.replaceAll("ss", secondMatcher);
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

    static enum DateType {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND
    }
}
