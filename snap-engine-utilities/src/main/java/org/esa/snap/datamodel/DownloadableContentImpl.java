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
package org.esa.snap.datamodel;

import org.esa.beam.util.io.FileUtils;
import org.esa.snap.gpf.StatusProgressMonitor;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.util.ftpUtils;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Retrieves downloadable content
 */
public abstract class DownloadableContentImpl implements DownloadableContent {

    private File localFile;
    private final File localZipFile;
    private boolean localFileExists = false;
    private boolean remoteFileExists = true;
    private boolean errorInLocalFile = false;
    private DownloadableFile contentFile = null;
    private ftpUtils ftp = null;
    private Map<String, Long> fileSizeMap = null;
    private boolean unrecoverableError = false;

    private final URL remoteURL;

    public DownloadableContentImpl(final File localFile,
                                   final URL remoteURL, final String archiveExt) {
        this.remoteURL = remoteURL;

        this.localFile = localFile;
        this.localZipFile = FileUtils.exchangeExtension(localFile, archiveExt);
    }

    public void dispose() {
        try {
            if (ftp != null)
                ftp.disconnect();
            ftp = null;
            contentFile.dispose();
            contentFile = null;
        } catch (Exception e) {
            //
        }
    }

    public final DownloadableFile getContentFile() throws IOException {
        if (contentFile == null) {
            if (!remoteFileExists && !localFileExists)
                return null;
            findFile();
        }
        return contentFile;
    }

    public String getFileName() {
        return localFile.getName();
    }

    protected abstract DownloadableFile createContentFile(final File dataFile);

    private boolean findLocalFile() {
        return (localFile.exists() && localFile.isFile()) || (localZipFile.exists() && localZipFile.isFile());
    }

    private boolean getRemoteFile() throws IOException {
        if (remoteURL.getProtocol().contains("http"))
            return getRemoteHttpFile(remoteURL.toString());
        else
            return getRemoteFTPFile(remoteURL);
    }

    private synchronized void findFile() throws IOException {
        try {
            if (contentFile != null) return;
            if (!localFileExists && !errorInLocalFile) {
                localFileExists = findLocalFile();
            }
            if (localFileExists) {
                getLocalFile();
            } else if (remoteFileExists) {
                if (getRemoteFile()) {
                    getLocalFile();
                }
            }
            if (contentFile != null) {
                errorInLocalFile = false;
            } else {
                if (!remoteFileExists && localFileExists) {
                    System.out.println("Unable to read product " + localFile.getAbsolutePath());
                }
                localFileExists = false;
                errorInLocalFile = true;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            contentFile = null;
            localFileExists = false;
            errorInLocalFile = true;
            if (unrecoverableError) {
                throw new IOException(e);
            }
        }
    }

    private void getLocalFile() throws IOException {
        File dataFile = localFile;
        if (!dataFile.exists())
            dataFile = getFileFromZip(localZipFile);
        if (dataFile != null) {
            contentFile = createContentFile(dataFile);
        }
    }

    private boolean getRemoteHttpFile(final String baseUrl) throws IOException {
        final String remotePath = baseUrl + localZipFile.getName();
        System.out.println("http retrieving " + remotePath);
        try {
            downloadFile(new URL(remotePath), localZipFile);
            return true;
        } catch (Exception e) {
            System.out.println("http error:" + e.getMessage() + " on " + remotePath);
            remoteFileExists = false;
        }
        return false;
    }

    /**
     * Downloads a file from the specified URL to the specified local target directory.
     * The method uses a Swing progress monitor to visualize the download process.
     *
     * @param fileUrl      the URL of the file to be downloaded
     * @param localZipFile the target file
     * @return File the downloaded file
     * @throws java.io.IOException if an I/O error occurs
     */
    private static File downloadFile(final URL fileUrl, final File localZipFile) throws IOException {
        final File outputFile = new File(localZipFile.getParentFile(), new File(fileUrl.getFile()).getName());
        final URLConnection urlConnection = fileUrl.openConnection();
        final int contentLength = urlConnection.getContentLength();
        final InputStream is = new BufferedInputStream(urlConnection.getInputStream(), contentLength);
        final OutputStream os;
        try {
            if (!outputFile.getParentFile().exists())
                outputFile.getParentFile().mkdirs();
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (IOException e) {
            is.close();
            throw e;
        }

        try {
            final StatusProgressMonitor status = new StatusProgressMonitor(contentLength,
                    "Downloading " + localZipFile.getName() + "... ");
            status.setAllowStdOut(false);

            final int size = 32768;
            final byte[] buf = new byte[size];
            int n;
            int total = 0;
            while ((n = is.read(buf, 0, size)) > -1) {
                os.write(buf, 0, n);
                total += n;
                status.worked(total);
            }
            status.done();

            while (true) {
                final int b = is.read();
                if (b == -1) {
                    break;
                }
                os.write(b);
            }
        } catch (IOException e) {
            outputFile.delete();
            throw e;
        } finally {
            try {
                os.close();
            } finally {
                is.close();
            }
        }
        return outputFile;
    }

    private boolean getRemoteFTPFile(final URL remoteURL) throws IOException {
        try {
            if (ftp == null) {
                ftp = new ftpUtils(remoteURL.getHost());
                fileSizeMap = ftpUtils.readRemoteFileList(ftp, remoteURL.getHost(), remoteURL.getPath());
            }

            final String remoteFileName = localZipFile.getName();
            final Long fileSize = fileSizeMap.get(remoteFileName);

            final ftpUtils.FTPError result = ftp.retrieveFile(remoteURL.getPath() + remoteFileName, localZipFile, fileSize);
            if (result == ftpUtils.FTPError.OK) {
                return true;
            } else {
                if (result == ftpUtils.FTPError.FILE_NOT_FOUND) {
                    remoteFileExists = false;
                } else {
                    dispose();
                }
                localZipFile.delete();
            }
            return false;
        } catch (SocketException e) {
            unrecoverableError = true;
            throw e;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (ftp == null) {
                unrecoverableError = false;      // allow to continue
                remoteFileExists = false;
                throw new IOException("Failed to connect to FTP " + remoteURL.getHost() + '\n' + e.getMessage());
            }
            dispose();
        }
        return false;
    }

    private File getFileFromZip(final File dataFile) throws IOException {
        final String ext = FileUtils.getExtension(dataFile.getName());
        if (ext.equalsIgnoreCase(".zip")) {
            final String baseName = localFile.getName();
            final File newFile = new File(ResourceUtils.getApplicationUserTempDataDir(), baseName);
            if (newFile.exists())
                return newFile;

            ZipFile zipFile = null;
            BufferedOutputStream fileoutputstream = null;
            try {
                zipFile = new ZipFile(dataFile);
                fileoutputstream = new BufferedOutputStream(new FileOutputStream(newFile));

                ZipEntry zipEntry = zipFile.getEntry(baseName);
                if (zipEntry == null) {
                    zipEntry = zipFile.getEntry(baseName.toLowerCase());
                    if (zipEntry == null) {
                        final String folderName = FileUtils.getFilenameWithoutExtension(dataFile.getName());
                        zipEntry = zipFile.getEntry(folderName + '/' + localFile.getName());
                        if (zipEntry == null) {
                            localFileExists = false;
                            throw new IOException("Entry '" + baseName + "' not found in zip file.");
                        }
                    }
                }

                final int size = 8192;
                final byte[] buf = new byte[size];
                InputStream zipinputstream = zipFile.getInputStream(zipEntry);

                int n;
                while ((n = zipinputstream.read(buf, 0, size)) > -1)
                    fileoutputstream.write(buf, 0, n);

                return newFile;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                dataFile.delete();
                return null;
            } finally {
                if (zipFile != null)
                    zipFile.close();
                if (fileoutputstream != null)
                    fileoutputstream.close();
            }
        }
        return dataFile;
    }
}