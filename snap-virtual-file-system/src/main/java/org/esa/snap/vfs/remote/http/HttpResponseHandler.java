package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Response Handler for HTTP VFS.
 *
 * @author Adrian Drăghici
 */
class HttpResponseHandler {

    private final Document doc;
    private String prefix;
    private String serviceAddress;
    private String root;
    private HttpWalker walker;

    /**
     * Creates the new response handler for HTTP VFS.
     *
     * @param doc    The HTML document to parse
     * @param prefix The VFS path to traverse
     */
    HttpResponseHandler(Document doc, String prefix, String serviceAddress, String root, HttpWalker walker) {
        this.doc = doc;
        this.prefix = prefix;
        this.serviceAddress = serviceAddress;
        this.root = root;
        this.walker = walker;
    }

    /**
     * Creates the authorization token used for HTTP authentication.
     *
     * @param username The username HTTP credential
     * @param password The password HTTP credential
     * @return The authorization token
     */
    private static String getAuthorizationToken(String username, String password) {
        return (!StringUtils.isNotNullAndNotEmpty(username) && !StringUtils.isNotNullAndNotEmpty(password)) ? Base64.getEncoder().encodeToString((username + ":" + password).getBytes()) : "";
    }

    /**
     * Creates the connection channel.
     *
     * @param url               The URL address to connect
     * @param method            The HTTP method (GET POST DELETE etc)
     * @param requestProperties The properties used on the connection
     * @param username          The username HTTP credential
     * @param password          The password HTTP credential
     * @return The connection channel
     * @throws IOException If an I/O error occurs
     */
    static HttpURLConnection getConnectionChannel(URL url, String method, Map<String, String> requestProperties, String username, String password) throws IOException {
        HttpURLConnection connection = buildConnection(url, method, requestProperties, username, password);
        int responseCode = connection.getResponseCode();
        if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            /* error from server */
            throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
        } else {
            return connection;
        }
    }

    static HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties, String username, String password) throws IOException {
        String authorizationToken = getAuthorizationToken(username, password);
        HttpURLConnection connection;
        if (url.getProtocol().equals("https")) {
            connection = (HttpsURLConnection) url.openConnection();
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        method = method.toUpperCase();
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        if (authorizationToken != null && !authorizationToken.isEmpty())
            connection.setRequestProperty("authorization", "Basic " + authorizationToken);
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestPropertiesSet = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestPropertiesSet) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        connection.setRequestProperty("user-agent", "SNAP Virtual File System");
        return connection;
    }

    /**
     * Parse the HTML document and extract the VFS path and file attributes.
     * Adds the new path to the list of VFS paths for files and directories.
     * Current implementation works only with Apache Server ('Index of' pages).
     * On the future will add implementation for other HTTP servers as needed.
     */
    List<BasicFileAttributes> getElements() throws IOException {
        List<BasicFileAttributes> items = new ArrayList<>();
        Pattern p = Pattern.compile("<a href=\"(.*?)\">.*?</a>");
        Matcher m = p.matcher(doc.html());
        while (m.find()) {
            String name = m.group(1);
            if (!name.isEmpty() && !name.startsWith("/") && !name.startsWith("?") && !name.equals("/")) {
                if (name.endsWith("/")) {
                    items.add(VFSFileAttributes.newDir(prefix + name));
                } else {
                    String filePath = prefix.concat(name);
                    String filePathUrl = filePath.replaceAll(root + "/?", "");
                    String fileUrl = this.serviceAddress;
                    if (!fileUrl.endsWith("/")) {
                        fileUrl = fileUrl.concat("/");
                    }
                    fileUrl = fileUrl.concat(filePathUrl);
                    items.add(walker.getVFSFileAttributes(fileUrl, filePath));
                }
            }
        }
        return items;
    }
}
