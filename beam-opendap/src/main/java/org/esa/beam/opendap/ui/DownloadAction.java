/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.ui;

import org.esa.beam.opendap.utils.DAPDownloader;

import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
class DownloadAction implements ActionListener, DAPDownloader.DownloadContext {

    private final Set<DownloadWorker> activeDownloaders = new HashSet<DownloadWorker>();

    private final DownloadProgressBarPM pm;
    private final ParameterProvider parameterProvider;
    private final DownloadHandler downloadHandler;

    private int downloadedFilesCount;
    private int filesToDownloadCount;

    public DownloadAction(DownloadProgressBarPM pm, ParameterProvider parameterProvider,
                          DownloadHandler downloadHandler) {
        this.pm = pm;
        this.parameterProvider = parameterProvider;
        this.downloadHandler = downloadHandler;
    }

    @Override
    public void actionPerformed(ActionEvent ignored) {
        Map<String, Boolean> dapURIs = parameterProvider.getDapURIs();
        List<String> fileURIs = parameterProvider.getFileURIs();
        parameterProvider.reset();
        if (dapURIs.size() == 0 && fileURIs.size() == 0) {
            return;
        }
        File targetDirectory = parameterProvider.getTargetDirectory();

        DAPDownloader downloader = new DAPDownloader(dapURIs, fileURIs, this, pm);
        if (activeDownloaders.isEmpty()) {
            pm.beginTask("", (int) (parameterProvider.getDatasizeInKb()));
            pm.worked(0);
            pm.resetStartTime();
            filesToDownloadCount = dapURIs.size() + fileURIs.size();
        } else {
            pm.updateTask((int) (parameterProvider.getDatasizeInKb()));
            filesToDownloadCount += dapURIs.size() + fileURIs.size();
        }
        DownloadWorker downloadWorker = new DownloadWorker(downloader, targetDirectory);
        downloadWorker.execute();
        activeDownloaders.add(downloadWorker);
    }

    @Override
    public int getAllFilesCount() {
        return filesToDownloadCount;
    }

    @Override
    public int getAllDownloadedFilesCount() {
        return downloadedFilesCount;
    }

    @Override
    public void notifyFileDownloaded(File downloadedFile) {
        downloadedFilesCount++;
        downloadHandler.handleDownloadFinished(downloadedFile);
    }

    @Override
    public boolean mayOverwrite(String filename) {
        return parameterProvider.inquireOverwritePermission(filename);
    }

    public void cancel() {
        for (DownloadWorker activeDownloader : new HashSet<DownloadWorker>(activeDownloaders)) {
            activeDownloader.cancel(true);
        }
        activeDownloaders.clear();
        pm.setCanceled(true);
        pm.setPreMessage("Download cancelled");
        pm.setPostMessage("");
        downloadedFilesCount = 0;
    }

    private class DownloadWorker extends SwingWorker<Void, Void> {

        private final DAPDownloader downloader;
        private final File targetDirectory;

        public DownloadWorker(DAPDownloader downloader, File targetDirectory) {
            this.downloader = downloader;
            this.targetDirectory = targetDirectory;
        }

        @Override
        protected Void doInBackground() {
            try {
                downloader.saveProducts(targetDirectory);
            } catch (Exception e) {
                downloadHandler.handleException(e);
            }
            return null;
        }

        @Override
        protected void done() {
            activeDownloaders.remove(this);
            if (activeDownloaders.isEmpty()) {
                pm.done();
                pm.setPreMessage("All downloads completed");
                pm.setPostMessage("");
                downloadedFilesCount = 0;
            }
        }
    }


    interface ParameterProvider {

        void reset();

        Map<String, Boolean> getDapURIs();

        List<String> getFileURIs();

        double getDatasizeInKb();

        File getTargetDirectory();

        boolean inquireOverwritePermission(String filename);
    }

    interface DownloadHandler {

        void handleException(Exception e);

        void handleDownloadFinished(File downloadedFiles);

    }
}
