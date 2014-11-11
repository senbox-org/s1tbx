/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ProxyConfig;

import java.io.File;
import java.net.*;
import java.io.IOException;

import static com.bc.ceres.core.runtime.Constants.*;

public class UrlHelper {

    public static boolean existsResource(String urlString, ProxyConfig proxyConfig) {
        try {
            URLConnection urlConnection = openConnection(urlString, proxyConfig, "HEAD");
            urlConnection.connect();
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                httpURLConnection.disconnect();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static URLConnection openConnection(String urlString, ProxyConfig proxyConfig, String request) throws IOException {
        return openConnection(new URL(urlString), proxyConfig, request);
    }

    public static URLConnection openConnection(URL url, ProxyConfig proxyConfig, String request) throws IOException {
        URLConnection urlConnection;
        if (proxyConfig == ProxyConfig.NULL) {
            urlConnection = url.openConnection();
        } else {
            urlConnection = url.openConnection(createProxy(proxyConfig));
        }
        if (urlConnection instanceof HttpURLConnection) {
            HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
            httpUrlConnection.setRequestMethod(request);
        }
        if (proxyConfig.isAuthorizationUsed()) {
            addProxyAuthorization(urlConnection, proxyConfig);
        }
        return urlConnection;
    }

    private static void addProxyAuthorization(URLConnection urlConnection, ProxyConfig proxyConfig) {
        // from http://floatingsun.net/articles/java-proxy.html
        String s = proxyConfig.getUsername() + ':' + new String(proxyConfig.getPassword());
        byte[] bytes = s.getBytes();
        // todo - this encoder might not be available on Mac OS X!!!
        sun.misc.BASE64Encoder base64Encoder = new sun.misc.BASE64Encoder();
        urlConnection.setRequestProperty("Proxy-Authorization",
                                         "Basic " +
                                                 base64Encoder.encode(bytes));
    }

    private static Proxy createProxy(ProxyConfig proxyConfig) {
        InetSocketAddress socketAddress = new InetSocketAddress(proxyConfig.getHost(),
                                                                proxyConfig.getPort());
        return new Proxy(Proxy.Type.HTTP, socketAddress);
    }

    public static URI urlToUri(URL url) throws URISyntaxException {
        return new URI(url.toExternalForm().replace(" ", "%20"));
    }

    public static File urlToFile(URL url) {
        try {
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                String path = url.getPath();
                int jarEntrySepPos = path.lastIndexOf("!/");
                if (jarEntrySepPos > 0) {
                    path = path.substring(0, jarEntrySepPos);
                }
                url = new URL(path);
            }
            URI uri = urlToUri(url);
            // Exhaustive checking on uri required to prevent
            // File.File(URI) constructor from throwing an IllegalArgumentException
            if ("file".equalsIgnoreCase(uri.getScheme())
                    && uri.isAbsolute()
                    && !uri.isOpaque()
                    && uri.getAuthority() == null
                    && uri.getFragment() == null
                    && uri.getQuery() == null) {
                return new File(uri);
            }
        } catch (MalformedURLException e) {
            // ignored
        } catch (URISyntaxException e) {
            // ignored
        }
        return null;
    }

    public static URL fileToUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static URL locationToManifestUrl(URL locationUrl) {
        String location = locationUrl.toExternalForm();

        String xmlUrlString;
        if (JarFilenameFilter.isJarName(location)) {
            xmlUrlString = "jar:" + location + "!/" + MODULE_MANIFEST_NAME;
        } else if (location.endsWith("/")) {
            xmlUrlString = location + MODULE_MANIFEST_NAME;
        } else {
            return null;
        }

        try {
            return new URL(xmlUrlString);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL manifestToLocationUrl(URL manifestUrl) {
        String location = manifestUrl.toExternalForm();
        if (!location.endsWith(MODULE_MANIFEST_NAME)) {
            return null;
        }
        location = location.substring(0, location.length() - MODULE_MANIFEST_NAME.length());
        location = location.replace(" ", "%20");  // fixes bug in maven surefire plugin

        // A JAR URL?
        String prefix = "jar:";
        String suffix = "!/";
        if (location.startsWith(prefix) && location.endsWith(suffix)) {
            location = location.substring(prefix.length(), location.length() - suffix.length());
        }

        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
