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
package org.esa.snap.util;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.esa.snap.gpf.StatusProgressMonitor;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.*;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class ftpUtils {

    private final FTPClient ftpClient = new FTPClient();
    private final String server;
    private final String user;
    private final String password;
    private boolean ftpClientConnected = false;

    public enum FTPError {FILE_NOT_FOUND, OK, READ_ERROR}

    public ftpUtils(final String server) throws IOException {
        this(server, "anonymous", "anonymous");
    }

    public ftpUtils(final String server, final String user, final String password) throws IOException {
        this.server = server;
        this.user = user;
        this.password = password;
        connect();
    }

    private void connect() throws IOException {
        ftpClient.setRemoteVerificationEnabled(false);
        ftpClient.connect(server);
        int reply = ftpClient.getReplyCode();
        if (FTPReply.isPositiveCompletion(reply))
            ftpClientConnected = ftpClient.login(user, password);
        if (!ftpClientConnected) {
            disconnect();
            throw new IOException("Unable to connect to " + server);
        } else {
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setDataTimeout(60000);
        }
    }

    public void disconnect() throws IOException {
        if (ftpClientConnected) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public FTPError retrieveFile(final String remotePath, final File localFile, final Long fileSize) throws Exception {
        BufferedOutputStream fos = null;
        InputStream fis = null;
        try {
            System.out.println("ftp retrieving " + remotePath);

            fis = ftpClient.retrieveFileStream(remotePath);
            if (fis == null) {
                final int code = ftpClient.getReplyCode();
                System.out.println("error code:" + code + " on " + remotePath);
                if (code == 550)
                    return FTPError.FILE_NOT_FOUND;
                else
                    return FTPError.READ_ERROR;
            }

            final File parentFolder = localFile.getParentFile();
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }
            fos = new BufferedOutputStream(new FileOutputStream(localFile.getAbsolutePath()));

            final StatusProgressMonitor status = new StatusProgressMonitor(fileSize,
                    "Downloading " + localFile.getName() + "... ");
            status.setAllowStdOut(false);

            final int size = 4096;//32768;
            final byte[] buf = new byte[size];
            int n;
            int total = 0;
            while ((n = fis.read(buf, 0, size)) > -1) {
                fos.write(buf, 0, n);
                if (fileSize != null) {
                    total += n;
                    status.worked(total);
                } else {
                    status.working();
                }
            }
            status.done();

            ftpClient.completePendingCommand();
            return FTPError.OK;

        } catch (SocketException e) {
            System.out.println(e.getMessage());
            connect();
            throw new SocketException(e.getMessage() + "\nPlease verify that FTP is not blocked by your firewall.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            connect();
            return FTPError.READ_ERROR;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long getFileSize(final FTPFile[] fileList, final String remoteFileName) {
        for (FTPFile file : fileList) {
            if (file.getName().equalsIgnoreCase(remoteFileName)) {
                return file.getSize();
            }
        }
        return 0;
    }

    private FTPFile[] getRemoteFileList(final String path) throws IOException {
        return ftpClient.listFiles(path);
    }

    public static String getPathFromSettings(final String tag) {
        String path = Settings.instance().get(tag);
        path = path.replace("\\", "/");
        if (!path.endsWith("/"))
            path += "/";
        return path;
    }

    public static Map<String, Long> readRemoteFileList(final ftpUtils ftp, final String server, final String remotePath) {

        boolean useCachedListing = true;
        final String tmpDirUrl = ResourceUtils.getApplicationUserTempDataDir().getAbsolutePath();
        final File listingFile = new File(tmpDirUrl + "//" + server + ".listing.xml");
        if (!listingFile.exists())
            useCachedListing = false;

        final Map<String, Long> fileSizeMap = new HashMap<String, Long>(900);

        if (useCachedListing) {
            Document doc = null;
            try {
                doc = XMLSupport.LoadXML(listingFile.getAbsolutePath());
            } catch (IOException e) {
                useCachedListing = false;
            }

            if (useCachedListing) {
                final Element root = doc.getRootElement();
                boolean listingFound = false;

                final List children1 = root.getContent();
                for (Object c1 : children1) {
                    if (!(c1 instanceof Element)) continue;
                    final Element remotePathElem = (Element) c1;
                    final Attribute pathAttrib = remotePathElem.getAttribute("path");
                    if (pathAttrib != null && pathAttrib.getValue().equalsIgnoreCase(remotePath)) {
                        listingFound = true;
                        final List children2 = remotePathElem.getContent();
                        for (Object c2 : children2) {
                            if (!(c2 instanceof Element)) continue;
                            final Element fileElem = (Element) c2;
                            final Attribute attrib = fileElem.getAttribute("size");
                            if (attrib != null) {
                                try {
                                    fileSizeMap.put(fileElem.getName(), attrib.getLongValue());
                                } catch (Exception e) {
                                    //
                                }
                            }
                        }
                    }
                }
                if (!listingFound)
                    useCachedListing = false;
            }
        }
        if (!useCachedListing) {
            try {
                final FTPFile[] remoteFileList = ftp.getRemoteFileList(remotePath);

                writeRemoteFileList(remoteFileList, server, remotePath, listingFile);

                for (FTPFile ftpFile : remoteFileList) {
                    fileSizeMap.put(ftpFile.getName(), ftpFile.getSize());
                }
            } catch (Exception e) {
                System.out.println("Unable to get remote file list " + e.getMessage());
            }
        }

        return fileSizeMap;
    }

    private static void writeRemoteFileList(final FTPFile[] remoteFileList, final String server,
                                            final String remotePath, final File file) {

        final Element root = new Element("remoteFileListing");
        root.setAttribute("server", server);

        final Document doc = new Document(root);
        final Element remotePathElem = new Element("remotePath");
        remotePathElem.setAttribute("path", remotePath);
        root.addContent(remotePathElem);

        for (FTPFile ftpFile : remoteFileList) {
            final Element fileElem = new Element(ftpFile.getName());
            fileElem.setAttribute("size", String.valueOf(ftpFile.getSize()));
            remotePathElem.addContent(fileElem);
        }
        XMLSupport.SaveXML(doc, file.getAbsolutePath());
    }

    public static boolean testFTP(final String remoteFTP, final String remotePath) throws IOException {
        final ftpUtils ftp = new ftpUtils(remoteFTP);

        final FTPFile[] remoteFileList = ftp.getRemoteFileList(remotePath);
        ftp.disconnect();

        return (remoteFileList != null);
    }
}