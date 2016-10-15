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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Base Data Access Object
 */
public abstract class DAO {

    private boolean isConnected;
    private final Properties dbProperties;
    private final String dbName;
    protected Connection dbConnect = null;
    protected SQLException lastSQLException = null;

    public DAO(final String name, final Properties dbProperties) throws IOException {
        this.dbName = name;
        this.dbProperties = dbProperties;

        setDBSystemDir();

        loadDatabaseDriver(dbProperties.getProperty("derby.driver"));
        if (!dbExists()) {
            if (!createDatabase()) {
                throw new IOException("Unable to create tables\n" + getLastSQLException().getMessage());
            }
        }
    }

    private boolean dbExists() {
        final String dbLocation = getDatabaseLocation();
        final File dbFileDir = new File(dbLocation);
        return dbFileDir.exists();
    }

    private static void setDBSystemDir() {
        // create the db system directory
        final File fileSystemDir = getDBSystemDir();
        if(!fileSystemDir.exists()) {
            fileSystemDir.mkdir();
        }
        // decide on the db system directory
        System.setProperty("derby.system.home", fileSystemDir.getAbsolutePath());
    }

    public static File getDBSystemDir() {
        return new File(SystemUtils.getApplicationDataDir(true), "productDB");
    }

    private static void loadDatabaseDriver(final String driverName) {
        // load Derby driver
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException ex) {
            SystemUtils.LOG.severe("Unable to load database: " + ex.getMessage());
        }
    }

    protected abstract boolean createTables(final Connection dbConnection) throws SQLException;

    protected abstract void validateTables(final Connection dbConnection) throws SQLException;

    protected abstract void prepareStatements() throws SQLException;

    private boolean createDatabase() {
        boolean bCreated = false;
        dbProperties.put("create", "true");

        try {
            dbConnect = DriverManager.getConnection(getDatabaseUrl(), dbProperties);
            bCreated = createTables(dbConnect);
        } catch (SQLException ex) {
            SystemUtils.LOG.severe("Unable to create database: " + ex.getMessage());
            lastSQLException = ex;
        }
        dbProperties.remove("create");
        return bCreated;
    }

    private void validateDatabase(final Connection dbConnection) {
        dbProperties.put("create", "true");

        try {
            validateTables(dbConnection);
        } catch (SQLException ex) {
            SystemUtils.LOG.severe("Database validation error: " + ex.getMessage());
            lastSQLException = ex;
        }
        dbProperties.remove("create");
    }

    protected boolean connect() {
        if (isConnected) return isConnected;

        try {
            if (dbConnect == null)
                dbConnect = DriverManager.getConnection(getDatabaseUrl(), dbProperties);

            validateDatabase(dbConnect);
            prepareStatements();

            isConnected = dbConnect != null;
        } catch (SQLException ex) {
            SystemUtils.LOG.severe("Unable to connect to database: " + ex.getMessage());
            isConnected = false;
            lastSQLException = ex;
        }
        return isConnected;
    }

    protected void disconnect() {
        if (isConnected) {
            dbProperties.put("shutdown", "true");
            try (Connection connection = DriverManager.getConnection(getDatabaseUrl(), dbProperties)){

            } catch (SQLException ex) {
                lastSQLException = ex;
            }
            isConnected = false;
            dbProperties.remove("shutdown");
        }
      /*  try {
            final Driver drv = DriverManager.getDriver(getDatabaseUrl());
            DriverManager.deregisterDriver(drv);
        } catch (SQLException ex) {
                lastSQLException = ex;
        }   */
    }

    public Connection getConnection() {
        return dbConnect;
    }

    public SQLException getLastSQLException() {
        return lastSQLException;
    }

    public String getDatabaseLocation() {
        return System.getProperty("derby.system.home") + File.separator + dbName;
    }

    public String getDatabaseUrl() {
        return dbProperties.getProperty("derby.url") + dbName;
    }

}