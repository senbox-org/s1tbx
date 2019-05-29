package org.esa.snap.vfs.remote.s3;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.logging.Logger;


class S3MockService {

    private HttpServer mockServer;

    S3MockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        mockServer.createContext(serviceAddress.getPath(), new S3MockServiceHandler(serviceRootPath));
    }

    public static void main(String[] args) {
        try {
            S3MockService mockService = new S3MockService(new URL("http://localhost:777/mock-api/"), Paths.get(System.getProperty("s3.mock-service.root")));
            mockService.start();
        } catch (IOException e) {
            Logger.getLogger(S3MockService.class.getName()).severe("Unable to start S3 mock service.\nReason: " + e.getMessage());
        }
    }

    void start() {
        mockServer.start();
    }

    void stop() {
        mockServer.stop(1);
    }

    private class S3MockServiceHandler implements HttpHandler {

        private static final String PREFIX_CONTENT = "%prefix_content%";
        private static final String MARKER_CONTENT = "%marker_content%";
        private static final String MAX_KEYS_CONTENT = "%max_keys_content%";
        private static final String IS_TRUNCATED_CONTENT = "%is_truncated_content%";
        private static final String BUCKET_CONTENT = "%bucket_content%";
        private static final String DIRECTORY_PATH = "%dir_path%";
        private static final String FILE_PATH = "%file_path%";
        private static final String FILE_SIZE = "%file_size%";
        private static final String FILE_DATE = "%file_date%";
        private static final String RESPONSE_XML = "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n<Name>mock-bucket</Name>\n<Prefix>" + PREFIX_CONTENT + "</Prefix>\n<Marker>" + MARKER_CONTENT + "</Marker>\n<MaxKeys>" + MAX_KEYS_CONTENT + "</MaxKeys>\n<Delimiter>/</Delimiter>\n<IsTruncated>" + IS_TRUNCATED_CONTENT + "</IsTruncated>\n" + BUCKET_CONTENT + "</ListBucketResult>";
        private static final String DIRECTORY_XML = "<CommonPrefixes>\n<Prefix>" + DIRECTORY_PATH + "\n</Prefix>\n</CommonPrefixes>\n";
        private static final String FILE_XML = "<Contents>\n<Key>" + FILE_PATH + "</Key>\n<LastModified>" + FILE_DATE + "</LastModified>\n<ETag>00000000000000000000000000000000</ETag>\n<Size>" + FILE_SIZE + "</Size>\n<StorageClass>STANDARD</StorageClass>\n</Contents>\n";

        private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");

        private Path serviceRootPath;

        S3MockServiceHandler(Path serviceRootPath) {
            this.serviceRootPath = serviceRootPath;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] response;
            int httpStatus = HttpURLConnection.HTTP_OK;
            String contentType = "text/plain";
            try {
                String uriPath = httpExchange.getRequestURI().getPath();
                uriPath = uriPath.replace(httpExchange.getHttpContext().getPath(), "");
                uriPath = uriPath.replaceAll("^/", "").replaceAll("/{2,}", "/");
                Path responsePath = serviceRootPath.resolve(uriPath);
                if (Files.isDirectory(responsePath)) {
                    if (responsePath.equals(serviceRootPath)) {
                        response = getXMLResponse(uriPath, httpExchange.getRequestURI().getQuery());
                        contentType = "application/xml";
                    } else {
                        response = "Not Found".getBytes();
                        httpStatus = HttpURLConnection.HTTP_NOT_FOUND;
                    }
                } else if (Files.isRegularFile(responsePath) && !uriPath.endsWith("/")) {
                    response = readFile(responsePath);
                    contentType = "application/octet-stream";
                } else {
                    response = "Not Found".getBytes();
                    httpStatus = HttpURLConnection.HTTP_NOT_FOUND;
                }
            } catch (Exception ex) {
                if (ex instanceof IllegalArgumentException) {
                    response = "Bad request".getBytes();
                    httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
                } else {
                    response = "Internal error".getBytes();
                    httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
                }
            }
            httpExchange.getResponseHeaders().add("Content-Type", contentType);
            httpExchange.getResponseHeaders().add("Server", "MockS3");
            httpExchange.sendResponseHeaders(httpStatus, response.length);
            httpExchange.getResponseBody().write(response);
            httpExchange.close();
        }

        private String getRequestParameter(String query, String key) {
            String value = "";
            if (query != null && key != null) {
                value = query.replaceAll("(.*" + key + "=([^&]*).*)", "$2");
                value = value.contentEquals(query) ? "" : value;
            }
            return value;
        }

        private byte[] readFile(Path inputFile) throws IOException {
            InputStream is = Files.newInputStream(inputFile);
            byte data[] = new byte[is.available()];
            if (is.read(data) < 0) {
                throw new IOException();
            }
            is.close();
            return data;
        }

        private byte[] getXMLResponse(String uriPath, String uriQuery) throws IOException {
            int limit = 1000;
            Path bucketPath = serviceRootPath.resolve(uriPath);
            StringBuilder xml = new StringBuilder();
            String prefix = getRequestParameter(uriQuery, "prefix");
            if (!prefix.isEmpty() && !prefix.endsWith("/")) {
                throw new IllegalArgumentException("Invalid prefix parameter.");
            }
            if (!uriPath.endsWith("/") && !prefix.startsWith("/")) {
                uriPath = uriPath.concat("/");
            }
            uriPath = uriPath.concat(prefix);
            String marker = getRequestParameter(uriQuery, "continuation-token");
            marker = marker.replaceAll("/$", "");
            if (!marker.isEmpty() && !marker.startsWith(prefix)) {
                throw new IllegalArgumentException("Invalid continuation-token parameter.");
            }
            uriPath = uriPath.replaceAll("^/", "").replaceAll("/{2,}", "/");
            Path path = serviceRootPath.resolve(uriPath);
            if (Files.isRegularFile(path) && uriPath.endsWith("/")) {
                throw new IllegalArgumentException("dir requested, but was file");
            }
            Iterator<Path> paths = Files.walk(path, 1).iterator();
            boolean markerReached = marker.isEmpty();
            int index = 0;
            paths.next();
            while (paths.hasNext() && index < limit) {
                index++;
                Path pathItem = paths.next();
                if (!markerReached) {
                    Path markerPath = bucketPath.resolve(marker);
                    markerReached = pathItem.endsWith(markerPath);
                } else {
                    if (Files.isDirectory(pathItem)) {
                        String directoryPath = pathItem.toString().replace(bucketPath.toString(), "").replace(bucketPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                        xml.append(DIRECTORY_XML.replaceAll(DIRECTORY_PATH, directoryPath + "/"));
                    } else {
                        long fileSize = Files.size(pathItem);
                        String fileDate = isoDateFormat.format(Files.getLastModifiedTime(pathItem).toMillis());
                        String filePath = pathItem.toString().replace(bucketPath.toString(), "").replace(bucketPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                        xml.append(FILE_XML.replaceAll(FILE_PATH, filePath).replaceAll(FILE_SIZE, "" + fileSize).replaceAll(FILE_DATE, fileDate));
                    }
                }
            }
            String isTruncated = paths.hasNext() ? "true" : "false";
            return RESPONSE_XML.replace(PREFIX_CONTENT, prefix).replace(MARKER_CONTENT, marker).replace(MAX_KEYS_CONTENT, "" + limit).replace(IS_TRUNCATED_CONTENT, isTruncated).replace(BUCKET_CONTENT, xml.toString()).getBytes();
        }
    }

}
