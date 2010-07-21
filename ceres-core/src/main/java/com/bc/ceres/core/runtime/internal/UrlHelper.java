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
