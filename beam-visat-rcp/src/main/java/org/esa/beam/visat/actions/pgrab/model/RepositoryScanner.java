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
package org.esa.beam.visat.actions.pgrab.model;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;

public class RepositoryScanner {


    public static void collectEntries(final Repository repository) {
        final File baseDir = repository.getBaseDir();
        final File[] productFiles = baseDir.listFiles(new ProductFileFilter());
        if (productFiles != null) {
            for (final File productFile : productFiles) {
                final RepositoryEntry repositoryEntry = new RepositoryEntry(productFile.getPath());
                repository.addEntry(repositoryEntry);
            }
        }
    }

    public static class ProductFileFilter implements FileFilter {

        public boolean accept(final File file) {
            final Iterator it = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

            while (it.hasNext()) {
                final ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();

                if (plugIn.getDecodeQualification(file) != DecodeQualification.UNABLE) {
                    return true;
                }
            }
            return false;

        }
    }

    public static class DirectoryFileFilter implements FileFilter {

        public boolean accept(final File file) {
            boolean isDataWithDim = file.getName().endsWith(".data") 
            && FileUtils.exchangeExtension(file, ".dim").exists();
            return file.isDirectory() && !isDataWithDim;
        }
    }
}
