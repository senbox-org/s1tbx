package org.esa.snap.vfs.remote.swift;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * AuthorizationToken class for Swift Authorization Token
 *
 * @author Adrian Drăghici
 */
final class SwiftAuthenticationV3 {

    /**
     * The date-time format used.
     */
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private static final String DOMAIN_NAME = "%domain%";
    private static final String PROJECT_ID_NAME = "%projectId%";
    private static final String USER_NAME = "%user%";
    private static final String CREDENTIAL_NAME = "%password%";
    private static final String API_REQUEST = "{\n" +
            "    \"auth\": {\n" +
            "        \"identity\": {\n" +
            "            \"methods\": [\n" +
            "                \"password\"\n" +
            "            ],\n" +
            "            \"password\": {\n" +
            "                \"user\": {\n" +
            "                    \"domain\": {\n" +
            "                        \"name\": \"" + DOMAIN_NAME + "\"\n" +
            "                    },\n" +
            "                    \"name\": \"" + USER_NAME + "\",\n" +
            "                    \"password\": \"" + CREDENTIAL_NAME + "\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"scope\": {\n" +
            "            \"project\": {\n" +
            "                \"id\": \"" + PROJECT_ID_NAME + "\"\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private LocalDateTime expirationDate;

    private String authAddress;
    private String domain;
    private String projectId;
    private String user;
    private String password;

    private String authorizationToken;

    /**
     * Initializes the SwiftAuthenticationV3 class.
     *
     * @param authAddress The address of authentication service used by OpenStack Swift service (mandatory if credentials is provided)
     * @param domain      The domain name OpenStack Swift credential
     * @param projectId   The account ID/ Project/ Tenant name OpenStack Swift credential
     * @param user        The username OpenStack Swift credential
     * @param password    The password OpenStack Swift credential
     */
    SwiftAuthenticationV3(String authAddress, String domain, String projectId, String user, String password) {
        this.expirationDate = null; //no expires
        this.authAddress = authAddress;
        this.domain = domain;
        this.projectId = projectId;
        this.user = user;
        this.password = password;
        this.authorizationToken = null;
    }

    /**
     * Check whether the token is valid (not expired)
     *
     * @return {@code true} if the token is valid (expiration date is after now)
     */
    boolean isValid() {
        return this.expirationDate != null && this.expirationDate.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    private String requestNewAuthorizationToken() {
        HttpURLConnection connection = null;
        try {
            URL authUrl = new URL(this.authAddress);
            connection = (HttpURLConnection) authUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("user-agent", "SNAP Virtual File System");
            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter out = new PrintWriter(outputStream)) {
                String apiRequest = API_REQUEST;
                apiRequest = apiRequest.replace(DOMAIN_NAME, this.domain);
                apiRequest = apiRequest.replace(PROJECT_ID_NAME, this.projectId);
                apiRequest = apiRequest.replace(USER_NAME, this.user);
                apiRequest = apiRequest.replace(CREDENTIAL_NAME, this.password);
                out.print(apiRequest);
                out.flush();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    String newAuthorizationToken = connection.getHeaderField("X-Subject-Token");
                    InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String result = bufferedReader.lines().collect(Collectors.joining("\n"));
                    String newExpirationDate = result.replaceAll("[\\s\\S]*\"expires_at\": \"(.*?)\",[\\s\\S]*", "$1");
                    this.expirationDate = LocalDateTime.parse(newExpirationDate, ISO_DATE_TIME);
                    return newAuthorizationToken;
                }
            }
        } catch (Exception ignored) {
            //nothing
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * Creates the authorization token used for Swift authentication.
     *
     * @return The Swift authorization token
     * @see <a href="https://developer.openstack.org/api-ref/identity/v3/index.html">Identity API v3</a>
     */
    String getAuthorizationToken() {
        if (this.domain == null || this.domain.isEmpty() || this.projectId == null || this.projectId.isEmpty() || this.user == null || this.user.isEmpty() || this.password == null || this.password.isEmpty()) {
            return null;
        }
        if (!isValid()) {
            this.authorizationToken = requestNewAuthorizationToken();
        }
        return this.authorizationToken;
    }
}
