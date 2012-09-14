package org.esa.nest.gpf.orbits;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.Orbits;
import org.esa.nest.util.ftpUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Base class for Orbit files
 */
public abstract class BaseOrbitFile implements OrbitFile {
    protected final String orbitType;
    protected final MetadataElement absRoot;
    protected File orbitFile = null;

    protected ftpUtils ftp = null;
    protected Map<String, Long> fileSizeMap = null;

    protected BaseOrbitFile(final String orbitType, final MetadataElement absRoot) {
        this.orbitType = orbitType;
        this.absRoot = absRoot;
    }

    public abstract Orbits.OrbitData getOrbitData(final double utc) throws Exception;

    public File getOrbitFile() {
        return orbitFile;
    }

    protected static void getRemoteFiles(final ftpUtils ftp, final Map<String, Long> fileSizeMap,
                                       final String remotePath, final File localPath, final ProgressMonitor pm) {
        final Set<String> remoteFileNames = fileSizeMap.keySet();
        pm.beginTask("Downloading Orbit files from "+remotePath, remoteFileNames.size());
        for(String fileName : remoteFileNames) {
            if(pm.isCanceled()) break;

            final long fileSize = fileSizeMap.get(fileName);
            final File localFile = new File(localPath, fileName);
            if(localFile.exists() && localFile.length() == fileSize)
                continue;
            try {
                int attempts=0;
                while(attempts < 3) {
                    final ftpUtils.FTPError result = ftp.retrieveFile(remotePath +'/'+ fileName, localFile, fileSize);
                    if(result == ftpUtils.FTPError.OK) {
                        break;
                    } else {
                        attempts++;
                        localFile.delete();
                    }
                }
            } catch(Exception e) {
                localFile.delete();
                System.out.println(e.getMessage());
            }

            pm.worked(1);
        }
        pm.done();
    }

    protected boolean getRemoteFile(String remoteFTP, String remotePath, File localFile) {
        try {
            if(ftp == null) {
                ftp = new ftpUtils(remoteFTP);
                fileSizeMap = ftpUtils.readRemoteFileList(ftp, remoteFTP, remotePath);
            }

            final String remoteFileName = localFile.getName();
            final Long fileSize = fileSizeMap.get(remoteFileName);

            final ftpUtils.FTPError result = ftp.retrieveFile(remotePath + remoteFileName, localFile, fileSize);
            if(result == ftpUtils.FTPError.OK) {
                return true;
            } else {
                localFile.delete();
            }

            return false;
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    protected static class DownloadOrbitWorker extends ProgressMonitorSwingWorker {

        private final String remotePath;
        private final File localPath;
        private final ftpUtils ftp;
        private final Map<String, Long> fileSizeMap;

        DownloadOrbitWorker(final VisatApp visatApp, final String title,
                            final ftpUtils ftp, final Map<String, Long> fileSizeMap,
                            final String remotePath, final File localPath) {
            super(visatApp.getMainFrame(), title);
            this.ftp = ftp;
            this.fileSizeMap = fileSizeMap;
            this.remotePath = remotePath;
            this.localPath = localPath;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            getRemoteFiles(ftp, fileSizeMap, remotePath, localPath, pm);
            return 0;
        }
    }

}
