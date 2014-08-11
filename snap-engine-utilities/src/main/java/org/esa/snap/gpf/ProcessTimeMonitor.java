package org.esa.snap.gpf;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**

 */
public final class ProcessTimeMonitor {

    private Date executeStartTime = null;

    public ProcessTimeMonitor() {

    }

    public void start() {
        executeStartTime = Calendar.getInstance().getTime();
    }

    public long stop() {
        return getCurrentDuration();
    }

    public long getCurrentDuration() {
        final Date now = Calendar.getInstance().getTime();
        return (now.getTime() - executeStartTime.getTime()) / 1000;
    }

    public static String formatDuration(final long dur) {
        final DecimalFormat df = new DecimalFormat("#.##");
        if (dur > 120) {
            final float minutes = dur / 60f;
            return df.format(minutes) + " minutes";
        } else {
            return df.format(dur) + " seconds";
        }
    }
}
