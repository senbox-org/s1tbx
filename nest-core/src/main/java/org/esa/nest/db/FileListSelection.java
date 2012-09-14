package org.esa.nest.db;

import com.bc.ceres.swing.selection.AbstractSelection;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
   File list selection
 */
public class FileListSelection extends AbstractSelection implements Transferable  {

    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
	    DataFlavor.javaFileListFlavor
    };

    private final List<File> fileList = new ArrayList<File>();

    public FileListSelection(File[] fileList) {
        this.fileList.addAll(Arrays.asList(fileList));
    }

    @Override
    public File getSelectedValue() {
        return fileList.get(0);
    }

    @Override
    public File[] getSelectedValues() {
        return fileList.toArray(new File[fileList.size()]);
    }

    /**
     * Returns an array of flavors in which this <code>Transferable</code>
     * can provide the data. <code>DataFlavor.stringFlavor</code>
     * @return an array of flavors
     */
    public DataFlavor[] getTransferDataFlavors() {
	    return flavors;
    }

    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        for (DataFlavor f : flavors) {
            if (flavor.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the <code>Transferable</code>'s data in the requested <code>DataFlavor</code> if possible.
     * @param flavor the requested flavor for the data
     * @return the data in the requested flavor, as outlined above
     * @throws java.awt.datatransfer.UnsupportedFlavorException if the requested data flavor not supported
     */
    public Object getTransferData(final DataFlavor flavor)
        throws UnsupportedFlavorException
    {
        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
            return fileList;
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
            return null;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}