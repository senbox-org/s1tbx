package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.ObjectStorageFileAttributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Response Handler for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class HttpResponseHandler {

    /**
     * The keyword of data unit measure : KB (Kilobyte)
     */
    private static final String KB = "K";

    /**
     * The keyword of data unit measure : MB (Megabyte)
     */
    private static final String MB = "M";

    /**
     * The keyword of data unit measure : GB (Gigabyte)
     */
    private static final String GB = "G";

    /**
     * The keyword of data unit measure : TB (Terabyte)
     */
    private static final String TB = "T";

    /**
     * The keyword of data unit measure : PB (Petabyte)
     */
    private static final String PB = "P";

    private final Document doc;
    private final List<BasicFileAttributes> items;
    private String prefix;

    /**
     * Creates the new response handler for HTTP Object Storage VFS.
     *
     * @param doc    The HTML document to parse
     * @param prefix The VFS path to traverse
     * @param items  The list with VFS paths for files and directories
     */
    HttpResponseHandler(Document doc, String prefix, List<BasicFileAttributes> items) {
        this.doc = doc;
        this.prefix = prefix;
        this.items = items;
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
    static URLConnection getConnectionChannel(URL url, String method, Map<String, String> requestProperties, String username, String password) throws IOException {
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
        int responseCode = connection.getResponseCode();

        if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            /* error from server */
            throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
        } else {
            return connection;
        }
    }

    /**
     * Parse the HTML document and extract the VFS path and file attributes.
     * Adds the new path to the list of VFS paths for files and directories.
     * Current implementation works only with Apache Server ('Index of' pages).
     * On the future will add implementation for other HTTP servers as needed.
     */
    void getElements() {
        Elements rows = doc.getElementsByTag("tr");
        if (!rows.isEmpty()) {
            rows.remove(0);
            rows.remove(0);
            rows.remove(rows.size() - 1);
            for (Element row : rows) {
                Elements columns = row.getElementsByTag("td");
                String name = columns.get(1).selectFirst("a").attr("href");
                if (!name.equals("/") && !name.startsWith("/")) {
                    if (name.endsWith("/")) {
                        items.add(ObjectStorageFileAttributes.newDir(prefix + name));
                    } else {
                        String lastModified = columns.get(2).text();
                        long size = calculateSize(columns.get(3).text());
                        items.add(ObjectStorageFileAttributes.newFile(prefix + name, size, lastModified));
                    }
                }
            }
        }
    }

    /**
     * Returns the size of a file by converting it to base data unit measure (byte)
     *
     * @param sizeS The size of a file string
     * @return the size of a file in bytes
     */
    private long calculateSize(String sizeS) {
        String sizeUnit = sizeS.replaceAll("\\d", "");
        sizeS = sizeS.replaceAll("\\D", "");
        float size = Float.parseFloat(sizeS);
        switch (sizeUnit) {
            case KB:
                return (long) (size * Math.pow(1024, 1));
            case MB:
                return (long) (size * Math.pow(1024, 2));
            case GB:
                return (long) (size * Math.pow(1024, 3));
            case TB:
                return (long) (size * Math.pow(1024, 4));
            case PB:
                return (long) (size * Math.pow(1024, 5));
            default:
                break;
        }
        return (long) size;
    }

}
