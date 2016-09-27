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

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MetadataTable implements TableInterface {

    public static final String TABLE = "APP.METADATA";

    private final Connection dbConnection;
    private final static List<String> metadataNamesList = new ArrayList<>();

    private PreparedStatement stmtSaveNewRecord;
    private PreparedStatement stmtDeleteProduct;
    private PreparedStatement stmtGetMetadata;

    private final static MetadataElement emptyMetadata = AbstractMetadata.addAbstractedMetadataHeader(null);
    private static String createTableStr;
    private static String saveProductStr;

    static {
        createTableStrings();
    }

    private static final String strCreateProductTable =
            "create table " + TABLE + " (" +
                    "    ID          INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)";

    private static final String strSaveProduct =
            "INSERT INTO " + TABLE + ' ';

    private static final String strDeleteProduct =
            "DELETE FROM " + TABLE + " WHERE ID = ?";

    private static final String strGetMetadata =
            "SELECT * FROM " + TABLE + ' ' +
                    "WHERE ID = ?";

    public MetadataTable(final Connection dbConnection) throws SQLException {
        this.dbConnection = dbConnection;
    }

    public void createTable() throws SQLException {
        try (final Statement statement = dbConnection.createStatement()) {
            statement.execute(createTableStr);
        }
    }

    public void validateTable() throws SQLException {

        try (final Statement alterStatement = dbConnection.createStatement()) {
            alterStatement.setMaxRows(2);

            final String selectStr = "SELECT * FROM " + TABLE;
            final ResultSet results = alterStatement.executeQuery(selectStr);

            final ResultSetMetaData meta = results.getMetaData();
            final int colCnt = meta.getColumnCount();

            final String[] colNames = new String[colCnt + 1];
            for (int i = 1; i <= colCnt; ++i) {
                colNames[i] = meta.getColumnName(i);
            }
            final MetadataAttribute[] attribList = emptyMetadata.getAttributes();
            for (MetadataAttribute attrib : attribList) {
                final String name = attrib.getName();
                boolean found = false;
                for (String col : colNames) {
                    if (name.equalsIgnoreCase(col)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    final int dataType = attrib.getDataType();
                    final String alterStr = "ALTER TABLE " + TABLE + " ADD COLUMN " + name + ' ' + getDataType(dataType) +
                            " DEFAULT " + getDefault(dataType) + " NOT NULL";
                    alterStatement.execute(alterStr);
                }
            }
        }
    }

    private static void createTableStrings() {
        createTableStr = strCreateProductTable;
        String namesStr = "";
        String valueStr = "";

        final MetadataAttribute[] attribList = emptyMetadata.getAttributes();
        for (MetadataAttribute attrib : attribList) {
            final String name = attrib.getName();
            metadataNamesList.add(name);
            createTableStr += ", " + name + ' ' + getDataType(attrib.getDataType());
            namesStr += name + ',';
            valueStr += "?,";
        }
        createTableStr += ")";
        namesStr = namesStr.substring(0, namesStr.length() - 1);
        valueStr = valueStr.substring(0, valueStr.length() - 1);

        saveProductStr = strSaveProduct + '(' + namesStr + ')' + "VALUES (" + valueStr + ')';
    }

    private static String getDataType(final int dataType) {
        if (dataType == ProductData.TYPE_FLOAT32)
            return "FLOAT";
        else if (dataType == ProductData.TYPE_FLOAT64)
            return "DOUBLE";
        else if (dataType == ProductData.TYPE_UTC)
            return "VARCHAR(255)"; //"TIMESTAMP";
        else if (dataType < ProductData.TYPE_FLOAT32)
            return "INTEGER";
        return "VARCHAR(555)";
    }

    private static String getDefault(final int dataType) {
        if (dataType == ProductData.TYPE_FLOAT32)
            return "99999";
        else if (dataType == ProductData.TYPE_FLOAT64)
            return "99999";
        else if (dataType == ProductData.TYPE_UTC)
            return " "; //"TIMESTAMP";
        else if (dataType < ProductData.TYPE_FLOAT32)
            return "99999";
        return "' '";
    }


    public void prepareStatements() throws SQLException {
        stmtSaveNewRecord = dbConnection.prepareStatement(saveProductStr, Statement.RETURN_GENERATED_KEYS);
        stmtDeleteProduct = dbConnection.prepareStatement(strDeleteProduct);
        stmtGetMetadata = dbConnection.prepareStatement(strGetMetadata);
    }

    public ResultSet addRecord(final ProductEntry record) throws SQLException {
        stmtSaveNewRecord.clearParameters();
        //System.out.println(record.getFile());

        final MetadataElement absRoot = record.getMetadata();
        if (absRoot == null)
            throw new SQLException("Metadata is null");
        final MetadataAttribute[] attribList = emptyMetadata.getAttributes();
        int i = 1;
        for (MetadataAttribute attrib : attribList) {
            final String name = attrib.getName();
            final int dataType = attrib.getDataType();
            if (dataType == ProductData.TYPE_FLOAT32)
                stmtSaveNewRecord.setFloat(i, (float) absRoot.getAttributeDouble(name));
            else if (dataType == ProductData.TYPE_FLOAT64)
                stmtSaveNewRecord.setDouble(i, absRoot.getAttributeDouble(name));
            else if (dataType == ProductData.TYPE_UTC)
                //stmtSaveNewRecord.setDate(i, new Date((long)absRoot.getAttributeUTC(name).getMJD()));
                stmtSaveNewRecord.setString(i, absRoot.getAttributeUTC(name).getElemString());
            else if (dataType < ProductData.TYPE_FLOAT32)
                stmtSaveNewRecord.setInt(i, absRoot.getAttributeInt(name));
            else
                stmtSaveNewRecord.setString(i, absRoot.getAttributeString(name));
            ++i;
        }
        final int rowCount = stmtSaveNewRecord.executeUpdate();
        return stmtSaveNewRecord.getGeneratedKeys();
    }

    public void deleteRecord(final int id) throws SQLException {
        stmtDeleteProduct.clearParameters();
        stmtDeleteProduct.setInt(1, id);
        stmtDeleteProduct.executeUpdate();
    }

    public MetadataElement getProductMetadata(final int id) throws SQLException {
        stmtGetMetadata.clearParameters();
        stmtGetMetadata.setString(1, String.valueOf(id));
        final ResultSet results = stmtGetMetadata.executeQuery();
        if (results.next()) {
            return createMetadataRoot(results);
        }
        return null;
    }

    private static MetadataElement createMetadataRoot(final ResultSet results) {
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(null);
        final MetadataAttribute[] attribList = emptyMetadata.getAttributes();
        for (MetadataAttribute attrib : attribList) {
            try {
                final int dataType = attrib.getDataType();
                final String name = attrib.getName();
                if (dataType == ProductData.TYPE_FLOAT32) {
                    AbstractMetadata.setAttribute(absRoot, name, results.getFloat(name));
                } else if (dataType == ProductData.TYPE_FLOAT64) {
                    AbstractMetadata.setAttribute(absRoot, name, results.getDouble(name));
                } else if (dataType == ProductData.TYPE_UTC) {
                    AbstractMetadata.setAttribute(absRoot, name, AbstractMetadata.parseUTC(results.getString(name)));
                } else if (dataType < ProductData.TYPE_FLOAT32) {
                    AbstractMetadata.setAttribute(absRoot, name, results.getInt(name));
                } else {
                    AbstractMetadata.setAttribute(absRoot, name, results.getString(name));
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
            }
        }
        return absRoot;
    }

    public static String[] getAllMetadataNames() {
        return metadataNamesList.toArray(new String[metadataNamesList.size()]);
    }
}
