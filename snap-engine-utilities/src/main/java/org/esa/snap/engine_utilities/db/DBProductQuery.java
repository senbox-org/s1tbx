/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.util.SystemUtils;

import java.sql.SQLException;

/**
 * Search interface for Products from a database
 */
public class DBProductQuery implements ProductQueryInterface {

    private static DBProductQuery instance;

    private ProductEntry[] productEntryList = null;
    private ProductDB db;
    private static final String[] emptyStringList = new String[] {};

    private DBProductQuery() {

    }

    public static DBProductQuery instance() {
        if(instance == null) {
            instance = new DBProductQuery();
        }
        return instance;
    }

    public boolean isReady() {
        return db != null && db.isReady();
    }

    public ProductDB getDB() {
        return db;
    }

    public boolean partialQuery(final DBQuery dbQuery) throws Exception {
        return fullQuery(dbQuery, ProgressMonitor.NULL);
    }

    public boolean fullQuery(final DBQuery dbQuery, final ProgressMonitor pm) throws Exception {
        if (db == null) {
            db = ProductDB.instance();
        }

        if (productEntryList != null) {
            ProductEntry.dispose(productEntryList);
        }
        if (db.isReady()) {
            productEntryList = dbQuery.queryDatabase(db);
            return true;
        }
        return false;
    }

    public ProductEntry[] getProductEntryList() {
        return productEntryList;
    }

    public String[] getAllMissions() {
        try {
            if (db != null) {
                return db.getAllMissions();
            }
        } catch (SQLException e) {
            SystemUtils.LOG.severe("Error getting missions from database " + e.getMessage());
        }
        return emptyStringList;
    }

    public String[] getAllProductTypes(final String[] missions) {
        try {
            if (db != null) {
                return db.getProductTypes(missions);
            }
        } catch (SQLException e) {
            SystemUtils.LOG.severe("Error getting product types from database " + e.getMessage());
        }
        return emptyStringList;
    }

    public String[] getAllAcquisitionModes(final String[] missions) {
        try {
            if (db != null) {
                return db.getAcquisitionModes(missions);
            }
        } catch (SQLException e) {
            SystemUtils.LOG.severe("Error getting acquisition modes from database " + e.getMessage());
        }
        return emptyStringList;
    }
}
