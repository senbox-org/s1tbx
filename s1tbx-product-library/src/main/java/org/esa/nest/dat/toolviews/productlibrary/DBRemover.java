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
package org.esa.nest.dat.toolviews.productlibrary;

import org.esa.snap.db.ProductDB;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * removes repositories from the database
 */
public final class DBRemover extends SwingWorker {
    private final ProductDB db;

    private final File baseDir;
    private final com.bc.ceres.core.ProgressMonitor pm;
    private final List<DBRemoverListener> listenerList = new ArrayList<>(1);

    /**
     *
     * @param database the database
     * @param baseDir the basedir to remove. If null, all entries will be removed
     * @param pm the progress monitor
     */
    public DBRemover(final ProductDB database, final File baseDir, final com.bc.ceres.core.ProgressMonitor pm) {
        this.db = database;
        this.pm = pm;
        this.baseDir = baseDir;
    }

    public void addListener(final DBRemoverListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyMSG(final DBRemoverListener.MSG msg) {
        for (final DBRemoverListener listener : listenerList) {
            listener.notifyMSG(msg);
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {

        try {
            if(baseDir == null) {
                db.removeAllProducts(pm);
            } else {
                db.removeProducts(baseDir, pm);
            }
        } catch (Throwable e) {
            System.out.println("Product Removal Exception\n" + e.getMessage());
        } finally {
            pm.done();
        }
        return true;
    }

    @Override
    public void done() {
        notifyMSG(DBRemoverListener.MSG.DONE);
    }

    public interface DBRemoverListener {

        public enum MSG {DONE}

        public void notifyMSG(final MSG msg);
    }
}
