package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * IRemoteConnectionBuilder for provide HttpURLConnection on VFS
 *
 * @author Jean Coravu
 */
public interface IRemoteConnectionBuilder {

    HttpURLConnection buildConnection(String fileSystemRoot, URL url, String method, Map<String, String> requestProperties) throws IOException;
}
