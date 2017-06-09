/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.download.opendata;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.esa.snap.core.util.SystemUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

class HTTPDownloader {

    private static final String HTTP_HEADER_ACCEPT = "Accept";
    private static final String HTTP_METHOD_PUT = "PUT";
    private static final String HTTP_METHOD_POST = "POST";
    static final String HTTP_METHOD_GET = "GET";
    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    private static final int BUFFER_SIZE = 8192;
    private static final int MB = 1024 * 1024;

    public enum ExtractorStatus {

        STARTING("Starting"), PROCESSING_QUERY("Processing query"), DOWNLOADING("Downloading"), STALLED("Stalled"), FORCE_STOP("Force Stop"), FINISHED("Finished");
        private String value = null;
        ExtractorStatus(String value) { this.value = value; }
    }

    HTTPDownloader() {

    }

    public static InputStream connect(final String relativeUri, final String contentType, final String httpMethod,
                        final String user, final String password) throws IOException {

        final HttpURLConnection connection = initializeConnection(relativeUri, contentType, httpMethod, user, password);
        connection.connect();
        checkStatus(connection);

        return connection.getInputStream();
    }

    public static EntryFileProperty getEntryFilePropertyFromUrlString(String urlStr, String fileName,
                                                                      long completeFileSize,  String contentType,
                                                                      File outputFolder, String user, String password,
                                                                      ProgressMonitor pm) throws IOException {

        final File outFile = new File(outputFolder, fileName);

        final DownloaderThreadChecker tChecker = new DownloaderThreadChecker(outFile, completeFileSize);

        HttpURLConnection connection = null;
        FileOutputStream fos = null;
        InputStream in = null;
        OutputStream bout = null;
        RandomAccessFile randomAccessfile = null;

        long downloadedFileSize = 0;

        try {
            Authenticator.setDefault(new SeHttpAuthenticator(user, password));
            URL url = new URL(urlStr);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestProperty(HTTP_HEADER_ACCEPT, contentType);

            byte[] data = null;
            int bytesread = 0;
            int bytesBuffered = 0;

            if (outFile.exists()) {

                downloadedFileSize = outFile.length();

                int mbLength = (int)((completeFileSize - downloadedFileSize)/1024);
                pm.beginTask("Downloading", mbLength);

                if (downloadedFileSize < completeFileSize) {
                    SystemUtils.LOG.info("Incomplete download. Resuming.");

                    connection.setRequestProperty("Range", "bytes=" + downloadedFileSize + '-');

                    connection.connect();
                    try {
                        checkStatus(connection);
                        if (!tChecker.isAlive()) tChecker.start();
                    } catch (IOException e) {
                        SystemUtils.LOG.severe("Error: " + e.getMessage());
                        return null;
                    }

                    long contentLength = connection.getContentLength();
                    if (contentLength < 1) contentLength = completeFileSize;


                    randomAccessfile = new RandomAccessFile(outFile, "rw");
                    randomAccessfile.seek(downloadedFileSize);

                    in = connection.getInputStream();

                    final long downloadDiff = completeFileSize - downloadedFileSize;
                    if (downloadDiff > BUFFER_SIZE) {
                        data = new byte[BUFFER_SIZE];
                    } else {
                        data = new byte[(int) downloadDiff];
                    }

                    while ((bytesread = in.read(data)) > -1) {

                        randomAccessfile.write(data, 0, bytesread);
                        downloadedFileSize += bytesread;
                        //printDownloadedProgress(completeFileSize, downloadedFileSize);
                        pm.worked((int)(downloadedFileSize/1024));
                    }
                } else {
                    SystemUtils.LOG.warning("File already downloaded.");
                }
            } else {

                int mbLength = (int)(completeFileSize/1024);
                pm.beginTask("Downloading", mbLength);

                SystemUtils.LOG.info("Starting download to " + outFile.getAbsolutePath());
                connection.connect();
                try {
                    checkStatus(connection);
                    if (!tChecker.isAlive()) tChecker.start();
                } catch (IOException e) {
                    SystemUtils.LOG.severe(e.getMessage());
                    return null;
                }

                in = connection.getInputStream();
                fos = new FileOutputStream(outFile);
                bout = new BufferedOutputStream(fos, BUFFER_SIZE);
                data = new byte[BUFFER_SIZE];

                while ((bytesread = in.read(data)) > -1) {
                    bout.write(data, 0, bytesread);
                    bytesBuffered += bytesread;
                    downloadedFileSize += bytesread;

                    //printDownloadedProgress(completeFileSize, downloadedFileSize);

                    if (bytesBuffered > MB) { //flush after 1MB
                        bytesBuffered = 0;
                        bout.flush();
                        pm.worked((int)(downloadedFileSize/1024));
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pm.done();

            if (bout != null) {
                try {
                    bout.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    bout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (randomAccessfile != null) try {
                randomAccessfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null) {
                connection.disconnect();
            }
            if (tChecker.isAlive()) {
                tChecker.forceStop();
            }
        }

        return new EntryFileProperty(outFile.getAbsolutePath(), bytesToHex(md5CheksumFromFilePath(outFile)), null, downloadedFileSize);
    }

    private static byte[] md5CheksumFromFilePath(final File fSource){

        byte[] byteArrayChecksum = null;
        InputStream is = null;
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            is = new FileInputStream(fSource);

            final byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = is.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byteArrayChecksum = md.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        } finally{
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return byteArrayChecksum;
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static void printDownloadedProgress(long completeFileSize, long downloadedFileSize) {

        SystemUtils.LOG.info(formatDownloadedProgress(completeFileSize, downloadedFileSize)+'\r');
	}

    private static String formatDownloadedProgress(long completeFileSize, long downloadedFileSize) {
		DecimalFormat dfa = new DecimalFormat("000.0");
		DecimalFormat dfb = new DecimalFormat("###,###,###,###");
		double currentProgress = ((((double)downloadedFileSize) * 100) / ((double)completeFileSize));
		return dfa.format(currentProgress)+"% "+dfb.format(downloadedFileSize) + " bytes";
	}

	private static String formatDownloadedProgressOnlyNumber(long completeFileSize, long downloadedFileSize) {
		DecimalFormat dfa = new DecimalFormat("000.0");
		double currentProgress = ((((double)downloadedFileSize) * 100) / ((double)completeFileSize));
		return dfa.format(currentProgress);
	}

    private static void checkStatus(final HttpURLConnection connection) throws IOException {
        final HttpStatusCodes httpStatusCode = HttpStatusCodes.fromStatusCode(connection.getResponseCode());

        if (400 <= httpStatusCode.getStatusCode() && httpStatusCode.getStatusCode() <= 599) {
            throw new IOException("Http Connection failed with status " + httpStatusCode.getStatusCode() + ' ' + httpStatusCode.toString() + ' ' + connection.getURL().toString());
        } else {
            SystemUtils.LOG.finer("HTTP Response Code: " + httpStatusCode);
        }
    }

    private static HttpURLConnection initializeConnection(String absolutUri, String contentType, String httpMethod, String user, String password) throws IOException {

        Authenticator.setDefault(new SeHttpAuthenticator(user, password));
        HttpURLConnection connection = null;
        try {
            URL url = new URL(absolutUri);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod(httpMethod);
            connection.setRequestProperty(HTTP_HEADER_ACCEPT, contentType);
            if (HTTP_METHOD_POST.equals(httpMethod) || HTTP_METHOD_PUT.equals(httpMethod)) {
                connection.setDoOutput(true);
                connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
            }
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");

        } finally {
            if (connection != null) connection.disconnect();
        }

        return connection;
    }

    static class EntryFileProperty {

        private String name = null;
        private String md5Checksum = null;
        private String uuid = null;
        private long size = -1l;

        EntryFileProperty(String name, String md5Checksum, String uuid, long size) {
            this.name = name;
            this.md5Checksum = md5Checksum;
            this.uuid = uuid;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMd5Checksum() {
            return md5Checksum;
        }

        public String getUuid() {
            return uuid;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    private static class SeHttpAuthenticator extends Authenticator {

        private String user = null;
        private String password = null;

        SeHttpAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(user, password.toCharArray()));
        }
    }

    private static class DownloaderThreadChecker extends Thread {

        private final File outputFileNamePath;
        private final long len;
        private boolean forceStop = false;
        private ExtractorStatus status;

        private static final int THREAD_SLEEP = 60000;

        DownloaderThreadChecker(final File outputFileNamePath, final long len){
            this.outputFileNamePath = outputFileNamePath;
            this.len = len;
        }

        void forceStop(){
            forceStop = true;
            status = ExtractorStatus.FORCE_STOP;
            SystemUtils.LOG.info("ThreadChecker status:    "+status);
        }

        public void run(){
            RandomAccessFile raf = null;
            long lenT0 = -1l;
            long lenT1 = -1l;

            while(lenT1 < len && !forceStop){

                try {
                    raf = new RandomAccessFile(outputFileNamePath, "r");
                    lenT0 = raf.length();

                    Thread.sleep(THREAD_SLEEP);

                    lenT1 = raf.length();

                    String statusDescription = outputFileNamePath+" | "+ formatDownloadedProgress(len, lenT1);

                    if(lenT1 > lenT0){
                        status = ExtractorStatus.DOWNLOADING;
                        SystemUtils.LOG.info("ThreadChecker status:\t"+formatDownloadedProgress(len, lenT1));
                    }
                    else if(len == lenT1 || len == lenT0){
                        status = ExtractorStatus.FINISHED;
                        SystemUtils.LOG.info("ThreadChecker status: "+status);
                    }
                    else if(lenT0 == lenT1){
                        status = ExtractorStatus.STALLED;
                        SystemUtils.LOG.info("ThreadChecker status: "+status+"         FORCE QUITING!");

//                        if(Launch.UDP_SERVER_PORT > 0){
//
//                            SeUdpClient udpClient = new SeUdpClient(Launch.UDP_SERVER_PORT, aHistory);
//                            udpClient.sendMsgToUdpServer();
//
//                        }

                        System.exit(1);
                    }

                } catch (FileNotFoundException e) {

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if(raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
