package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by jcoravu on 19/4/2019.
 */
public interface IRemoteConnectionBuilder {

    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException;
}
