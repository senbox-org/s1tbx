package org.esa.nest.util;

import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.db.FileListSelection;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.List;

/**
 * Utils for using the clipboard
 */
public class ClipboardUtils {

    /**
     * Copies the given text to the system clipboard.
     * @param text the text to copy
     */
    public static void copyToClipboard(final String text) {
        final StringSelection selection = new StringSelection(text == null ? "" : text);
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, selection);
        } else {
            BeamLogManager.getSystemLogger().severe("failed to obtain clipboard instance");
        }
    }

    /**
     * Retrieves text from the system clipboard.
     * @return string
     */
    public static String getClipboardString() {
        try {
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard != null) {
                return (String)clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
        return null;
    }

    /**
     * Copies the given file list to the system clipboard.
     * @param fileList the list to copy
     */
    public static void copyToClipboard(final File[] fileList) {
        final FileListSelection selection = new FileListSelection(fileList);
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, selection);
        } else {
            BeamLogManager.getSystemLogger().severe("failed to obtain clipboard instance");
        }
    }

    /**
     * Retrieves a list of files from the system clipboard.
     * @return file[] list
     */
    public static File[] getClipboardFileList() {
        try {
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard != null) {
                final List<File> fileList = (List<File>)clipboard.getData(DataFlavor.javaFileListFlavor);
                return fileList.toArray(new File[fileList.size()]);
            }
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
        return new File[0];
    }
}
