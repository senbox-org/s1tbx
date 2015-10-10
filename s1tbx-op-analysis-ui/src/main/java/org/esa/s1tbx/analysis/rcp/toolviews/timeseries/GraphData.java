/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.db.CommonReaders;

import java.awt.*;
import java.io.File;

/**
 * Graph content
 */
public class GraphData {

    private String title;
    private final File[] files;
    private final Color color;
    private final Product[] productList;

    public GraphData(final String title) {
        this.title = title;
        this.files = new File[]{};
        this.color = Color.BLACK;
        this.productList = null;
    }

    public GraphData(final String title, final File[] files, final Color color) {
        this.title = title;
        this.files = files;
        this.color = color;

        this.productList = readProducts(files);
    }

    private static Product[] readProducts(final File[] fileList) {
        final Product[] prodList = new Product[fileList.length];
        int i = 0;
        for (File file : fileList) {
            try {
                prodList[i] = CommonReaders.readProduct(file);
                ++i;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return prodList;
    }

    public String getTitle() {
        return title;
    }

    public Color getColor() {
        return color;
    }

    public File[] getFileList() {
        return files;
    }

    public Product[] getProducts() {
        return productList;
    }
}
