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

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.util.ProductFunctions;
import org.esa.snap.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**

 */
public class TestProductDao {

    private ProductDB db;

    @Before
    public void setUp() throws Exception {
        db = ProductDB.instance();
    }

    @Test
    public void testAddAll() throws Exception {

        final File folder1 = new File(TestUtils.rootPathASAR);
        if (!folder1.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        recurseProcessFolder(folder1, db);
    }

    public static void recurseProcessFolder(final File folder, final ProductDB db) throws SQLException {
        final File[] fileList = folder.listFiles(new ProductFunctions.ValidProductFileFilter(true));
        for (File file : fileList) {

            if (file.isDirectory()) {
                recurseProcessFolder(file, db);
            } else {

                if (db.pathExistsInDB(file))
                    continue;

                final ProductReader reader = ProductIO.getProductReaderForInput(file);
                if (reader != null) {
                    Product sourceProduct = null;
                    try {
                        sourceProduct = reader.readProductNodes(file, null);
                    } catch (Exception e) {
                        System.out.println("Unable to read " + file.getAbsolutePath());
                    }
                    if (sourceProduct != null) {
                        //System.out.println("Adding "+file.getAbsolutePath());

                        db.saveProduct(sourceProduct);
                        sourceProduct.dispose();
                    }
                }
            }
        }
    }

    @Test
    public void testListAll() throws SQLException {

        final ProductEntry[] list = db.getProductEntryList(false);
        for (ProductEntry entry : list) {
            //System.out.println(entry.getId() + " " + entry.getFile());
        }
    }

    @Test
    public void testGetAllMissions() throws SQLException {
        TestUtils.log.info("Missions:");
        final String[] missions = db.getAllMissions();
        for (String str : missions) {
            TestUtils.log.info(str);
        }
    }

    @Test
    public void testGetENVISATProductTypes() throws SQLException {
        TestUtils.log.info("ENVISAT productTypes:");
        final String[] productTypes = db.getProductTypes(new String[]{"ENVISAT"});
        for (String str : productTypes) {
            TestUtils.log.info(str);
        }
    }

    @Test
    public void testGetAllProductTypes() throws SQLException {
        TestUtils.log.info("All productTypes:");
        final String[] productTypes = db.getAllProductTypes();
        for (String str : productTypes) {
            TestUtils.log.info(str);
        }
    }

    @Test
    public void testSelect() throws SQLException {
        final String strGetProductsWhere = "SELECT * FROM " + ProductTable.TABLE + " WHERE MISSION='ENVISAT'";

        final Statement queryStatement = db.getConnection().createStatement();
        final ResultSet results = queryStatement.executeQuery(strGetProductsWhere);
    }

    @Test
    public void testRectIntersect() {
        Rectangle.Float a = new Rectangle.Float(-10, 10, 100, 100);
        Rectangle.Float b = new Rectangle.Float(-20, 20, 50, 50);

        boolean r1 = a.intersects(b);
        boolean r2 = b.intersects(a);

        System.out.println();
    }

}
