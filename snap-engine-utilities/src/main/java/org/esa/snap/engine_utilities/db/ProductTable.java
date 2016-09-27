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

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProductTable implements TableInterface {

    public static final String TABLE = "APP.PRODUCTS";

    private final Connection dbConnection;

    private PreparedStatement stmtSaveNewRecord;
    private PreparedStatement stmtGetProduct;
    private PreparedStatement stmtGetProductWithPath;
    private PreparedStatement stmtGetProductInPath;
    private PreparedStatement stmtDeleteProduct;
    private PreparedStatement stmtAllMissions;
    private PreparedStatement stmtAllProductTypes;
    private PreparedStatement stmtAllAcquisitionModes;

    private static final String[] colNames = {
            AbstractMetadata.PATH,
            AbstractMetadata.PRODUCT,
            AbstractMetadata.MISSION,
            AbstractMetadata.PRODUCT_TYPE,
            AbstractMetadata.ACQUISITION_MODE,
            AbstractMetadata.PASS,
            AbstractMetadata.first_near_lat,
            AbstractMetadata.first_near_long,
            AbstractMetadata.first_far_lat,
            AbstractMetadata.first_far_long,
            AbstractMetadata.last_near_lat,
            AbstractMetadata.last_near_long,
            AbstractMetadata.last_far_lat,
            AbstractMetadata.last_far_long,
            AbstractMetadata.range_spacing,
            AbstractMetadata.azimuth_spacing,
            AbstractMetadata.first_line_time,
            ProductEntry.FILE_SIZE,
            ProductEntry.LAST_MODIFIED,
            ProductEntry.FILE_FORMAT,
            ProductEntry.GEO_BOUNDARY
    };

    private static final String[] colTypes = {
            "VARCHAR(255)",
            "VARCHAR(255)",
            "VARCHAR(30)",
            "VARCHAR(30)",
            "VARCHAR(30)",
            "VARCHAR(30)",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DOUBLE",
            "DATE",
            "DOUBLE",
            "DOUBLE",
            "VARCHAR(40)",
            "VARCHAR(1200)"
    };

    private static final String strCreateProductTable = TableInterface.createTableString(TABLE, colNames, colTypes);

    private static final String strGetProduct =
            "SELECT * FROM " + TABLE + " WHERE ID = ?";

    private static final String strSaveProduct = TableInterface.createSaveString(TABLE, colNames);

    private static final String strGetListEntries =
            "SELECT * FROM " + TABLE + " ORDER BY " + AbstractMetadata.MISSION + " ASC";

    private static final String strGetProductWithPath =
            "SELECT * FROM " + TABLE + " WHERE " + AbstractMetadata.PATH + " = ?";

    private static final String strUpdateProduct =
            "UPDATE " + TABLE + " SET " +
                    AbstractMetadata.PATH + " = ?, " +
                    AbstractMetadata.MISSION + " = ?, " +
                    AbstractMetadata.PRODUCT_TYPE + " = ? " +
                    "WHERE ID = ?";

    private static final String strDeleteProduct =
            "DELETE FROM " + TABLE + " WHERE ID = ?";

    private static final String strAllMissions = "SELECT DISTINCT " + AbstractMetadata.MISSION + " FROM " + TABLE;
    private static final String strAllProductTypes = "SELECT DISTINCT " + AbstractMetadata.PRODUCT_TYPE + " FROM " + TABLE;
    private static final String strAllAcquisitionModes = "SELECT DISTINCT " + AbstractMetadata.ACQUISITION_MODE + " FROM " + TABLE;

    public ProductTable(final Connection dbConnection) throws SQLException {
        this.dbConnection = dbConnection;
    }

    public void createTable() throws SQLException {
        try (final Statement statement = dbConnection.createStatement()) {
            statement.execute(strCreateProductTable);
        }
    }

    public void validateTable() throws SQLException {
        validateTable(dbConnection, TABLE, colNames, colTypes);
    }

    public void prepareStatements() throws SQLException {
        stmtSaveNewRecord = dbConnection.prepareStatement(strSaveProduct, Statement.RETURN_GENERATED_KEYS);
        stmtGetProductWithPath = dbConnection.prepareStatement(strGetProductWithPath);
        stmtDeleteProduct = dbConnection.prepareStatement(strDeleteProduct);

        stmtAllMissions = dbConnection.prepareStatement(strAllMissions);
        stmtAllProductTypes = dbConnection.prepareStatement(strAllProductTypes);
        stmtAllAcquisitionModes = dbConnection.prepareStatement(strAllAcquisitionModes);
    }

    public ResultSet addRecord(final ProductEntry record) throws SQLException {
        stmtSaveNewRecord.clearParameters();
        int i = 1;
        if(record.getFile() == null)
            stmtSaveNewRecord.setString(i++, "");
        else
            stmtSaveNewRecord.setString(i++, record.getFile().getAbsolutePath());
        stmtSaveNewRecord.setString(i++, record.getName());
        stmtSaveNewRecord.setString(i++, record.getMission());
        stmtSaveNewRecord.setString(i++, record.getProductType());
        stmtSaveNewRecord.setString(i++, record.getAcquisitionMode());
        stmtSaveNewRecord.setString(i++, record.getPass());
        stmtSaveNewRecord.setDouble(i++, record.getFirstNearGeoPos().getLat());
        stmtSaveNewRecord.setDouble(i++, record.getFirstNearGeoPos().getLon());
        stmtSaveNewRecord.setDouble(i++, record.getFirstFarGeoPos().getLat());
        stmtSaveNewRecord.setDouble(i++, record.getFirstFarGeoPos().getLon());
        stmtSaveNewRecord.setDouble(i++, record.getLastNearGeoPos().getLat());
        stmtSaveNewRecord.setDouble(i++, record.getLastNearGeoPos().getLon());
        stmtSaveNewRecord.setDouble(i++, record.getLastFarGeoPos().getLat());
        stmtSaveNewRecord.setDouble(i++, record.getLastFarGeoPos().getLon());
        stmtSaveNewRecord.setDouble(i++, record.getRangeSpacing());
        stmtSaveNewRecord.setDouble(i++, record.getAzimuthSpacing());
        stmtSaveNewRecord.setDate(i++, SQLUtils.toSQLDate(record.getFirstLineTime()));
        stmtSaveNewRecord.setDouble(i++, record.getFileSize());
        stmtSaveNewRecord.setDouble(i++, record.getLastModified());
        stmtSaveNewRecord.setString(i++, record.getFileFormat());
        final String geoStr = record.formatGeoBoundayString();
        if (geoStr.length() > 1200) {
            SystemUtils.LOG.warning("Geoboundary string exceeds 1200");
            stmtSaveNewRecord.setString(i++, "");
        } else {
            stmtSaveNewRecord.setString(i++, geoStr);
        }

        final int rowCount = stmtSaveNewRecord.executeUpdate();
        return stmtSaveNewRecord.getGeneratedKeys();
    }

    /* public void editRecord(final ProductEntry record) throws SQLException {
        stmtUpdateExistingRecord.clearParameters();

        stmtUpdateExistingRecord.setString(1, record.getFile());
        stmtUpdateExistingRecord.setInt(12, record.getId());
        stmtUpdateExistingRecord.executeUpdate();
    } */

    public void deleteRecord(final int id) throws SQLException {
        stmtDeleteProduct.clearParameters();
        stmtDeleteProduct.setInt(1, id);
        stmtDeleteProduct.executeUpdate();
    }

    public ProductEntry getProductEntry(final File path) throws SQLException {
        stmtGetProductWithPath.clearParameters();
        stmtGetProductWithPath.setString(1, path.getAbsolutePath());
        final ResultSet results = stmtGetProductWithPath.executeQuery();
        if (results.next()) {
            return new ProductEntry(results);
        }
        return null;
    }

    public boolean pathExists(final File path) throws SQLException {
        if (path == null)
            return false;
        stmtGetProductWithPath.clearParameters();
        stmtGetProductWithPath.setString(1, path.getAbsolutePath());
        final ResultSet results = stmtGetProductWithPath.executeQuery();
        return results.next();
    }

    public ProductEntry[] getProductEntryList() throws SQLException {
        final List<ProductEntry> listEntries = new ArrayList<>();

        try (final Statement queryStatement = dbConnection.createStatement()) {
            final ResultSet results = queryStatement.executeQuery(strGetListEntries);
            while (results.next()) {
                listEntries.add(new ProductEntry(results));
            }
            return listEntries.toArray(new ProductEntry[listEntries.size()]);
        }
    }

    public String[] getAllMissions() throws SQLException {
        if (stmtAllMissions == null)
            return new String[]{};
        final List<String> listEntries = new ArrayList<>();
        final ResultSet results = stmtAllMissions.executeQuery();
        while (results.next()) {
            listEntries.add(results.getString(1));
        }
        return listEntries.toArray(new String[listEntries.size()]);
    }

    /**
     * Get All product types
     *
     * @return list of product types
     * @throws SQLException .
     */
    public String[] getAllProductTypes() throws SQLException {
        final List<String> listEntries = new ArrayList<>();
        final ResultSet results = stmtAllProductTypes.executeQuery();
        while (results.next()) {
            listEntries.add(results.getString(1));
        }
        return listEntries.toArray(new String[listEntries.size()]);
    }

    /**
     * Get All product types for specified mission
     *
     * @param missions the selected missions
     * @return list of product types
     * @throws SQLException .
     */
    public String[] getProductTypes(final String[] missions) throws SQLException {
        if (missions == null || missions.length == 0)
            return new String[]{};
        String strMissionProductTypes = "SELECT DISTINCT " + AbstractMetadata.PRODUCT_TYPE + " FROM " + TABLE + " WHERE ";
        strMissionProductTypes += SQLUtils.getOrList(AbstractMetadata.MISSION, missions);

        final List<String> listEntries = new ArrayList<>();
        try (final Statement queryStatement = dbConnection.createStatement()) {
            final ResultSet results = queryStatement.executeQuery(strMissionProductTypes);
            while (results.next()) {
                listEntries.add(results.getString(1));
            }
            return listEntries.toArray(new String[listEntries.size()]);
        }
    }

    /**
     * Get All acquisition modes
     *
     * @return list of acquisition modes
     * @throws SQLException .
     */
    public String[] getAllAcquisitionModes() throws SQLException {
        final List<String> listEntries = new ArrayList<>();
        final ResultSet results = stmtAllAcquisitionModes.executeQuery();
        while (results.next()) {
            listEntries.add(results.getString(1));
        }
        return listEntries.toArray(new String[listEntries.size()]);
    }

    /**
     * Get All acquisition modes for specified mission
     *
     * @param missions the selected missions
     * @return list of acquisition modes
     * @throws SQLException .
     */
    public String[] getAcquisitionModes(final String[] missions) throws SQLException {
        if (missions == null || missions.length == 0)
            return new String[]{};
        String strMissionAcquisitionModes = "SELECT DISTINCT " + AbstractMetadata.ACQUISITION_MODE + " FROM " + TABLE + " WHERE ";
        strMissionAcquisitionModes += SQLUtils.getOrList(AbstractMetadata.MISSION, missions);

        final List<String> listEntries = new ArrayList<>();
        try (final Statement queryStatement = dbConnection.createStatement()) {
            final ResultSet results = queryStatement.executeQuery(strMissionAcquisitionModes);
            while (results.next()) {
                listEntries.add(results.getString(1));
            }
            return listEntries.toArray(new String[listEntries.size()]);
        }
    }
}