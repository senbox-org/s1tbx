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

    private static final String LEGAL_DATE_TIME_CHAR_MATCHER = "[yMdhms:_\\.-]+";
    private static final String LEGAL_FILENAME_CHAR_MATCHER = "[\\?\\*\\w\\. -]*";

    private static final String DATE_PLACEHOLDER = "${date}";
    private static final String DPH_MATCHER = "(\\$\\{date\\})";

    private final String datePattern;
    private final String filenamePattern;

    private Map<DateType, Integer> startDateGroupIndices;
    private Map<DateType, Integer> stopDateGroupIndices;

    private TimeStampAccess timeStampAccess;

    public TimeStampExtractor(String dateInterpretationPattern, String filenameInterpretationPattern) {
        datePattern = dateInterpretationPattern;
        filenamePattern = filenameInterpretationPattern;
        init();
    }

    public ProductData.UTC[] extractTimeStamps(String fileName) throws ValidationException {
        final ProductData.UTC startTime = timeStampAccess.getStartTime(fileName);
        final ProductData.UTC stopTime = timeStampAccess.getStopTime(fileName);
        return new ProductData.UTC[]{startTime, stopTime};
    }

    private void init() {
        createGroupIndices();
        final boolean filenameHasStopTime = countOf(DATE_PLACEHOLDER).in(filenamePattern) == 2;
        if(filenameHasStopTime) {
            timeStampAccess = new StartStopTimeAccess();
        } else {
            timeStampAccess = new StartTimeAccess();
        }
        timeStampAccess.init();
    }

    private void createGroupIndices() {
        startDateGroupIndices = new HashMap<DateType, Integer>(6);
        stopDateGroupIndices = new HashMap<DateType, Integer>(6);
        final int yearIndex = datePattern.indexOf("yyyy");
        final int monthIndex = datePattern.indexOf("MM");
        final int dayIndex = datePattern.indexOf("dd");
        final int hourIndex = datePattern.indexOf("hh");
        final int minuteIndex = datePattern.indexOf("mm");
        final int secondIndex = datePattern.indexOf("ss");
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
        createGroupIndices(0, yearIndex, monthIndex, dayIndex, hourIndex, minuteIndex, secondIndex, indices, startDateGroupIndices);
        createGroupIndices(startDateGroupIndices.size(), yearIndex, monthIndex, dayIndex, hourIndex, minuteIndex, secondIndex, indices, stopDateGroupIndices);
    }

    private void createGroupIndices(int offset, int yearIndex, int monthIndex, int dayIndex, int hourIndex, int minuteIndex, int secondIndex, List<Integer> indices, Map<DateType, Integer> groupIndices) {
        int position = offset + 1;
        for (Integer index : indices) {
            if(index == yearIndex) {
                groupIndices.put(DateType.YEAR, position);
                position++;
            } else if (index == monthIndex) {
                groupIndices.put(DateType.MONTH, position);
                position++;
            } else if (index == dayIndex) {
                groupIndices.put(DateType.DAY, position);
                position++;
            } else if(index == hourIndex) {
                groupIndices.put(DateType.HOUR, position);
                position++;
            } else if(index == minuteIndex) {
                groupIndices.put(DateType.MINUTE, position);
                position++;
            } else if(index == secondIndex) {
                groupIndices.put(DateType.SECOND, position);
                position++;
            }
        }
    }

    private ProductData.UTC createTime(Matcher matcher, Map<DateType, Integer> groupIndices) {
        final String startYearGroup = getString(matcher, DateType.YEAR, groupIndices);
        final String startMonthGroup = getString(matcher, DateType.MONTH, groupIndices);
        final String startDayGroup = getString(matcher, DateType.DAY, groupIndices);
        final String startHourGroup = getString(matcher, DateType.HOUR, groupIndices);
        final String startMinuteGroup = getString(matcher, DateType.MINUTE, groupIndices);
        final String startSecondGroup = getString(matcher, DateType.SECOND, groupIndices);

        String pattern = createPattern(startYearGroup, startMonthGroup, startDayGroup, startHourGroup, startMinuteGroup, startSecondGroup);
        final ProductData.UTC startTime;
        try {
            startTime = ProductData.UTC.parse(
                    startYearGroup + startMonthGroup + startDayGroup + startHourGroup + startMinuteGroup + startSecondGroup, pattern);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        return startTime;
    }

    private String getString(Matcher matcher, DateType dateType, Map<DateType, Integer> groupIndices) {
        if(!groupIndices.containsKey(dateType)) {
            return "";
        }
        return matcher.group(groupIndices.get(dateType));
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


    private String replaceSpecialSigns(String string) {
        final String validSign = "[\\\\w\\\\. -]";
        final String exactlyOneTimesModifier = "{1}";
        final String anyTimesModifier = "\\*";

        final String starSignPattern = validSign + anyTimesModifier;
        final String questionSignPattern = validSign + exactlyOneTimesModifier;

        String result = string.replaceAll("\\*", starSignPattern);
        result = result.replaceAll("\\?", questionSignPattern);
        return result;
    }

    private String getDateMatcher() {
        final String yearPattern = "yyyy";
        final String monthPattern = "MM";
        final String dayPattern = "dd";
        final String hourPattern = "hh";
        final String minutePattern = "mm";
        final String secondPattern = "ss";

        final String yearMatcher = "(\\\\d{" + yearPattern.length() + "})";
        final String monthMatcher = "(\\\\d{" + monthPattern.length() + "})";
        final String dayMatcher = "(\\\\d{" + dayPattern.length() + "})";
        final String hourMatcher = "(\\\\d{" + hourPattern.length() + "})";
        final String minuteMatcher = "(\\\\d{" + minutePattern.length() + "})";
        final String secondMatcher = "(\\\\d{" + secondPattern.length() + "})";

        String dateMatcher = datePattern.replaceAll(yearPattern, yearMatcher);
        dateMatcher = dateMatcher.replaceAll(monthPattern, monthMatcher);
        dateMatcher = dateMatcher.replaceAll(dayPattern, dayMatcher);
        dateMatcher = dateMatcher.replaceAll(hourPattern, hourMatcher);
        dateMatcher = dateMatcher.replaceAll(minutePattern, minuteMatcher);
        dateMatcher = dateMatcher.replaceAll(secondPattern, secondMatcher);
        return dateMatcher;
    }

    private void validateFileName(Matcher matcher, String fileName) throws ValidationException {
        if(!matcher.matches()) {
            throw new ValidationException("Given filename '" + fileName + "' does not match the given date pattern.");
        }
    }

    public static class DateInterpretationPatternValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String pattern = ((String) value).trim();
            if (pattern.length() < 4) {
                throw new ValidationException("Value of dateInterpretationPattern must at least contain 4 Characters");
            }
            if (!pattern.matches(LEGAL_DATE_TIME_CHAR_MATCHER)) {
                throw new ValidationException("Value of dateInterpretationPattern contains illegal charachters.\n" +
                        "Valid characters are: 'y' 'M' 'd' 'h' 'm' 's' ':' '_' '-' '.'");
            }
            if (!pattern.contains("yyyy")) {
                throw new ValidationException("Value of dateInterpretationPattern must contain 'yyyy' as year placeholder.");
            }
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
            if (!pattern.contains(DATE_PLACEHOLDER)) {
                throw new ValidationException("Value of filenameInterpretationPattern must contain a date placeholder '" +
                                              DATE_PLACEHOLDER + "' at least once.");
            }
            final int count = countOf(DATE_PLACEHOLDER).in(pattern);
            if (count > 2) {
                throw new ValidationException("Value of filenameInterpretationPattern can contain a date placeholder '" +
                                              DATE_PLACEHOLDER + "' twice maximally.");
            }
            if (count == 2) {
                if (!pattern.matches(
                        LEGAL_FILENAME_CHAR_MATCHER + DPH_MATCHER + LEGAL_FILENAME_CHAR_MATCHER + DPH_MATCHER +
                        LEGAL_FILENAME_CHAR_MATCHER)) {
                    throw new ValidationException("Value of filenameInterpretationPattern contains illegal characters.\n" +
                            "legal characters are a-zA-Z0-9_-*.?${}");
                }
            } else {
                if (!pattern.matches(LEGAL_FILENAME_CHAR_MATCHER + DPH_MATCHER + LEGAL_FILENAME_CHAR_MATCHER)) {
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

    private static interface TimeStampAccess {

        void init();
        ProductData.UTC getStartTime(String fileName) throws ValidationException;
        ProductData.UTC getStopTime(String fileName) throws ValidationException;
    }

    private class StartTimeAccess implements TimeStampAccess {

        private Pattern startDatePattern;

        @Override
        public void init() {
            final int startDatePos = filenamePattern.indexOf(DATE_PLACEHOLDER);
            String prefix = filenamePattern.substring(0, startDatePos);
            prefix = replaceSpecialSigns(prefix);
            String suffix = filenamePattern.substring(startDatePos + DATE_PLACEHOLDER.length());
            suffix = replaceSpecialSigns(suffix);
            String matcherExpression = prefix + getDateMatcher() + suffix;
            startDatePattern = Pattern.compile(matcherExpression);
        }

        @Override
        public ProductData.UTC getStartTime(String fileName) throws ValidationException {
            final Matcher matcher = startDatePattern.matcher(fileName);
            validateFileName(matcher, fileName);
            return createTime(matcher, startDateGroupIndices);
        }

        @Override
        public ProductData.UTC getStopTime(String fileName) throws ValidationException {
            return getStartTime(fileName);
        }
    }

    private class StartStopTimeAccess implements TimeStampAccess {

        private Pattern startStopDatesPattern;

        @Override
        public void init() {
            final int startDatePos = filenamePattern.indexOf(DATE_PLACEHOLDER);
            final int endDatePos = filenamePattern.lastIndexOf(DATE_PLACEHOLDER);
            String prefix = filenamePattern.substring(0, startDatePos);
            String inBetween = filenamePattern.substring(startDatePos + DATE_PLACEHOLDER.length(), endDatePos);
            String suffix = filenamePattern.substring(endDatePos + DATE_PLACEHOLDER.length());

            prefix = replaceSpecialSigns(prefix);
            inBetween = replaceSpecialSigns(inBetween);
            suffix = replaceSpecialSigns(suffix);

            String matcherExpression = prefix + getDateMatcher() + inBetween + getDateMatcher() + suffix;
            startStopDatesPattern = Pattern.compile(matcherExpression);
        }

        @Override
        public ProductData.UTC getStartTime(String fileName) throws ValidationException {
            final Matcher matcher = startStopDatesPattern.matcher(fileName);
            validateFileName(matcher, fileName);
            return createTime(matcher, startDateGroupIndices);
        }

        @Override
        public ProductData.UTC getStopTime(String fileName) throws ValidationException {
            final Matcher matcher = startStopDatesPattern.matcher(fileName);
            validateFileName(matcher, fileName);
            return createTime(matcher, stopDateGroupIndices);
        }
    }
}
