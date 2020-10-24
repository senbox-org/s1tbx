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
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.rcp.SnapApp;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Graph content
 */
public class GraphData {

    private final String title;
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
        final List<Product> prodList = Collections.synchronizedList(new ArrayList<>());
        final ThreadExecutor executor = new ThreadExecutor();
        try {
            for (File file : fileList) {
                if (file.exists()) {
                    ThreadRunnable runnable = new ThreadRunnable() {
                        @Override
                        public void process() throws Exception {
                            Product product = getProductFromProductManager(file);
                            if (product == null) {
                                product = CommonReaders.readProduct(file);
                            }
                            if (product != null) {
                                prodList.add(product);
                            }
                        }
                    };
                    executor.execute(runnable);
                }
            }
            executor.complete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prodList.isEmpty() ? null : prodList.toArray(new Product[0]);
    }

    private static Product getProductFromProductManager(final File file) {
        final SnapApp app = SnapApp.getDefault();
        if (app != null) {
            final Product[] products = app.getProductManager().getProducts();
            for (Product p : products) {
                if (file.equals(p.getFileLocation())) {
                    return p;
                }
            }
        }
        return null;
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
