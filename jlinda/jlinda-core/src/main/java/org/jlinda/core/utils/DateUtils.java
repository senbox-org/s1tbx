package org.jlinda.core.utils;

public class DateUtils {

    public static double dateTimeToSecOfDay(final String dateTime) {
        // assume format:  02-AUG-1995 21:16:42.210
        final String timeHrsMinSec = dateTime.split(" ")[1];
        final String timeHrs = timeHrsMinSec.split(":")[0];
        final String timeMin = timeHrsMinSec.split(":")[1];
        final String timeSec = timeHrsMinSec.split(":")[2];

        return (new Double(timeHrs) * 3600 + new Double(timeMin) * 60 + new Double(timeSec));
    }

}
