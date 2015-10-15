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
package org.esa.snap.engine_utilities.db;

import org.esa.snap.core.datamodel.ProductData;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;

/**
 */
public class SQLUtils {

    public static String getOrList(final String columnStr, final String values[]) {
        final StringBuilder orListStr = new StringBuilder(columnStr.length() * values.length);
        orListStr.append('(');
        int i = 0;
        for (String v : values) {
            if (i > 0)
                orListStr.append(" OR ");
            orListStr.append(columnStr);
            orListStr.append(" = '");
            orListStr.append(v);
            orListStr.append("'");
            ++i;
        }
        orListStr.append(')');
        return orListStr.toString();
    }

    public static Date toSQLDate(final ProductData.UTC utc) {
        return toSQLDate(utc.getAsCalendar());
    }

    public static Date toSQLDate(final Calendar cal) {
        return new java.sql.Date(cal.getTimeInMillis());
    }

    public static String[] prependString(final String firstValue, final String[] origList) {
        final String[] newList = new String[origList.length + 1];
        newList[0] = firstValue;
        System.arraycopy(origList, 0, newList, 1, origList.length);
        return newList;
    }

    public static void addAND(StringBuilder str) {
        if (str.length() > 0)
            str.append(" AND ");
    }

    public static String insertTableName(final String[] tokens, final String tableName, final String freeQuery) {
        String query = freeQuery;
        for (String tok : tokens) {
            query = query.replaceAll(tok, tableName + '.' + tok);
        }
        return query;
    }

    public static void printResults(final ResultSet results) throws SQLException {
        while (results.next()) {
            final ResultSetMetaData meta = results.getMetaData();
            final int colCnt = meta.getColumnCount();

            for (int i = 1; i <= colCnt; ++i) {
                final String str = results.getString(i);
                System.out.print(meta.getColumnName(i) + ":" + str + " ");
            }
            System.out.println();
        }
    }
}
