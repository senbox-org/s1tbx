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
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gpf.ThreadManager;
import org.esa.snap.util.ProductFunctions;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans folders for products to add or update into the database
 */
public final class DBScanner extends SwingWorker {
    private final ProductDB db;

    private final File baseDir;
    private final boolean doRecursive;
    private final boolean generateQuicklooks;
    private final com.bc.ceres.core.ProgressMonitor pm;
    private final List<DBScannerListener> listenerList = new ArrayList<>(1);
    private final List<ErrorFile> errorList = new ArrayList<>();

    public DBScanner(final ProductDB database, final File baseDir, final boolean doRecursive,
                     final boolean doQuicklooks, final com.bc.ceres.core.ProgressMonitor pm) {
        this.db = database;
        this.pm = pm;
        this.baseDir = baseDir;
        this.doRecursive = doRecursive;
        this.generateQuicklooks = doQuicklooks;
    }

    public void addListener(final DBScannerListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final DBScannerListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final DBScannerListener.MSG msg) {
        for (final DBScannerListener listener : listenerList) {
            listener.notifyMSG(this, msg);
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        errorList.clear();

        final List<File> dirList = new ArrayList<>(20);
        dirList.add(baseDir);
        if (doRecursive) {
            final File[] subDirs = collectAllSubDirs(baseDir);
            dirList.addAll(Arrays.asList(subDirs));
        }

        final ProductFunctions.ValidProductFileFilter fileFilter = new ProductFunctions.ValidProductFileFilter(false);
        final List<File> fileList = new ArrayList<>(dirList.size());
        for (File file : dirList) {
            fileList.addAll(Arrays.asList(file.listFiles(fileFilter)));
        }

        final List<File> qlProductFiles = new ArrayList<>(fileList.size());
        final List<Integer> qlIDs = new ArrayList<>(fileList.size());

        final ProductEntry[] entriesInPath = db.getProductEntryInPath(baseDir);
        final Map<File, ProductEntry> fileMap = new ConcurrentHashMap<>(entriesInPath.length);
        for (ProductEntry entry : entriesInPath) {
            fileMap.put(entry.getFile(), entry);
        }

        final int total = fileList.size();
        pm.beginTask("Scanning Files...", total);
        int i = 0;
        int prodCount = 0;
        try {
            for (File file : fileList) {
                ++i;
                String taskMsg = "Scanning " + i + " of " + total + " files ";
                if (prodCount > 0)
                    taskMsg += "(" + prodCount + " new products)";
                pm.setTaskName(taskMsg);
                pm.worked(1);

                // check if already exists in db
                //final ProductEntry existingEntry = db.getProductEntry(file);
                final ProductEntry existingEntry = fileMap.get(file);

                if (existingEntry != null) {
                    // check for missing quicklook
                    if (generateQuicklooks && !existingEntry.quickLookExists()) {
                        qlProductFiles.add(file);
                        qlIDs.add(existingEntry.getId());
                    }
                    existingEntry.dispose();
                    continue;
                }

                if (pm.isCanceled())
                    break;

                try {
                    // quick test for common readers
                    final Product sourceProduct = ProductIO.readProduct(file);
                    if (sourceProduct != null) {
                        final ProductEntry entry = db.saveProduct(sourceProduct);
                        ++prodCount;
                        if (!entry.quickLookExists()) {
                            qlProductFiles.add(file);
                            qlIDs.add(entry.getId());
                        }
                        sourceProduct.dispose();
                        entry.dispose();
                    } else if (!file.isDirectory()) {
                        System.out.println("No reader for " + file.getAbsolutePath());
                    }
                } catch (Throwable e) {
                    errorList.add(new ErrorFile(file, ErrorFile.UNREADABLE));
                    System.out.println("Unable to read " + file.getAbsolutePath() + '\n' + e.getMessage());
                }
            }

            db.cleanUpRemovedProducts();

            notifyMSG(DBScannerListener.MSG.FOLDERS_SCANNED);

            if (generateQuicklooks) {
                final int numQL = qlProductFiles.size();
                pm.beginTask("Generating Quicklooks...", numQL);
                final ThreadManager threadManager = new ThreadManager();

                for (int j = 0; j < numQL; ++j) {
                    pm.setTaskName("Generating Quicklook... " + (j + 1) + " of " + numQL);
                    pm.worked(1);
                    if (pm.isCanceled())
                        break;

                    final File file = qlProductFiles.get(j);
                    final int qlID = qlIDs.get(j);

                    final Thread worker = new Thread() {

                        @Override
                        public void run() {
                            try {
                                boolean isCorrupt = QuickLookGenerator.createQuickLook(qlID, file);
                                if (isCorrupt) {
                                    errorList.add(new ErrorFile(file, ErrorFile.CORRUPT));
                                }
                            } catch (Throwable e) {
                                System.out.println("QL Unable to read " + file.getAbsolutePath() + '\n' + e.getMessage());
                            }
                        }
                    };
                    threadManager.add(worker);

                    notifyMSG(DBScannerListener.MSG.QUICK_LOOK_GENERATED);
                }
                threadManager.finish();
            }
            pm.setTaskName("");

        } catch (Throwable e) {
            System.out.println("Scanning Exception\n" + e.getMessage());
        } finally {
            pm.done();
        }
        return true;
    }

    @Override
    public void done() {
        notifyMSG(DBScannerListener.MSG.DONE);
    }

    private static File[] collectAllSubDirs(final File dir) {
        final List<File> dirList = new ArrayList<>(20);
        final ProductFunctions.DirectoryFileFilter dirFilter = new ProductFunctions.DirectoryFileFilter();

        final File[] subDirs = dir.listFiles(dirFilter);
        for (final File subDir : subDirs) {
            dirList.add(subDir);
            final File[] dirs = collectAllSubDirs(subDir);
            dirList.addAll(Arrays.asList(dirs));
        }
        return dirList.toArray(new File[dirList.size()]);
    }

    public List<ErrorFile> getErrorList() {
        return errorList;
    }

    public static class ErrorFile {
        public final File file;
        public final String message;
        public final static String CORRUPT = "Corrupt Image";
        public final static String UNREADABLE = "Product unreadable";

        public ErrorFile(final File file, final String msg) {
            this.file = file;
            this.message = msg;
        }
    }

    public interface DBScannerListener {

        public enum MSG {DONE, FOLDERS_SCANNED, QUICK_LOOK_GENERATED}

        public void notifyMSG(final DBScanner dbScanner, final MSG msg);
    }
}
