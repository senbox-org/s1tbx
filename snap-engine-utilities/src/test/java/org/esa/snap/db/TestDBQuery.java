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

import junit.framework.TestCase;
import org.esa.snap.datamodel.AbstractMetadata;

import java.sql.SQLException;


/**

 */
public class TestDBQuery extends TestCase {

    private ProductDB db;

    public TestDBQuery(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

        db = ProductDB.instance();
    }

    public void tearDown() throws Exception {
        super.tearDown();

        ProductDB.deleteInstance();
    }

    public void testQuery() throws SQLException {
        final DBQuery dbQuery = new DBQuery();
        dbQuery.setSelectedMissions(new String[]{"ENVISAT"});

        final ProductEntry[] productEntryList = dbQuery.queryDatabase(db);
        showProductEntries(productEntryList);
    }

    public void testFreeQuery() throws SQLException {
        final DBQuery dbQuery = new DBQuery();
        dbQuery.setFreeQuery(AbstractMetadata.PRODUCT + " LIKE 'RS2_SGF%'");

        final ProductEntry[] productEntryList = dbQuery.queryDatabase(db);
        showProductEntries(productEntryList);
    }

    private static void showProductEntries(final ProductEntry[] productEntryList) {
        for (ProductEntry entry : productEntryList) {
            //System.out.println(entry.getId() +" "+ entry.getName());
        }
    }
}