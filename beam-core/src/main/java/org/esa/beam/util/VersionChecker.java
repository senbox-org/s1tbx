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
package org.esa.beam.util;

import com.bc.ceres.core.runtime.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class VersionChecker {

    private String remoteVersionUrlString;
    private File localVersionFile;
    private static final String VERSION_PREFIX = "VERSION ";

    // todo - use application.properties with version ID set by Maven (resource Filter!)
    public VersionChecker() {
        this(new File(SystemUtils.getApplicationHomeDir(), "VERSION.txt"),
             SystemUtils.getApplicationRemoteVersionUrl());
    }

    public VersionChecker(File localVersionFile, String remoteVersionUrlString) {
        this.localVersionFile = localVersionFile;
        this.remoteVersionUrlString = remoteVersionUrlString;
    }

    public String getRemoteVersionUrlString() {
        return remoteVersionUrlString;
    }

    public void setRemoteVersionUrlString(String remoteVersionUrlString) {
        this.remoteVersionUrlString = remoteVersionUrlString;
    }

    public File getLocalVersionFile() {
        return localVersionFile;
    }

    public void setLocalVersionFile(File localVersionFile) {
        this.localVersionFile = localVersionFile;
    }

    public int compareVersions() throws IOException {
        final String remoteVersion = getRemoteVersion();
        final String localVersion = getLocalVersion();
        return compareVersions(localVersion, remoteVersion);
    }

    static int compareVersions(String localVersion, String remoteVersion) {
        if (localVersion.startsWith(VERSION_PREFIX) && remoteVersion.startsWith(VERSION_PREFIX)) {
            Version v1 = Version.parseVersion(localVersion.substring(VERSION_PREFIX.length()));
            Version v2 = Version.parseVersion(remoteVersion.substring(VERSION_PREFIX.length()));
            return v1.compareTo(v2);
        }
        return localVersion.compareTo(remoteVersion);
    }

    public String getLocalVersion() throws IOException {
        try {
            return getVersion(getLocalVersionFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public String getRemoteVersion() throws IOException {
        try {
            return getVersion(new URL(getRemoteVersionUrlString()));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private String getVersion(final URL url) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        final String line;
        try {
            line = reader.readLine();
        } finally {
            reader.close();
        }
        if (line == null || !line.startsWith(VERSION_PREFIX)) {
            throw new IOException("unexpected version file format");
        }
        return line;
    }
}
