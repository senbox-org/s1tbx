package org.esa.snap.vfs.remote.http;

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

import static org.junit.Assume.assumeTrue;

public class HttpMockService {

    private HttpServer mockServer;
    private String mockServiceAddress;

    public HttpMockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        this.mockServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        int port = this.mockServer.getAddress().getPort();
        this.mockServiceAddress = serviceAddress.toString().replaceAll(":([\\d]+)", ":" + port);
        this.mockServer.createContext(serviceAddress.getPath(), new HTTPMockServiceHandler(serviceRootPath));
    }

    public static void main(String[] args) {
        try {
            HttpMockService mockService = new HttpMockService(new URL("http://localhost:0/mock-api/"), Paths.get(System.getProperty("http.mock-service.root")));
            mockService.start();
            Logger.getLogger(HttpMockService.class.getName()).info("HTTP mock service started at: " + mockService.getMockServiceAddress());
        } catch (IOException e) {
            Logger.getLogger(HttpMockService.class.getName()).severe("Unable to start HTTP mock service.\nReason: " + e.getMessage());
        }
    }

    public void start() {
        this.mockServer.start();
    }

    public void stop() {
        this.mockServer.stop(1);
    }

    public String getMockServiceAddress() {
        return this.mockServiceAddress;
    }

    private class HTTPMockServiceHandler implements HttpHandler {

        private static final String PREFIX_CONTENT = "%prefix_content%";
        private static final String HTTP_CONTENT = "%bucket_content%";
        private static final String DIRECTORY_PATH = "%dir_path%";
        private static final String FILE_PATH = "%file_path%";
        private static final String FILE_SIZE = "%file_size%";
        private static final String FILE_DATE = "%file_date%";
        private static final String RESPONSE_HTML = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n<html>\n<head>\n<title>Index of " + PREFIX_CONTENT + "</title>\n</head>\n<body>\n<h1>Index of " + PREFIX_CONTENT + "</h1>\n<table>\n<tr><th valign=\"top\"><img src=\"/icons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n<tr><th colspan=\"5\"><hr></th></tr>\n<tr><td valign=\"top\"><img src=\"/icons/back.gif\" alt=\"[PARENTDIR]\"></td><td><a href=\"/\">Parent Directory</a></td><td>&nbsp;</td><td align=\"right\">  - </td><td>&nbsp;</td></tr>\n" + HTTP_CONTENT + "<tr><th colspan=\"5\"><hr></th></tr>\n</table>\n<address>Mock HTTP Server</address>\n</body>\n</html>";
        private static final String DIRECTORY_HTML = "<tr><td valign=\"top\"><img src=\"/icons/folder.gif\" alt=\"[DIR]\"></td><td><a href=\"" + DIRECTORY_PATH + "\">" + DIRECTORY_PATH + "</a></td><td align=\"right\"> -  </td><td align=\"right\">  - </td><td>&nbsp;</td></tr>";
        private static final String FILE_HTML = "<tr><td valign=\"top\"><img src=\"/icons/compressed.gif\" alt=\"[   ]\"></td><td><a href=\"" + FILE_PATH + "\">" + FILE_PATH + "</a></td><td align=\"right\">" + FILE_DATE + "</td><td align=\"right\">" + FILE_SIZE + "</td><td>&nbsp;</td></tr>";

        private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");

        private Path serviceRootPath;

        HTTPMockServiceHandler(Path serviceRootPath) {
            assumeTrue(Files.exists(serviceRootPath));
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
                    response = getHTMLResponse(uriPath);
                    contentType = "text/html";
                } else if (Files.isRegularFile(responsePath) && !uriPath.endsWith("/")) {
                    response = readFile(responsePath);
                    contentType = "application/octet-stream";
                    long fileSize = Files.size(responsePath);
                    String fileDate = this.isoDateFormat.format(Files.getLastModifiedTime(responsePath).toMillis());
                    httpExchange.getResponseHeaders().add("Last-Modified", fileDate);
                    httpExchange.getResponseHeaders().add("Content-Length", "" + fileSize);
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
            httpExchange.getResponseHeaders().add("Server", "MockHTTP");
            httpExchange.sendResponseHeaders(httpStatus, response.length);
            httpExchange.getResponseBody().write(response);
            httpExchange.close();
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

        private byte[] getHTMLResponse(String uriPath) throws IOException {
            StringBuilder html = new StringBuilder();
            if (!uriPath.isEmpty() && !uriPath.endsWith("/")) {
                uriPath = uriPath.concat("/");
            }
            String prefix = uriPath;
            Path path = this.serviceRootPath.resolve(uriPath);
            try (Stream<Path> pathsStream = Files.walk(path, 1)) {
                Iterator<Path> paths = pathsStream.iterator();
                paths.next();
                while (paths.hasNext()) {
                    Path pathItem = paths.next();
                    if (Files.isDirectory(pathItem)) {
                        String directoryPath = pathItem.toString().replace(path.toString(), "").replace(path.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                        html.append(DIRECTORY_HTML.replaceAll(DIRECTORY_PATH, directoryPath + "/"));
                    } else {
                        long fileSize = Files.size(pathItem);
                        String fileDate = this.isoDateFormat.format(Files.getLastModifiedTime(pathItem).toMillis());
                        String filePath = pathItem.toString().replace(path.toString(), "").replace(path.getFileSystem().getSeparator(), "/").replaceAll("^/", "");
                        html.append(FILE_HTML.replaceAll(FILE_PATH, filePath).replaceAll(FILE_SIZE, "" + fileSize).replaceAll(FILE_DATE, fileDate));
                    }
                }
            }
            return RESPONSE_HTML.replace(PREFIX_CONTENT, prefix).replace(HTTP_CONTENT, html.toString()).getBytes();
        }
    }

}
