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
package org.esa.snap.db;

import org.apache.commons.io.FileDeleteStrategy;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.datamodel.AbstractMetadata;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProductDB extends DAO {

    private ProductTable productTable;
    private MetadataTable metadataTable;
    private Connection dbConnection = null;

    private static ProductDB _instance = null;
    public static final String DEFAULT_PRODUCT_DATABASE_NAME = "productDB";

    private static final String strGetProductsWhere =
            "SELECT * FROM " + ProductTable.TABLE + ", " + MetadataTable.TABLE + " WHERE " + ProductTable.TABLE + ".ID = " + MetadataTable.TABLE + ".ID AND ";

    public static ProductDB instance() throws Exception {
        if (_instance == null) {
            _instance = new ProductDB();
            initializeInstance();
        }
        return _instance;
    }

    public static ProductDB testInstance(final File dbPropertiesFile) throws Exception {
        if (_instance == null) {
            _instance = new ProductDB(dbPropertiesFile);
            initializeInstance();
        }
        return _instance;
    }

    private static void initializeInstance() throws Exception {
        boolean connected = _instance.connect();
        if (!connected) {
            final String dbLocation = _instance.getDatabaseLocation();
            deleteInstance();

            final File dbFolder = new File(dbLocation);
            final File qlFolder = new File(dbFolder.getParentFile(), "QuickLooks");
            boolean deleted = FileDeleteStrategy.FORCE.deleteQuietly(dbFolder);
            deleted = FileDeleteStrategy.FORCE.deleteQuietly(qlFolder);

            _instance = new ProductDB();
            connected = _instance.connect();
            if (!connected) {
                throw new Exception("Unable to connect to database\n" + _instance.getLastSQLException().getMessage());
            }
        }
    }

    public static void deleteInstance() {
        _instance.disconnect();
        _instance = null;
    }

    private ProductDB() throws IOException {
        super(DEFAULT_PRODUCT_DATABASE_NAME);
    }

    private ProductDB(final File dbPropertiesFile) throws IOException {
        super(DEFAULT_PRODUCT_DATABASE_NAME, dbPropertiesFile);
    }

    public boolean isReady() {
        return productTable != null;
    }

    @Override
    protected boolean createTables(final Connection connection) throws SQLException {
        this.dbConnection = connection;
        productTable = new ProductTable(dbConnection);
        productTable.createTable();
        metadataTable = new MetadataTable(dbConnection);
        metadataTable.createTable();
        return true;
    }

    @Override
    protected void validateTables(final Connection connection) throws SQLException {
        this.dbConnection = connection;
        if (productTable == null)
            productTable = new ProductTable(dbConnection);
        if (metadataTable == null)
            metadataTable = new MetadataTable(dbConnection);
        productTable.validateTable();
        metadataTable.validateTable();
    }

    protected void prepareStatements() throws SQLException {
        productTable.prepareStatements();
        metadataTable.prepareStatements();
    }

    public boolean pathExistsInDB(final File path) throws SQLException {
        return productTable.pathExists(path);
    }

    public ProductEntry getProductEntry(final File path) throws SQLException {
        return productTable.getProductEntry(path);
    }

    public ProductEntry[] getProductEntryInPath(final File baseDir) throws SQLException {
        final String queryStr = AbstractMetadata.PATH + " LIKE '" + baseDir.getAbsolutePath() + "%'";
        return queryProduct(queryStr);
    }

    public MetadataElement getProductMetadata(final int id) throws SQLException {
        return metadataTable.getProductMetadata(id);
    }

    public ProductEntry saveProduct(final Product product) throws SQLException {
        final ProductEntry newEntry = new ProductEntry(product);

        if (productTable.pathExists(newEntry.getFile())) {
            // update
        } else {
            addRecord(newEntry);
        }
        return newEntry;
    }

    private void addRecord(final ProductEntry record) throws SQLException {

        final ResultSet results = productTable.addRecord(record);
        if (results.next()) {
            final int id = results.getInt(1);
            record.setId(id);

            metadataTable.addRecord(record);
        }
    }

    public void cleanUpRemovedProducts() throws SQLException {
        final DBQuery dbQuery = new DBQuery();
        final ProductEntry[] entries = dbQuery.queryDatabase(this);
        for (ProductEntry entry : entries) {
            if (!entry.getFile().exists()) {
                deleteProductEntry(entry);
            }
        }
    }

    public void deleteProductEntry(final ProductEntry entry) throws SQLException {
        deleteRecord(entry.getId());
    }

    private void deleteRecord(final int id) throws SQLException {
        productTable.deleteRecord(id);
        metadataTable.deleteRecord(id);
        QuickLookGenerator.deleteQuickLook(id);
    }

    public void removeProducts(final File baseDir) throws SQLException {
        final String queryStr = AbstractMetadata.PATH + " LIKE '" + baseDir.getAbsolutePath() + "%'";
        final ProductEntry[] list = queryProduct(queryStr);
        for (ProductEntry entry : list) {
            deleteProductEntry(entry);
        }
    }

    public void removeAllProducts() throws SQLException {
        final String queryStr = "";
        final ProductEntry[] list = queryProduct(queryStr);
        for (ProductEntry entry : list) {
            deleteProductEntry(entry);
        }
    }

    public ProductEntry[] getProductEntryList(final boolean validate) throws SQLException {
        if (!validate)
            return productTable.getProductEntryList();

        final ProductEntry[] entries = productTable.getProductEntryList();
        final List<ProductEntry> list = new ArrayList<>(entries.length);
        for (ProductEntry entry : entries) {
            if (entry.getFile().exists()) {
                list.add(entry);
            } else {
                deleteRecord(entry.getId());
            }
        }
        return list.toArray(new ProductEntry[list.size()]);
    }

    public ProductEntry[] queryProduct(final String queryStr) throws SQLException {
        final List<ProductEntry> listEntries = new ArrayList<>();

        final Statement queryStatement = dbConnection.createStatement();
        String whereStr = strGetProductsWhere;
        if (queryStr.isEmpty()) {
            whereStr = strGetProductsWhere.substring(0, strGetProductsWhere.lastIndexOf(" AND "));
        }
        final ResultSet results = queryStatement.executeQuery(whereStr + queryStr);
        while (results.next()) {
            listEntries.add(new ProductEntry(results));
        }
        return listEntries.toArray(new ProductEntry[listEntries.size()]);
    }

    public String[] getAllMissions() throws SQLException {
        if (productTable == null) {
            return new String[]{};
        }
        return productTable.getAllMissions();
    }

    public String[] getAllProductTypes() throws SQLException {
        return productTable.getAllProductTypes();
    }

    public String[] getProductTypes(final String[] missions) throws SQLException {
        return productTable.getProductTypes(missions);
    }

    public String[] getAllAcquisitionModes() throws SQLException {
        return productTable.getAllAcquisitionModes();
    }

    public String[] getAcquisitionModes(final String[] missions) throws SQLException {
        return productTable.getAcquisitionModes(missions);
    }

    public String[] getMetadataNames() {
        return metadataTable.getAllMetadataNames();
    }
}
