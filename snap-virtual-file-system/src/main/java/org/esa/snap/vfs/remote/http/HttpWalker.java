package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.remote.AbstractRemoteWalker;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.esa.snap.vfs.remote.VFSPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private final String servicePath;

    /**
     * Creates the new walker for HTTP  VFS
     *
     * @param address   The address of HTTP service. (mandatory)
     * @param delimiter The VFS path delimiter
     * @param root      The root of HTTP provider
     */
    HttpWalker(String address, String delimiter, String root, IRemoteConnectionBuilder remoteConnectionBuilder) {
        super(remoteConnectionBuilder);

        this.address = address;
        this.delimiter = delimiter;
        this.root = root;
        this.servicePath = address.replaceAll(".*//.*?(" + delimiter + ".*)", "$1");
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public synchronized List<BasicFileAttributes> walk(VFSPath dir) throws IOException {
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

        String urlAddress = urlAsString.toString();
        String htmlResponse = HttpUtils.readResponse(urlAddress, this.remoteConnectionBuilder, this.root);
        Document document = Jsoup.parse(htmlResponse, urlAddress);
        if (!dirPathAsString.endsWith(this.delimiter)) {
            dirPathAsString += this.delimiter;
        }
        return parseElements(document, dirPathAsString);
    }

    private List<BasicFileAttributes> parseElements(Document document, String prefix) {
        Set<BasicFileAttributes> items = new LinkedHashSet<>();
        Pattern p = Pattern.compile("<a href=\"(.*?)\">.*?</a>");
        Elements htmlTables = document.select("table");
        if (htmlTables.isEmpty() || htmlTables.size() > 1) {
            throw new IllegalArgumentException("Unsupported HTTP VFS service.\nReason: invalid/unknown VFS structure.");
        }
        Element htmlTable = htmlTables.first();
        Matcher m = p.matcher(htmlTable.html());
        boolean externalDirs = false;
        while (m.find()) {
            String name = m.group(1);
            name = name.replaceAll("^" + this.servicePath, "");
            if (!name.startsWith(this.delimiter)) {
                name = this.delimiter.concat(name);
            }
            String parent = prefix.replaceAll("^" + this.root, "");
            if (!name.isEmpty() && !name.contains("?") && !name.contains("#") && !name.equals("/") && isValidPath(name, prefix)) {
                if (!parent.contentEquals(name)) {
                    name = name.replaceAll("^" + parent, "");
                }
                name = name.replaceAll("^/", "");
                if (name.endsWith("/")) {
                    if (name.startsWith("http://") || name.startsWith("https://")) {
                        externalDirs = true;
                    } else {
                        items.add(VFSFileAttributes.newDir(prefix + name));
                    }
                } else {
                    String fileUrl;
                    String filePath;
                    if (name.startsWith("http://") || name.startsWith("https://")) {
                        fileUrl = name;
                        name = name.replaceAll(".*/(.*)", "$1");
                        filePath = prefix.concat(name);
                    } else {
                        fileUrl = this.address;
                        String filePathUrl;
                        if (name.contains("/")) {
                            name = name.startsWith("/") ? name.substring(1) : name;
                            filePathUrl = name;
                            name = name.replaceAll(".*/(.*)", "$1");
                            filePath = prefix.concat(name);
                        } else {
                            filePath = prefix.concat(name);
                            filePathUrl = filePath.replaceAll(this.root + "/?", "");
                        }
                        if (!fileUrl.endsWith("/")) {
                            fileUrl = fileUrl.concat("/");
                        }
                        fileUrl = fileUrl.concat(filePathUrl);
                    }
                    RegularFileMetadataCallback fileSizeQueryCallback = new RegularFileMetadataCallback(fileUrl, this.remoteConnectionBuilder, this.root);
                    BasicFileAttributes regularFileAttributes = VFSFileAttributes.newFile(filePath, fileSizeQueryCallback);
                    items.add(regularFileAttributes);
                }
            }
        }
        if (externalDirs && items.isEmpty()) {
            throw new IllegalArgumentException("Unsupported HTTP VFS service.\nReason: External directories unsupported. ");
        }
        return new ArrayList<>(items);
    }

    private boolean isValidPath(String target, String current) {
        if (target != null && !target.isEmpty() && current != null && !current.isEmpty()) {
            Path currentPath = NioPaths.get(current);
            Path parentPath = currentPath.getParent();
            if (parentPath != null) {
                String parent = parentPath.toString();
                if (target.endsWith(this.delimiter)) {
                    parent += this.delimiter;
                }
                if (parent.endsWith(target)) {
                    Path newPath = currentPath.resolve(target);
                    return Files.exists(newPath);
                }
            }
            return true;
        }
        return false;
    }
}
