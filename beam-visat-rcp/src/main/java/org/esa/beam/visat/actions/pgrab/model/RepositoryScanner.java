/*
 * $Id: RepositoryScanner.java,v 1.1 2007/04/19 10:28:49 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
