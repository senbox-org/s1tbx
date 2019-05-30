package org.esa.snap.vfs.remote.swift;

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
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;


class SwiftMockService {

    private HttpServer mockServer;
    private String mockServiceAddress;

    SwiftMockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        this.mockServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        int port = this.mockServer.getAddress().getPort();
        this.mockServiceAddress = serviceAddress.toString().replaceAll(":([\\d]+)", ":" + port);
        this.mockServer.createContext(serviceAddress.getPath(), new SwiftMockServiceHandler(serviceRootPath));
    }

    public static void main(String[] args) {
        try {
            SwiftMockService mockService = new SwiftMockService(new URL("http://localhost:0/mock-api/swift/v1/"), Paths.get(System.getProperty("swift.mock-service.root")));
            mockService.start();
            Logger.getLogger(SwiftMockService.class.getName()).info("Swift mock service started at: " + mockService.getMockServiceAddress());
        } catch (IOException e) {
            Logger.getLogger(SwiftMockService.class.getName()).severe("Unable to start Swift mock service.\nReason: " + e.getMessage());
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

    private class SwiftMockServiceHandler implements HttpHandler {

        private static final String CONTAINER_NAME = "%container_name%";
        private static final String CONTAINER_CONTENT = "%container_content%";
        private static final String DIRECTORY_PATH = "%dir_path%";
        private static final String FILE_PATH = "%file_path%";
        private static final String FILE_SIZE = "%file_size%";
        private static final String FILE_DATE = "%file_date%";
        private static final String RESPONSE_XML = "<container name=\"" + CONTAINER_NAME + "\">\n" + CONTAINER_CONTENT + "</container>";
        private static final String DIRECTORY_XML = "<subdir name=\"" + DIRECTORY_PATH + "\">\n<name>" + DIRECTORY_PATH + "</name>\n</subdir>\n";
        private static final String FILE_XML = "<object>\n<name>" + FILE_PATH + "</name>\n<hash>00000000000000000000000000000000</hash>\n<bytes>" + FILE_SIZE + "</bytes>\n<last_modified>" + FILE_DATE + "</last_modified>\n</object>\n";

        private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");

        private Path serviceRootPath;

        SwiftMockServiceHandler(Path serviceRootPath) {
            this.serviceRootPath = serviceRootPath;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] response;
            int httpStatus = HttpURLConnection.HTTP_OK;
            String contentType = "text/plain";
            try {
                List<String> authHeader = httpExchange.getRequestHeaders().get("X-Auth-Token");
                if (authHeader != null && authHeader.get(0).contentEquals(SwiftAuthMockService.TOKEN)) {
                    String uriPath = httpExchange.getRequestURI().getPath();
                    uriPath = uriPath.replace(httpExchange.getHttpContext().getPath(), "");
                    uriPath = uriPath.replaceAll("^/", "").replaceAll("/{2,}", "/");
                    Path responsePath = this.serviceRootPath.resolve(uriPath);
                    if (Files.isDirectory(responsePath)) {
                        if (responsePath.getParent().equals(this.serviceRootPath)) {
                            response = getXMLResponse(uriPath, httpExchange.getRequestURI().getQuery());
                            contentType = "application/xml";
                        } else {
                            response = new byte[0];
                            contentType = "application/octet-stream";
                        }
                    } else if (Files.isRegularFile(responsePath) && !uriPath.endsWith("/")) {
                        response = readFile(responsePath);
                        contentType = "application/octet-stream";
                    } else {
                        response = "Not Found".getBytes();
                        httpStatus = HttpURLConnection.HTTP_NOT_FOUND;
                    }
                } else {
                    response = "AccessDenied".getBytes();
                    httpStatus = HttpURLConnection.HTTP_FORBIDDEN;
                }
            } catch (IllegalArgumentException ex) {
                response = "Bad request".getBytes();
                httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
            } catch (Exception ex) {
                response = "Internal error".getBytes();
                httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
            httpExchange.getResponseHeaders().add("Content-Type", contentType);
            httpExchange.getResponseHeaders().add("Server", "MockSwiftS3");
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
            int limit = 10000;
            String container = uriPath.replaceAll("/.*", "");
            Path containerPath = this.serviceRootPath.resolve(container);
            StringBuilder xml = new StringBuilder();
            String limitParameterValue = getRequestParameter(uriQuery, "limit");
            if (!limitParameterValue.isEmpty()) {
                if (Integer.parseInt(limitParameterValue) > limit) {
                    throw new IllegalArgumentException("Invalid limit parameter.");
                }
                limit = Integer.parseInt(limitParameterValue);
            }
            String prefixParameterValue = getRequestParameter(uriQuery, "prefix");
            if (!prefixParameterValue.isEmpty() && !prefixParameterValue.endsWith("/")) {
                throw new IllegalArgumentException("Invalid prefix parameter.");
            }
            if (!uriPath.endsWith("/") && !prefixParameterValue.startsWith("/")) {
                uriPath = uriPath.concat("/");
            }
            uriPath = uriPath.concat(prefixParameterValue);
            String markerParameterValue = getRequestParameter(uriQuery, "marker");
            markerParameterValue = markerParameterValue.replaceAll("/$", "");
            if (!markerParameterValue.isEmpty() && !markerParameterValue.startsWith(prefixParameterValue)) {
                throw new IllegalArgumentException("Invalid marker parameter.");
            }
            uriPath = uriPath.replaceAll("^/", "").replaceAll("/{2,}", "/");
            Path path = this.serviceRootPath.resolve(uriPath);
            if (Files.isRegularFile(path) && uriPath.endsWith("/")) {
                throw new IllegalArgumentException("dir requested, but was file");
            }
            try (Stream<Path> pathsStream = Files.walk(path, 1)) {
                Iterator<Path> paths = pathsStream.iterator();
                boolean markerReached = markerParameterValue.isEmpty();
                int index = 0;
                paths.next();
                while (paths.hasNext() && index < limit) {
                    index++;
                    Path pathItem = paths.next();
                    if (!markerReached) {
                        Path markerPath = containerPath.resolve(markerParameterValue);
                        markerReached = pathItem.endsWith(markerPath);
                    } else {
                        if (Files.isDirectory(pathItem)) {
                            String directoryPath = pathItem.toString().replace(containerPath.toString(), "").replace(containerPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                            xml.append(DIRECTORY_XML.replaceAll(DIRECTORY_PATH, directoryPath + "/"));
                        } else {
                            long fileSize = Files.size(pathItem);
                            String fileDate = this.isoDateFormat.format(Files.getLastModifiedTime(pathItem).toMillis());
                            String filePath = pathItem.toString().replace(containerPath.toString(), "").replace(containerPath.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                            xml.append(FILE_XML.replaceAll(FILE_PATH, filePath).replaceAll(FILE_SIZE, "" + fileSize).replaceAll(FILE_DATE, fileDate));
                        }
                    }
                }
            }
            return RESPONSE_XML.replace(CONTAINER_NAME, container).replace(CONTAINER_CONTENT, xml.toString()).getBytes();
        }
    }

}
