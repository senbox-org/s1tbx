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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**

 */
public interface TableInterface {

    public void createTable() throws SQLException;

    public void validateTable() throws SQLException;

    public void prepareStatements() throws SQLException;

    static String createTableString(final String table, final String[] colNames, final String[] colTypes) {
        int i = 0;
        final StringBuilder s = new StringBuilder(255);
        s.append("create table " + table + " (" +
                         "    ID          INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),");
        for (String n : colNames) {
            s.append(n);
            s.append(' ');
            s.append(colTypes[i++]);
            s.append(", ");
        }
        return s.substring(0, s.length() - 2) + ')';
    }

    static String createSaveString(final String table, final String[] colNames) {
        final StringBuilder s = new StringBuilder(255);
        s.append("INSERT INTO " + table + " (");
        for (String n : colNames) {
            s.append(n);
            s.append(", ");
        }
        s.delete(s.length() - 2, s.length());
        s.append(") VALUES (");
        for (String n : colNames) {
            s.append("?, ");
        }
        s.delete(s.length() - 2, s.length());
        s.append(')');
        return s.toString();
    }

    default void validateTable(final Connection dbConnection, final String table,
                               final String[] colNames, final String[] colTypes) throws SQLException {
        // alter table if columns are missing
        try (final Statement alterStatement = dbConnection.createStatement()) {

            // add missing columns to the table
            int i = 0;
            for (String n : colNames) {
                final String testStr = "SELECT '" + n + "' FROM " + table;
                try {
                    alterStatement.executeQuery(testStr);
                } catch (SQLException e) {
                    if (e.getSQLState().equals("42X04")) {
                        final String alterStr = "ALTER TABLE " + table + " ADD '" + n + "' " + colTypes[i];
                        alterStatement.execute(alterStr);
                    }
                }
                ++i;
            }
        }
    }
}
