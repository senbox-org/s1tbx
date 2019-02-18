package org.esa.snap.core.dataio.vfs.remote.object_storage.http;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileAttributes;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Walker for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class HttpWalker implements ObjectStorageWalker {

    HttpWalker() {

    }

    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = HttpResponseHandler.getConnectionChannel(new URL(address), "GET", null);
        return ObjectStorageFileAttributes.newFile(prefix, urlConnection.getContentLengthLong(), urlConnection.getHeaderField("last-modified"));
    }

    public List<BasicFileAttributes> walk(String prefix, String delimiter) throws IOException {
        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        URL url = new URL(new HttpFileSystemProvider().getProviderAddress() + delimiter + prefix.replace(HttpFileSystemProvider.HTTP_ROOT, ""));
        URLConnection connection = new HttpFileSystemProvider().getProviderConnectionChannel(url, "GET", null);
        Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());
        HttpResponseHandler handler = new HttpResponseHandler(doc, prefix, items);
        handler.getElements();
        return items;
    }

}
