package org.esa.nest.dat.utils;

import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * To be removed
 */
public class FileFolderUtils {

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave) {
        return GetFilePath(title, formatName, extension, fileName, description, isSave,
                BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR,
                FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetSaveFilePath(final String title, final String formatName, final String extension,
                                       final String fileName, final String description) {
        return GetFilePath(title, formatName, extension, fileName, description, true,
                BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR,
                FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave,
                                   final String lastDirPropertyKey, final String defaultPath) {
        BeamFileFilter fileFilter = null;
        if (!extension.isEmpty()) {
            fileFilter = new BeamFileFilter(formatName, extension, description);
        }
        File file;
        if (isSave) {
            file = VisatApp.getApp().showFileSaveDialog(title, false, fileFilter, '.' + extension, fileName,
                    lastDirPropertyKey);
        } else {
            String lastDir = VisatApp.getApp().getPreferences().getPropertyString(lastDirPropertyKey, defaultPath);
            if (fileName == null)
                file = showFileOpenDialog(title, false, fileFilter, lastDir, lastDirPropertyKey);
            else
                file = showFileOpenDialog(title, false, fileFilter, fileName, lastDirPropertyKey);
        }

        return file == null ? null : FileUtils.ensureExtension(file, extension);
    }

    /**
     * allows the choice of picking directories only
     *
     * @param title
     * @param dirsOnly
     * @param fileFilter
     * @param currentDir
     * @param lastDirPropertyKey
     * @return
     */
    private static File showFileOpenDialog(String title,
                                           boolean dirsOnly,
                                           FileFilter fileFilter,
                                           String currentDir,
                                           String lastDirPropertyKey) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(new File(currentDir));
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(VisatApp.getApp().getAppName() + " - " + title);
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(VisatApp.getApp().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            final String lastDirPath = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (lastDirPath != null) {
                VisatApp.getApp().getPreferences().setPropertyString(lastDirPropertyKey, lastDirPath);
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().isEmpty()) {
                return null;
            }
            return file.getAbsoluteFile();
        }
        return null;
    }
}
