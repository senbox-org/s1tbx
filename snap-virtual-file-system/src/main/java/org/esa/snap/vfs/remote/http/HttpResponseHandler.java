package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.jsoup.nodes.Document;

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
                    items.add(walker.readFileAttributes(fileUrl, filePath));
                }
            }
        }
        return items;
    }
}
