/*
 * $Id: VersionChecker.java,v 1.2 2007/04/12 10:27:37 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class VersionChecker  {

    private String _remoteVersionUrlString;
    private File _localVersionFile;

    public VersionChecker() {
        // todo - use application.properties with version ID set by Maven (resource Filter!)
        _remoteVersionUrlString = SystemUtils.BEAM_HOME_PAGE + "software/version.txt";
        _localVersionFile = new File(SystemUtils.getBeamHomeDir(), "VERSION.txt");
    }

    public String getRemoteVersionUrlString() {
        return _remoteVersionUrlString;
    }

    public void setRemoteVersionUrlString(String remoteVersionUrlString) {
        _remoteVersionUrlString = remoteVersionUrlString;
    }

    public File getLocalVersionFile() {
        return _localVersionFile;
    }

    public void setLocalVersionFile(File localVersionFile) {
        _localVersionFile = localVersionFile;
    }

    public int compareVersions() throws IOException {
        final String remoteVersion = getRemoteVersion();
        final String localVersion = getLocalVersion();
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
        if (line == null || !line.startsWith("VERSION ")) {
            throw new IOException("unexpected version file format");
        }
        return line;
    }
}
