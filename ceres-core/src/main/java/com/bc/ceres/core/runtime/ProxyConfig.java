package com.bc.ceres.core.runtime;

import com.bc.ceres.core.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class ProxyConfig {
    public static final ProxyConfig NULL = new ProxyConfig();

    private String host;
    private int port;
    private boolean authorizationUsed;
    private String username;
    private char[] password;

    public ProxyConfig() {
        host = "";
        username = "";
        password = new char[0];
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        Assert.notNull(host, "host");
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAuthorizationUsed() {
        return authorizationUsed;
    }

    public void setAuthorizationUsed(boolean authorizationUsed) {
        this.authorizationUsed = authorizationUsed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Assert.notNull(username, "username");
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        Assert.notNull(password, "password");
        this.password = password;
    }

    public URLConnection openConnection(URL url) throws IOException {
        URLConnection urlConnection;
        if (this == NULL) {
            urlConnection = url.openConnection();
        } else {
            urlConnection = url.openConnection(createProxy());
            if (isAuthorizationUsed()) {
                addProxyAuthorization(urlConnection);
            }
        }
        return urlConnection;
    }

    public String getScrambledPassword() {
        if (password.length > 0) {
            return scramble(new String(password));
        } else {
            return "";
        }
    }

    public void setScrambledPassword(String password) {
        if (password != null && password.length() > 0) {
            setPassword(descramble(password).toCharArray());
        } else {
            setPassword(new char[0]);
        }
    }

    static String scramble(String epw) {
        String s = extra();
        String p = s + epw;
        return swap(p);
    }

    static String descramble(String cpw) {
        String p = swap(cpw);
        String s = extra();
        return p.substring(s.length());
    }

    private static String swap(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length / 2; i += 2) {
            char tmp = chars[chars.length - i - 1];
            chars[chars.length - i - 1] = chars[i];
            chars[i] = tmp;
        }
        return new String(chars);
    }

    private static String extra() {
        final int[] off = new int[]{17, 19, 11, 5, 4, 23, 3, 31, 1, 37, 13, 50, 38};
        char[] chars = new char[off.length];
        for (int i = 0; i < off.length; i++) {
            chars[i] = (char) (' ' + off[i]);
        }
        return new String(chars);
    }

    private Proxy createProxy() {
        InetSocketAddress socketAddress = new InetSocketAddress(getHost(), getPort());
        return new Proxy(Proxy.Type.HTTP, socketAddress);
    }

    private void addProxyAuthorization(URLConnection urlConnection) {
        // from http://floatingsun.net/articles/java-proxy.html
        String s = getUsername() + ':' + new String(getPassword());
        byte[] bytes = s.getBytes();
        urlConnection.setRequestProperty("Proxy-Authorization",
                                         "Basic " +
                                                 new sun.misc.BASE64Encoder().encode(bytes));
    }

}
