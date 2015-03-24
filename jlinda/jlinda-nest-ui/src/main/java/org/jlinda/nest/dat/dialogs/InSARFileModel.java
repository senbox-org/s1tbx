package org.jlinda.nest.dat.dialogs;

import org.esa.snap.dat.dialogs.BaseFileModel;
import org.esa.snap.dat.dialogs.FileTableModel;
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