/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.gpf;

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
        if (dur > 7200) {
            return df.format(dur / 3600f) + " hours";
        } else if (dur > 120) {
            return df.format(dur / 60f) + " minutes";
        } else {
            return df.format(dur) + " seconds";
        }
    }
}
