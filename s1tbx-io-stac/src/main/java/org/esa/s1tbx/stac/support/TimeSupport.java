/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac.support;

import org.esa.snap.core.datamodel.ProductData;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeSupport {

    public static final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String getFormattedTime(final ProductData.UTC utc) {
        if (utc != null) {
            final Calendar calendar = ProductData.UTC.createCalendar();
            calendar.add(Calendar.DATE, utc.getDaysFraction());
            calendar.add(Calendar.SECOND, (int) utc.getSecondsFraction());
            final Date time = calendar.getTime();
            final String dateString = dateFormat.format(time);
            final String microsString = String.valueOf(utc.getMicroSecondsFraction());
            StringBuilder sb = new StringBuilder(dateString.toUpperCase());
            sb.append('.');
            for (int i = microsString.length(); i < 6; i++) {
                sb.append('0');
            }
            sb.append(microsString);
            sb.append('Z');
            return sb.toString().replace(' ', 'T');
        }
        return "Unknown";
    }
}
