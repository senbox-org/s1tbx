/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProcessorTest {

    static {
        TestUtils.initTestEnvironment();
    }

    protected File createTmpFolder(final String folderName) throws IOException {
        File folder = Files.createTempDirectory(folderName).toFile();
        folder.mkdirs();
        return folder;
    }

    protected List<Product> readProducts(final File folder) throws IOException {
        if(!folder.isDirectory()) {
            throw new IOException("Expecting " + folder + " to be a directory");
        }
        final File[] files = folder.listFiles();
        return readProducts(files);
    }

    protected List<Product> readProducts(final File[] files) throws IOException {
        final List<Product> productList = new ArrayList<>();
        if(files != null) {
            for(File file : files) {
                Product product = ProductIO.readProduct(file);
                if(product != null) {
                    productList.add(product);
                }
            }
        }
        return productList;
    }

    public static void delete(final File file) {
        FileUtils.deleteTree(file);
    }
}
