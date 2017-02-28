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

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileDeleteStrategy;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 */
public class ProductDB extends DAO {

    private ProductTable productTable;
    private MetadataTable metadataTable;

    private static ProductDB _instance = null;
    public static final String DEFAULT_PRODUCT_DATABASE_NAME = "productDB";

    private static final String strGetProductsWhere =
            "SELECT * FROM " + ProductTable.TABLE + ", " + MetadataTable.TABLE + " WHERE " + ProductTable.TABLE + ".ID = " + MetadataTable.TABLE + ".ID AND ";

    public static ProductDB instance() throws Exception {
        if (_instance == null) {
            _instance = createDefaultInstance();
            initializeInstance();
        }
        return _instance;
    }

    private static ProductDB createDefaultInstance() throws IOException {
        final Properties properties = new Properties();
        properties.put("user","nestuser");
        properties.put("password","snapuser");
        properties.put("derby.driver","org.apache.derby.jdbc.EmbeddedDriver");
        properties.put("derby.url","jdbc:derby:");
        properties.put("db.table","PRODUCTS");
        properties.put("db.schema","APP");

        return new ProductDB(properties);
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

            _instance = createDefaultInstance();
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

    private ProductDB(final Properties dbProperties) throws IOException {
        super(DEFAULT_PRODUCT_DATABASE_NAME, dbProperties);
    }

    public boolean isReady() {
        return productTable != null;
    }

    @Override
    protected boolean createTables(final Connection connection) throws SQLException {
        productTable = new ProductTable(connection);
        productTable.createTable();
        metadataTable = new MetadataTable(connection);
        metadataTable.createTable();
        return true;
    }

    @Override
    protected void validateTables(final Connection connection) throws SQLException {
        if (productTable == null)
            productTable = new ProductTable(connection);
        if (metadataTable == null)
            metadataTable = new MetadataTable(connection);
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

    public void cleanUpRemovedProducts(final ProgressMonitor pm) throws SQLException {
        final DBQuery dbQuery = new DBQuery();
        final ProductEntry[] entries = dbQuery.queryDatabase(this);
        pm.beginTask("Cleaning up database...", entries.length);
        for (ProductEntry entry : entries) {
            if (!entry.getFile().exists()) {
                deleteProductEntry(entry);
            }
            pm.worked(1);
        }
        pm.done();
    }

    public void deleteProductEntry(final ProductEntry entry) throws SQLException {
        deleteRecord(entry.getId());
    }

    private void deleteRecord(final int id) throws SQLException {
        productTable.deleteRecord(id);
        metadataTable.deleteRecord(id);
        //QuickLookGenerator.deleteQuickLook(id);
    }

    public void removeProducts(final File baseDir, final ProgressMonitor pm) throws SQLException {
        final String queryStr = AbstractMetadata.PATH + " LIKE '" + baseDir.getAbsolutePath() + "%'";
        final ProductEntry[] list = queryProduct(queryStr);
        pm.beginTask("Removing products from database...", list.length);
        for (ProductEntry entry : list) {
            if(pm.isCanceled())
                break;

            deleteProductEntry(entry);
            pm.worked(1);
        }
        pm.done();
    }

    public void removeAllProducts(final ProgressMonitor pm) throws SQLException {
        final String queryStr = "";
        final ProductEntry[] list = queryProduct(queryStr);
        pm.beginTask("Removing products from database...", list.length);
        for (ProductEntry entry : list) {
            if(pm.isCanceled())
                break;

            deleteProductEntry(entry);
            pm.worked(1);
        }
        pm.done();
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

        try (final Statement queryStatement = dbConnect.createStatement()) {
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
        return missions == null ? productTable.getAllProductTypes() : productTable.getProductTypes(missions);
    }

    public String[] getAllAcquisitionModes() throws SQLException {
        return productTable.getAllAcquisitionModes();
    }

    public String[] getAcquisitionModes(final String[] missions) throws SQLException {
        return missions == null ? productTable.getAllAcquisitionModes() : productTable.getAcquisitionModes(missions);
    }

    public String[] getMetadataNames() {
        return metadataTable.getAllMetadataNames();
    }
}
