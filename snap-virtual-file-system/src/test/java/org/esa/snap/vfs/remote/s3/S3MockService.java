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


class S3MockService {

    private HttpServer mockServer;

    S3MockService(URL serviceAddress, Path serviceRootPath) throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        mockServer.createContext(serviceAddress.getPath(), new S3MockServiceHandler(serviceRootPath));
    }

    void start() {
        mockServer.start();
    }

    void stop() {
        mockServer.stop(1);
    }

    private class S3MockServiceHandler implements HttpHandler {

        static final String xmlFile = "index.xml";
        private Path serviceRootPath;

        S3MockServiceHandler(Path serviceRootPath) {
            this.serviceRootPath = serviceRootPath;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] response;
            try {
                String urlPath = httpExchange.getRequestURI().getPath();
                String prefix = getRequestParameter(httpExchange.getRequestURI().getQuery(), "prefix");
                if (!prefix.isEmpty()) {
                    if (!urlPath.endsWith("/") && !prefix.startsWith("/")) {
                        urlPath = urlPath.concat("/");
                    }
                    urlPath = urlPath.concat(prefix);
                }
                Path responsePath = serviceRootPath.resolve(urlPath.replaceAll("^/", "").replaceAll("/{2,}", "/"));
                if (Files.isDirectory(responsePath)) {
                    responsePath = responsePath.resolve(xmlFile);
                }
                if (Files.exists(responsePath)) {
                    response = readFile(responsePath);
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = "Not Found".getBytes();
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.length);
                }
            } catch (Exception ex) {
                response = ex.getMessage().getBytes();
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, response.length);
            }
            httpExchange.getResponseBody().write(response);
            httpExchange.close();
        }

        private String getRequestParameter(String query, String key) {
            String value = "";
            if (query != null && key != null) {
                value = query.replaceAll("(.*" + key + "=([^&]*).*)", "$2");
            }
            return value;
        }

        private byte[] readFile(Path inputFile) throws IOException {
            InputStream is = Files.newInputStream(inputFile);
            byte data[] = new byte[is.available()];
            is.read(data);
            is.close();
            return data;
        }
    }

}
