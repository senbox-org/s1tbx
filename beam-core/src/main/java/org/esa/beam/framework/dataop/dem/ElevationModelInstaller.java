/*
 * $Id: ElevationModelInstaller.java,v 1.2 2006/10/10 14:47:21 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.*;

import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;

import com.bc.io.FileDownloader;
import com.bc.io.FileUnpacker;

class ElevationModelInstaller extends SwingWorker {

    private final Component _parent;
    private IOException _ioException;
    private int _status;
    private AbstractElevationModelDescriptor _descriptor;
    private File _archiveFile;

    public ElevationModelInstaller(AbstractElevationModelDescriptor descriptor, Component parent) {
        _descriptor = descriptor;
        _parent = parent;
    }

    public int getStatus() {
        return _status;
    }

    public IOException getIOException() {
        return _ioException;
    }


    /**
     * Computes a result, or throws an exception if unable to do so.
     * <p/>
     * <p/>
     * Note that this method is executed only once.
     * <p/>
     * <p/>
     * Note: this method is executed in a background thread.
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    protected Object doInBackground() throws Exception {
        _status = ElevationModelDescriptor.DEM_INSTALLATION_IN_PROGRESS;
        _ioException = null;

        _archiveFile = null;

        try {
            final URL archiveUrl = _descriptor.getDemArchiveUrl();
            final File demInstallDir = _descriptor.getDemInstallDir();
            if (!demInstallDir.exists()) {
                final boolean success = demInstallDir.mkdirs();
                if (!success) {
                    throw new IOException("Failed to create directory " + demInstallDir);
                }
            }
            BeamLogManager.getSystemLogger().info("Downloading DEM archive from " + archiveUrl);
            _archiveFile = FileDownloader.downloadFile(archiveUrl, demInstallDir, _parent);
            BeamLogManager.getSystemLogger().info("Unpacking DEM archive " + _archiveFile + " to " + demInstallDir);
            FileUnpacker.unpackZip(_archiveFile, demInstallDir, _parent);
            BeamLogManager.getSystemLogger().info("DEM successfully installed");
        } catch (IOException e) {
            _ioException = e;
        } finally {
            if (_ioException == null) {
                _status = ElevationModelDescriptor.DEM_INSTALLED;
                BeamLogManager.getSystemLogger().info("DEM successfully installed");
                _descriptor.storeProperties();
            } else {
                _status = ElevationModelDescriptor.DEM_INSTALLATION_ERROR;
                BeamLogManager.getSystemLogger().log(Level.SEVERE,
                                                     "Failed to install DEM " + _descriptor.getName(),
                                                     _ioException);
                Debug.trace(_ioException);
            }
        }
        return null;
    }

    public void finished() {
        notifyUser();
        promptForArchiveFileDeletion();
    }

    private void notifyUser() {
        if (_ioException == null) {
            if (_descriptor.isDemInstalled()) {
                JOptionPane.showMessageDialog(_parent,
                                              "The DEM '" + _descriptor.getName() + "' has successfully been installed in directory\n" +
                                              _descriptor.getDemInstallDir(),
                                              "DEM Installed",
                                              JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(_parent,
                                              "Failed to install DEM '" + _descriptor.getName() + "' in directory\n" +
                                              _descriptor.getDemInstallDir() + "\n" +
                                              "An unknown error occured.\n",
                                              "DEM Installation Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(_parent,
                                          "Failed to install DEM '" + _descriptor.getName() + "' in directory\n" +
                                          _descriptor.getDemInstallDir() + "\n" +
                                          "An I/O error occured:\n"
                                          + _ioException.getMessage(),
                                          "DEM Installation Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptForArchiveFileDeletion() {
        if (_archiveFile != null && _archiveFile.exists()) {
            final int answer = JOptionPane.showConfirmDialog(_parent,
                                                             "Delete the zipped DEM archive file\n" +
                                                             "'" + _archiveFile.getPath() + "'?\n\n" +
                                                             "(Not required by BEAM anymore.)",
                                                             "Delete File",
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                _archiveFile.delete();
            }
        }
    }

}
