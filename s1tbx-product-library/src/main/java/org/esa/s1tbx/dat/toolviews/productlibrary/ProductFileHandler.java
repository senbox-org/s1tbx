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
package org.esa.s1tbx.dat.toolviews.productlibrary;

import org.esa.snap.dataio.dimap.DimapProductConstants;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.util.FileIOUtils;
import org.esa.snap.util.io.FileUtils;

import javax.swing.SwingWorker;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.*;

/**
 * Handle product files
 */
public class ProductFileHandler extends SwingWorker {

    private static final String[] singleFileExt = {"n1", "e1", "e2", "tif", "tiff", "zip"};
    private static final String[] folderExt = {"safe"};
    private static final String[] folderMissions = {"RS2", "TSX", "TDX", "CSKS1", "CSKS2", "CSKS3", "CSKS4",
            "ALOS", "JERS1", "RS1"};

    public enum TYPE { COPY_TO, MOVE_TO, DELETE }

    private final ProductEntry[] entries;
    private final TYPE operationType;
    private final File targetFolder;
    private final com.bc.ceres.core.ProgressMonitor pm;
    private final List<ProductFileHandlerListener> listenerList = new ArrayList<>(1);
    private final List<DBScanner.ErrorFile> errorList = new ArrayList<>();

    public ProductFileHandler(final ProductEntry[] entries, final TYPE operationType, final File targetFolder,
                              final com.bc.ceres.core.ProgressMonitor pm) {
        this.entries = entries;
        this.operationType = operationType;
        this.targetFolder = targetFolder;
        this.pm = pm;
    }

    public TYPE getOperationType() {
        return operationType;
    }

    public void addListener(final ProductFileHandlerListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyMSG(final ProductFileHandlerListener.MSG msg) {
        for (final ProductFileHandlerListener listener : listenerList) {
            listener.notifyMSG(this, msg);
        }
    }

    private String getOperationStr() {
        if(operationType.equals(TYPE.COPY_TO)) {
            return "Copying";
        } else if(operationType.equals(TYPE.MOVE_TO)) {
            return "Moving";
        } else if(operationType.equals(TYPE.DELETE)) {
            return "Deleting";
        }
        return "";
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        errorList.clear();

        try {
            pm.beginTask(getOperationStr()+" products...", entries.length);
            for (ProductEntry entry : entries) {
                if (pm.isCanceled())
                    break;
                try {
                    if(operationType.equals(TYPE.COPY_TO)) {
                        ProductFileHandler.copyTo(entry, targetFolder);
                    } else if(operationType.equals(TYPE.MOVE_TO)) {
                        ProductFileHandler.moveTo(entry, targetFolder);
                    } else if(operationType.equals(TYPE.DELETE)) {
                        ProductFileHandler.delete(entry);
                    }
                    pm.worked(1);
                } catch (Exception e) {
                    errorList.add(new DBScanner.ErrorFile(entry.getFile(), getOperationStr()+" file failed: " + e.getMessage()));
                }
            }

        } catch (Throwable e) {
            System.out.println("File Handling Exception\n" + e.getMessage());
        } finally {
            pm.done();
        }
        return true;
    }

    @Override
    public void done() {
        notifyMSG(ProductFileHandlerListener.MSG.DONE);
    }

    public List<DBScanner.ErrorFile> getErrorList() {
        return errorList;
    }

    public interface ProductFileHandlerListener {

        public enum MSG {DONE}

        public void notifyMSG(final ProductFileHandler fileHandler, final MSG msg);
    }

    public static boolean canMove(final ProductEntry entry) {
        return isDimap(entry) || isFolderProduct(entry) || isSingleFile(entry) || isSMOS(entry);
    }

    private static void copyTo(final ProductEntry entry, final File targetFolder) throws Exception {

        if (isSingleFile(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.copy(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING);
        } else if (isDimap(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.copy(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING);
            final String dataFolderName = entry.getFile().getName().replace(
                    DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION,
                    DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
            final File oldDataFolder = new File(entry.getFile().getParentFile(), dataFolderName);
            final File newDataFolder = new File(targetFolder, dataFolderName);
            FileIOUtils.copyFolder(oldDataFolder.toPath(), newDataFolder.toPath());
        } else if (isSMOS(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.copy(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING);
            final File hdrFile = FileUtils.exchangeExtension(entry.getFile(), ".HDR");
            final File newHdrFile = new File(targetFolder, hdrFile.getName());
            Files.copy(hdrFile.toPath(), newHdrFile.toPath(), REPLACE_EXISTING);
        } else if (isFolderProduct(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getParentFile().getName());
            FileIOUtils.copyFolder(entry.getFile().getParentFile().toPath(), newFile.toPath());
        }
    }

    public static void moveTo(final ProductEntry entry, final File targetFolder) throws Exception {

        if (isSingleFile(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.move(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
        } else if (isDimap(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.move(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            final String dataFolderName = entry.getFile().getName().replace(
                    DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION,
                    DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
            final File oldDataFolder = new File(entry.getFile().getParentFile(), dataFolderName);
            final File newDataFolder = new File(targetFolder, dataFolderName);
            FileIOUtils.moveFolder(oldDataFolder.toPath(), newDataFolder.toPath());
        } else if (isSMOS(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getName());
            Files.move(entry.getFile().toPath(), newFile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            final File hdrFile = FileUtils.exchangeExtension(entry.getFile(), ".HDR");
            final File newHdrFile = new File(targetFolder, hdrFile.getName());
            Files.move(hdrFile.toPath(), newHdrFile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
        } else if (isFolderProduct(entry)) {
            final File newFile = new File(targetFolder, entry.getFile().getParentFile().getName());
            FileIOUtils.moveFolder(entry.getFile().getParentFile().toPath(), newFile.toPath());
        }
    }

    public static void delete(final ProductEntry entry) throws Exception {

        if (isDimap(entry)) {
            Files.delete(entry.getFile().toPath());
            final String dataFolderName = entry.getFile().getName().replace(
                    DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION,
                    DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
            final File dataFolder = new File(entry.getFile().getParentFile(), dataFolderName);
            FileIOUtils.deleteFolder(dataFolder.toPath());
        } else if (isSMOS(entry)) {
            Files.delete(entry.getFile().toPath());
            final File hdrFile = FileUtils.exchangeExtension(entry.getFile(), ".HDR");
            Files.delete(hdrFile.toPath());
        } else if (isFolderProduct(entry)) {
            FileIOUtils.deleteFolder(entry.getFile().getParentFile().toPath());
        } else if (isSingleFile(entry)) {
            Files.delete(entry.getFile().toPath());
        }
    }

    private static boolean isDimap(final ProductEntry entry) {
        return entry.getFile().getName().endsWith(DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
    }

    private static boolean isSMOS(final ProductEntry entry) {
        return entry.getFile().getName().toUpperCase().endsWith("DBL");
    }

    private static boolean isSingleFile(final ProductEntry entry) {
        final String fileName = entry.getFile().getName().toLowerCase();
        for (String ext : singleFileExt) {
            if (fileName.endsWith(ext))
                return true;
        }
        return false;
    }

    private static boolean isFolderProduct(final ProductEntry entry) {
        final String mission = entry.getMission();
        for (String folderMission : folderMissions) {
            if (mission.equals(folderMission))
                return true;
        }
        final String fileName = entry.getFile().getName().toLowerCase();
        for (String ext : folderExt) {
            if (fileName.endsWith(ext))
                return true;
        }
        if (mission.equals("ERS1") || mission.equals("ERS2")) {
            if (!isSingleFile(entry))  // if not .e1 or .e2
                return true;
        }
        return false;
    }
}
