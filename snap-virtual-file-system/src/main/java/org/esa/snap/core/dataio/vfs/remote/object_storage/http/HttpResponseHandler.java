package org.esa.snap.core.dataio.vfs.remote.object_storage.http;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileAttributes;
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
 * Response Handler for EO Cloud OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class HttpResponseHandler {

    private static final String KB = "K";
    private static final String MB = "M";
    private static final String GB = "G";
    private static final String TB = "T";
    private static final String PB = "P";

    private final Document doc;
    private String prefix;
    private final List<BasicFileAttributes> items;

    HttpResponseHandler(Document doc, String prefix, List<BasicFileAttributes> items) {
        this.doc = doc;
        this.prefix = prefix;
        this.items = items;
    }

    private static String getAuthorizationToken(String username, String password) {
        return (username != null && !username.isEmpty() && password != null && !password.isEmpty()) ? Base64.getEncoder().encodeToString(("" + username + ":" + password + "").getBytes()) : "";
    }

    static URLConnection getConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        String authorizationToken = getAuthorizationToken(HttpFileSystemProvider.getUsername(), HttpFileSystemProvider.getPassword());
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
            connection.setRequestProperty("X-Auth-Token", authorizationToken);
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestProperties_set = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestProperties_set) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        connection.setRequestProperty("user-agent", "SNAP Virtual File System");
        int responseCode = connection.getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            /* error from server */
            throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
        } else {
            return connection;
        }
    }

    void getElements() {
        Elements rows = doc.getElementsByTag("tr");
        if (!rows.isEmpty()) {
            rows.remove(0);
            rows.remove(0);
            rows.remove(rows.size() - 1);
            prefix = prefix.isEmpty() ? HttpFileSystemProvider.HTTP_ROOT : prefix;
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

    private long calculateSize(String size_s) {
        String sizeUnit = size_s.replaceAll("\\d", "");
        size_s = size_s.replaceAll("\\D", "");
        float size = Float.parseFloat(size_s);
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
        }
        return (long) size;
    }

}
