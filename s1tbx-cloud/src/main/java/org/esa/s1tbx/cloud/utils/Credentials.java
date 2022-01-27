/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.cloud.utils;

import org.esa.snap.runtime.Config;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import java.util.prefs.Preferences;

/**
 * Created by lveci on 2/22/2017.
 */
public class Credentials {

    private final Preferences credentialsPreferences = Config.instance("Credentials").load().preferences();

    private static final String PREFIX = "credential.";
    private static final String USER = ".user";
    private static final String PASSWORD = ".password";

    private static Credentials instance;

    private StandardPBEStringEncryptor encryptor;

    private Credentials() {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("Mzg1YWFkNjY0MjA2MGY1ZTIyMThjYjFj");
    }

    public static Credentials instance() {
        if (instance == null) {
            instance = new Credentials();
        }
        return instance;
    }

    public void put(final String host, final String user, final String password) throws Exception {
        credentialsPreferences.put(PREFIX + host + USER, user);
        credentialsPreferences.put(PREFIX + host + PASSWORD, encryptor.encrypt(password));
        credentialsPreferences.flush();
    }

    public CredentialInfo get(final String host) {
        final String user = credentialsPreferences.get(PREFIX + host + USER, null);
        final String encryptedPassword = credentialsPreferences.get(PREFIX + host + PASSWORD, null);
        String password = encryptor.decrypt(encryptedPassword);

        return user == null || password == null ? null : new CredentialInfo(user, password);
    }

    public static class CredentialInfo {
        private final String user;
        private final String password;

        CredentialInfo(final String user, final String password) {
            this.user = user;
            this.password = password;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }
    }
}
