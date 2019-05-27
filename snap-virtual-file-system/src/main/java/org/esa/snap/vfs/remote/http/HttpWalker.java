package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteWalker;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walker for HTTP VFS.
 *
 * @author Adrian Drăghici
 */
class HttpWalker extends AbstractRemoteWalker {

    private final String address;
    private final String delimiter;
    private final String root;

    /**
     * Creates the new walker for HTTP  VFS
     *
     * @param address   The address of HTTP service. (mandatory)
     * @param delimiter The VFS path delimiter
     * @param root      The root of S3 provider
     */
    HttpWalker(String address, String delimiter, String root, IRemoteConnectionBuilder remoteConnectionBuilder) {
        super(remoteConnectionBuilder);

        this.address = address;
        this.delimiter = delimiter;
        this.root = root;
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public synchronized List<BasicFileAttributes> walk(Path dir) throws IOException {
        StringBuilder urlAsString = new StringBuilder();
        if (this.address.endsWith(this.delimiter)) {
            int endIndex = this.address.length() - this.delimiter.length();
            urlAsString.append(this.address, 0, endIndex); // do not write the file separator at the end
        } else {
            urlAsString.append(this.address);
        }
        String dirPathAsString = dir.toString();
        String urlPathAsString = dirPathAsString;
        if (urlPathAsString.startsWith(this.root)) {
            urlPathAsString = urlPathAsString.substring(this.root.length());
        }
        if (!urlPathAsString.startsWith(this.delimiter)) {
            urlAsString.append(this.delimiter);
        }
        urlAsString.append(urlPathAsString);

        URL url = new URL(urlAsString.toString());
        Document document;
        HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(url, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                try (InputStream inputStream = connection.getInputStream();
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 10 * 1024)) {

                    document = Jsoup.parse(bufferedInputStream, "UTF-8", url.toString());
                }
            } else {
                throw new IOException(url.toString() + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }

        if (!dirPathAsString.endsWith(this.delimiter)) {
            dirPathAsString += this.delimiter;
        }
        return parseElements(document, dirPathAsString);
    }

    private List<BasicFileAttributes> parseElements(Document document, String prefix) throws IOException {
        List<BasicFileAttributes> items = new ArrayList<>();
        Pattern p = Pattern.compile("<a href=\"(.*?)\">.*?</a>");
        Matcher m = p.matcher(document.html());
        while (m.find()) {
            String name = m.group(1);
            if (!name.isEmpty() && !name.startsWith("/") && !name.startsWith("?") && !name.equals("/")) {
                if (name.endsWith("/")) {
                    items.add(VFSFileAttributes.newDir(prefix + name));
                } else {
                    String filePath = prefix.concat(name);
                    String filePathUrl = filePath.replaceAll(this.root + "/?", "");
                    String fileUrl = this.address;
                    if (!fileUrl.endsWith("/")) {
                        fileUrl = fileUrl.concat("/");
                    }
                    fileUrl = fileUrl.concat(filePathUrl);
                    RegularFileMetadataCallback fileSizeQueryCallback = new RegularFileMetadataCallback(fileUrl, this.remoteConnectionBuilder);
                    BasicFileAttributes regularFileAttributes = VFSFileAttributes.newFile(filePath, fileSizeQueryCallback);
                    items.add(regularFileAttributes);
                }
            }
        }
        return items;
    }
}
