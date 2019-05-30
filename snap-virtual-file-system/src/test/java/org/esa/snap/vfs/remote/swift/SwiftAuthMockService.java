package org.esa.snap.vfs.remote.swift;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.logging.Logger;


class SwiftAuthMockService {

    static final String TOKEN = "c4760e89c8d945cd9a6fbfc7b71d6cbc";
    private static final String DOMAIN = "cloud_14547";
    private static final String PROJECT_ID = "c4761f89c8d940cd9a6dbfa7b72d6cba";
    private static final String USER = "swift_test";
    private static final String CREDENTIAL = "SwIfT0#";
    private HttpServer mockAuthServer;
    private String mockServiceAddress;

    SwiftAuthMockService(URL serviceAddress) throws IOException {
        this.mockAuthServer = HttpServer.create(new InetSocketAddress(serviceAddress.getPort()), 0);
        int port = this.mockAuthServer.getAddress().getPort();
        this.mockServiceAddress = serviceAddress.toString().replaceAll(":([\\d]+)", ":" + port);
        this.mockAuthServer.createContext(serviceAddress.getPath(), new SwiftAuthMockServiceHandler());
    }

    public static void main(String[] args) {
        try {
            SwiftAuthMockService mockService = new SwiftAuthMockService(new URL("http://localhost:0/mock-api/v3/auth/tokens"));
            mockService.start();
            Logger.getLogger(SwiftAuthMockService.class.getName()).info("Swift Auth mock service started at: " + mockService.getMockServiceAddress());
        } catch (IOException e) {
            Logger.getLogger(SwiftAuthMockService.class.getName()).severe("Unable to start Swift Auth mock service.\nReason: " + e.getMessage());
        }
    }

    void start() {
        this.mockAuthServer.start();
    }

    void stop() {
        this.mockAuthServer.stop(1);
    }

    String getMockServiceAddress() {
        return this.mockServiceAddress;
    }

    private class SwiftAuthMockServiceHandler implements HttpHandler {

        SwiftAuthMockServiceHandler() {
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            int httpStatus;
            String contentType = "text/plain";
            try {
                if (!httpExchange.getRequestMethod().toUpperCase().contentEquals("POST")) {
                    throw new IllegalArgumentException("Wrong HTTP method.");
                }
                String requestJson = getRequestJson(httpExchange.getRequestBody());
                if (requestJson.isEmpty()) {
                    throw new IllegalArgumentException("Missing request body");
                }
                String domain = requestJson.replaceAll("((.|\\n)*\"domain\".*\\s*\"name\":\\s*\"(.*)\"(.|\\n)*)", "$3");
                String projectId = requestJson.replaceAll("((.|\\n)*\"project\".*\\s*\"id\":\\s*\"(.*)\"(.|\\n)*)", "$3");
                String user = requestJson.replaceAll("((.|\\n)*\"user\"(.|\\n)*\\s*\"name\":\\s*\"(.*)\"(.|\\n)*)", "$4");
                String password = requestJson.replaceAll("((.|\\n)*\"user\"(.|\\n)*\\s*\"password\":\\s*\"(.*)\"(.|\\n)*)", "$4");
                if (!domain.contentEquals(DOMAIN) || !projectId.contentEquals(PROJECT_ID) || !user.contentEquals(USER) || !password.contentEquals(CREDENTIAL)) {
                    httpStatus = HttpURLConnection.HTTP_FORBIDDEN;
                } else {
                    httpExchange.getResponseHeaders().add("X-Subject-Token", TOKEN);
                    httpStatus = HttpURLConnection.HTTP_CREATED;
                }
            } catch (IllegalArgumentException ex) {
                httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
            } catch (Exception ex) {
                httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
            httpExchange.getResponseHeaders().add("Content-Type", contentType);
            httpExchange.getResponseHeaders().add("Server", "MockSwiftS3");
            httpExchange.sendResponseHeaders(httpStatus, 0);
            httpExchange.close();
        }

        private String getRequestJson(InputStream request) {
            String responseJson = "";
            try {
                byte[] data = new byte[request.available()];
                if (request.read(data) > -1) {
                    responseJson = new String(data);
                }
                request.close();
            } catch (Exception ignored) {
                //no data received
            }
            return responseJson;
        }
    }
}
