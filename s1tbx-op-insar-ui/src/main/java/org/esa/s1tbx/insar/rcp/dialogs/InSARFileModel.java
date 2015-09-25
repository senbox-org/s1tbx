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
package org.esa.s1tbx.insar.rcp.dialogs;

import org.esa.snap.graphbuilder.rcp.dialogs.support.BaseFileModel;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileTableModel;
import org.esa.snap.db.ProductEntry;

import java.io.File;

public class InSARFileModel extends BaseFileModel implements FileTableModel {

    protected void setColumnData() {
        titles = new String[]{
                "File Name", "Mst/Slv", "Acquisition", "Track", "Orbit",
                "Bperp [m]", "Btemp [days]",
                "Modeled Coherence",
                "Height Ambg [m]", "Delta fDC [Hz]"
        };

        types = new Class[]{
                String.class, String.class, String.class, String.class, String.class,
                String.class, String.class,
                String.class,
                String.class, String.class
        };

        widths = new int[]{
                35, 3, 20, 3, 3,
                5, 5,
                5,
                5, 5
        };
    }

    protected TableData createFileStats(final File file) {
        return new TableData(file);
    }

    protected TableData createFileStats(final ProductEntry entry) {
        return new TableData(entry);
    }
}