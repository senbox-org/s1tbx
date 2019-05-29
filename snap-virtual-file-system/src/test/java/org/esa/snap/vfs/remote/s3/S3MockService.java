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
import java.util.stream.Stream;


class S3MockService {

    private HttpServer mockServer;
    private String mockServiceAddress;

    S3MockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        this.mockServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        int port = this.mockServer.getAddress().getPort();
        this.mockServiceAddress = serviceAddress.toString().replaceAll(":([\\d]+)", ":" + port);
        this.mockServer.createContext(serviceAddress.getPath(), new S3MockServiceHandler(serviceRootPath));
    }

    public static void main(String[] args) {
        try {
            S3MockService mockService = new S3MockService(new URL("http://localhost:0/mock-api/"), Paths.get(System.getProperty("s3.mock-service.root")));
            mockService.start();
            Logger.getLogger(S3MockService.class.getName()).info("S3 mock service started at: " + mockService.getMockServiceAddress());
        } catch (IOException e) {
            Logger.getLogger(S3MockService.class.getName()).severe("Unable to start S3 mock service.\nReason: " + e.getMessage());
        }
    }

    void start() {
        this.mockServer.start();
    }

    void stop() {
        this.mockServer.stop(1);
    }

    String getMockServiceAddress() {
        return this.mockServiceAddress;
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
                Path responsePath = this.serviceRootPath.resolve(uriPath);
                if (Files.isDirectory(responsePath)) {
                    if (responsePath.getParent().equals(this.serviceRootPath)) {
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
            } catch (IllegalArgumentException ex) {
                response = "Bad request".getBytes();
                httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
            } catch (Exception ex) {
                response = "Internal error".getBytes();
                httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
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
            try (InputStream is = Files.newInputStream(inputFile)) {
                byte[] data = new byte[is.available()];
                if (is.read(data) < 0) {
                    throw new IOException();
                }
                return data;
            }
        }

        private byte[] getXMLResponse(String uriPath, String uriQuery) throws IOException {
            int limit = 1000;
            Path bucketPath = this.serviceRootPath.resolve(uriPath);
            StringBuilder xml = new StringBuilder();
            String prefixParameterValue = getRequestParameter(uriQuery, "prefix");
            if (!prefixParameterValue.isEmpty() && !prefixParameterValue.endsWith("/")) {
                throw new IllegalArgumentException("Invalid prefix parameter.");
            }
            if (!uriPath.endsWith("/") && !prefixParameterValue.startsWith("/")) {
                uriPath = uriPath.concat("/");
            }
            uriPath = uriPath.concat(prefixParameterValue);
            String markerParameterValue = getRequestParameter(uriQuery, "continuation-token");
            markerParameterValue = markerParameterValue.replaceAll("/$", "");
            if (!markerParameterValue.isEmpty() && !markerParameterValue.startsWith(prefixParameterValue)) {
                throw new IllegalArgumentException("Invalid continuation-token parameter.");
            }
            uriPath = uriPath.replaceAll("^/", "").replaceAll("/{2,}", "/");
            Path path = this.serviceRootPath.resolve(uriPath);
            if (Files.isRegularFile(path) && uriPath.endsWith("/")) {
                throw new IllegalArgumentException("dir requested, but was file");
            }
            String isTruncated;
            try (Stream<Path> pathsStream = Files.walk(path, 1)) {
                Iterator<Path> paths = pathsStream.iterator();
                boolean markerReached = markerParameterValue.isEmpty();
                int index = 0;
                paths.next();
                while (paths.hasNext() && index < limit) {
                    index++;
                    Path pathItem = paths.next();
                    if (!markerReached) {
                        Path markerPath = bucketPath.resolve(markerParameterValue);
                        markerReached = pathItem.endsWith(markerPath);
                    } else {
                        if (Files.isDirectory(pathItem)) {
                            String directoryPath = pathItem.toString().replace(bucketPath.toString(), "").replace(bucketPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                            xml.append(DIRECTORY_XML.replaceAll(DIRECTORY_PATH, directoryPath + "/"));
                        } else {
                            long fileSize = Files.size(pathItem);
                            String fileDate = this.isoDateFormat.format(Files.getLastModifiedTime(pathItem).toMillis());
                            String filePath = pathItem.toString().replace(bucketPath.toString(), "").replace(bucketPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                            xml.append(FILE_XML.replaceAll(FILE_PATH, filePath).replaceAll(FILE_SIZE, "" + fileSize).replaceAll(FILE_DATE, fileDate));
                        }
                    }
                }
                isTruncated = paths.hasNext() ? "true" : "false";
            }
            return RESPONSE_XML.replace(PREFIX_CONTENT, prefixParameterValue).replace(MARKER_CONTENT, markerParameterValue).replace(MAX_KEYS_CONTENT, "" + limit).replace(IS_TRUNCATED_CONTENT, isTruncated).replace(BUCKET_CONTENT, xml.toString()).getBytes();
        }
    }

}
