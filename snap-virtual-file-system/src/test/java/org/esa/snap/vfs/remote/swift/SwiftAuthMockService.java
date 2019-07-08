package org.esa.snap.vfs.remote.swift;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;


class SwiftAuthMockService {

    private static final String TOKEN = "c4760e89c8d945cd9a6fbfc7b71d6cbc";
    private static LocalDateTime expirationDate;

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

    static boolean isValidToken(String token) {
        return token.contentEquals(TOKEN) && expirationDate != null && expirationDate.isAfter(LocalDateTime.now(ZoneOffset.UTC));
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

        private static final String DOMAIN = "cloud_14547";
        private static final String PROJECT_ID = "c4761f89c8d940cd9a6dbfa7b72d6cba";
        private static final String USER = "swift_test";
        private static final String CREDENTIAL = "SwIfT0#";
        private static final long TOKEN_VALIDITY = 15L;
        private static final String EXPIRATION_DATE_NAME = "%expirationDate%";
        private static final String ISSUANCE_DATE_NAME = "%issuanceDate%";
        private static final String API_RESPONSE = "{\n" +
                "    \"token\": {\n" +
                "        \"expires_at\": \"" + EXPIRATION_DATE_NAME + "\",\n" +
                "        \"issued_at\": \"" + ISSUANCE_DATE_NAME + "\",\n" +
                "        \"methods\": [\n" +
                "            \"password\"\n" +
                "        ],\n" +
                "    }\n" +
                "}";

        private final DateTimeFormatter isoDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

        SwiftAuthMockServiceHandler() {
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] response;
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
                    response = "AccessDenied".getBytes();
                    httpStatus = HttpURLConnection.HTTP_FORBIDDEN;
                } else {
                    httpExchange.getResponseHeaders().add("X-Subject-Token", TOKEN);
                    LocalDateTime issuanceDate = LocalDateTime.now(ZoneOffset.UTC);
                    expirationDate = issuanceDate.plusMinutes(TOKEN_VALIDITY);
                    httpExchange.getResponseHeaders().add("expires_at", expirationDate.format(this.isoDateFormat));
                    String apiResponse = API_RESPONSE;
                    apiResponse = apiResponse.replace(EXPIRATION_DATE_NAME, expirationDate.format(this.isoDateFormat));
                    apiResponse = apiResponse.replace(ISSUANCE_DATE_NAME, issuanceDate.format(this.isoDateFormat));
                    response = apiResponse.getBytes();
                    httpStatus = HttpURLConnection.HTTP_CREATED;
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
            httpExchange.sendResponseHeaders(httpStatus, 0);
            httpExchange.getResponseBody().write(response);
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
