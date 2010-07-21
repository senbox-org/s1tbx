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

package com.bc.ceres.core.runtime;

import com.bc.ceres.core.Assert;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProxyConfig) {
            ProxyConfig proxyConfig = (ProxyConfig) obj;
            return host.equals(proxyConfig.host)
                    && port == proxyConfig.port
                    && authorizationUsed == proxyConfig.authorizationUsed;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return host.hashCode() + port;
    }
}
