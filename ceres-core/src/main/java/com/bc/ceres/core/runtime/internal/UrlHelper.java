package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ProxyConfig;

import java.net.*;
import java.io.IOException;

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
}
