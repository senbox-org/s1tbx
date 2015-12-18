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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

class Sen3ExpAtmosphericConverter {

    public static void main(String[] args) {
        final String sourceDirPath = args[0];
        final File sourceDir = new File(sourceDirPath);

        final String targetDirPath = args[1];
        final File targetDir = new File(targetDirPath);

        final String propertyFilePath = args[2];
        final File propertyFile = new File(propertyFilePath);

        convert(sourceDir, targetDir, propertyFile, new ErrorHandler() {
            @Override
            public void error(Throwable t) {
                System.out.println("Error: " + t.getMessage());
                System.exit(1);
            }

            @Override
            public void warning(Throwable t) {
                System.out.println("Warning: " + t.getMessage());
            }
        });
    }

    private static void convert(File sourceDir, File targetDir, File propertyFile, ErrorHandler errorHandler) {
        final Properties properties = new Properties();
        try {
            try (FileInputStream inStream = new FileInputStream(propertyFile)) {
                properties.load(inStream);
            }
        } catch (IOException e) {
            errorHandler.error(e);
        }

        final File[] sourceFiles = sourceDir.listFiles((dir, name) -> {
            return name.endsWith(".nc");
        });

        for (final File sourceFile : sourceFiles) {
            Product product = null;
            try {
                product = ProductIO.readProduct(sourceFile);
                if (product != null) {
                    for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                        final Band band = product.getBand((String) entry.getKey());
                        if (band != null) {
                            band.setSpectralWavelength(Float.parseFloat((String) entry.getValue()));
                        }
                    }
                    ProductIO.writeProduct(product, new File(targetDir, sourceFile.getName()),
                                           "NetCDF", false);
                }
            } catch (IOException e) {
                errorHandler.warning(e);
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        }
    }

    interface ErrorHandler {

        void error(Throwable t);

        void warning(Throwable t);
    }
}
