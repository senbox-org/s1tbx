/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Different types of {@link ProductLocation}.
 *
 * @author Marco Peters
 * @author Thomas Storm
 */
public enum ProductLocationType {

    FILE {
        @Override
        public Map<String, Product> findProducts(String path, ProgressMonitor pm) {
            final Map<String, Product> result = new HashMap<String, Product>();
            addProductToResult(result, new File(path));
            return result;
        }
    },
    DIRECTORY {
        @Override
        public Map<String, Product> findProducts(String path, ProgressMonitor pm) {
            final File[] files = listFiles(path);
            Map<String, Product> result = new HashMap<String, Product>();
            pm.beginTask("Scanning for products...", files.length);
            try {
                for (File file : files) {
                    if (!file.isDirectory()) {
                        addProductToResult(result, file);
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
            return result;
        }
    },
    DIRECTORY_REC {
        @Override
        public Map<String, Product> findProducts(String path, ProgressMonitor pm) {
            final File[] files = listFiles(path);
            Map<String, Product> result = new HashMap<String, Product>();
            pm.beginTask("Scanning for products...", files.length);
            try {
                for (File file : files) {
                    if (file.isDirectory()) {
                        result.putAll(findProducts(file.getPath(), new SubProgressMonitor(pm, 1)));
                    } else {
                        addProductToResult(result, file);
                    }
                }
            } finally {
                pm.done();
            }
            return result;
        }
    };

    abstract Map<String, Product> findProducts(String path, ProgressMonitor pm);

    private static void addProductToResult(Map<String, Product> result, File file) {
        try {
            final Product product = readSingleProduct(file);
            putIfNotNull(result, product);
        } catch (IOException e) {
            // ok; add nothing to result map.
        }
    }

    private static void putIfNotNull(Map<String, Product> result, Product product) {
        if (product != null) {
            result.put(product.getFileLocation().getAbsolutePath(), product);
        }
    }

    private static File[] listFiles(String path) {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The given path is not a directory: " + path);
        }
        return dir.listFiles();
    }

    private static Product readSingleProduct(File path) throws IOException {
        final Product product = ProductIO.readProduct(path);
        if (product == null) {
            return null;
        }

        if (product.getStartTime() != null) {
            return product;
        }

        final String productName = product.getName();
        try {
            final ProductData.UTC[] utcs = DateRangeParser.tryToGetDateRange(productName);
            product.setStartTime(utcs[0]);
            product.setEndTime(utcs[1]);
            return product;
        } catch (IllegalArgumentException e) {
            //todo inform the User with Popup Dialog
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Product '" + productName +
                                                                "' does not contain readable time information.", e);
            return null;
        }
    }
}
