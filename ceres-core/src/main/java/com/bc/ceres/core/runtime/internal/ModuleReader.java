/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ProxyConfig;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.bc.ceres.core.runtime.Constants.MODULE_MANIFEST_NAME;

public class ModuleReader {

    private Logger logger;
    public static final String[] NO_LIBS = new String[0];

    public ModuleReader(Logger logger) {
        this.logger = logger;
    }


    public ModuleImpl readFromLocation(File locationFile) throws CoreException {
        final ModuleImpl module;
        if (locationFile.isDirectory()) {
            module = readFromManifest(new File(locationFile, MODULE_MANIFEST_NAME));
        } else {
            try {
                ZipFile zipFile = new ZipFile(locationFile);
                try {
                    ZipEntry entry = zipFile.getEntry(MODULE_MANIFEST_NAME);
                    if (entry == null) {
                        throw new CoreException(
                                String.format("Manifest [%s] not found in [%s]", MODULE_MANIFEST_NAME, locationFile.getName()));
                    }
                    InputStream inputStream = zipFile.getInputStream(entry);
                    module = readFromManifest(inputStream);
                } finally {
                    zipFile.close();
                }
            } catch (IOException e) {
                throw new CoreException(
                        String.format("Failed to read manifest [%s] from [%s]", MODULE_MANIFEST_NAME, locationFile.getName()), e);
            }
        }
        initModule(module, UrlHelper.fileToUrl(locationFile), locationFile);
        return module;
    }

    public ModuleImpl readFromLocation(URL locationUrl) throws CoreException {
        final URL manifestUrl = UrlHelper.locationToManifestUrl(locationUrl);
        if (manifestUrl == null) {
            throw new CoreException("Not a module URL: [" + locationUrl + "]");
        }
        final ModuleImpl module = readFromManifest(manifestUrl, ProxyConfig.NULL);
        initModule(module, locationUrl, UrlHelper.urlToFile(locationUrl));
        return module;
    }

    public ModuleImpl readFromManifest(InputStream inputStream) throws CoreException {
        // Note: inputStream is closed in readFromXML
        return new ModuleManifestParser().parse(inputStream);
    }

    public ModuleImpl readFromManifest(File manifestFile) throws CoreException {
        try {
            final Reader reader = new FileReader(manifestFile);
            // Note: reader is closed in readFromXML
            return new ModuleManifestParser().parse(reader);
        } catch (FileNotFoundException e) {
            throw new CoreException("Module manifest [" + manifestFile + "] not found", e);
        }
    }

    public ModuleImpl readFromManifest(URL manifestUrl, ProxyConfig proxyConfig) throws CoreException {
        try {
            final URLConnection urlConnection = UrlHelper.openConnection(manifestUrl, proxyConfig, "GET");
            final InputStream stream = urlConnection.getInputStream();
            // Note: stream is closed in readFromXML
            return new ModuleManifestParser().parse(stream);
        } catch (IOException e) {
            throw new CoreException("Failed to read module manifest from [" + manifestUrl + "]", e);
        }
    }


    private void initModule(ModuleImpl module, URL locationUrl, File locationFile) {
        module.setLocation(locationUrl);
        module.setImpliciteLibs(NO_LIBS);
        module.setImpliciteNativeLibs(NO_LIBS);
        if (locationFile != null) {
            module.setContentLength(locationFile.length());
            module.setLastModified(locationFile.lastModified());
            if (locationFile.isDirectory()) {
                File implicitLibDir = new File(locationFile, "lib");
                if (implicitLibDir.exists() && implicitLibDir.isDirectory()) {
                    String[] libs = scanImpliciteLibs(implicitLibDir);
                    for (int i = 0; i < libs.length; i++) {
                        libs[i] = "lib/" + libs[i];
                    }
                    module.setImpliciteLibs(libs);
                }
                if (module.isNative()) {
                    String[] nativeLibs = scanImpliciteNativeLibs(locationFile);
                    module.setImpliciteNativeLibs(nativeLibs);
                }
            }
        }
        logger.info(MessageFormat.format("Module [{0}] read from [{1}].",
                module.getSymbolicName(),
                module.getLocation()));
    }


    private static String[] scanImpliciteLibs(File dir) {
        return new DirScanner(dir, true, true).scan(new JarFilenameFilter());
    }

    private static String[] scanImpliciteNativeLibs(File dir) {
        // todo - see FileHelper for similar OS dependent construct --> try to generyfy this in a class 'OS'
        FilenameFilter ff;
        String osNameLC = System.getProperty("os.name", "").toLowerCase();
        if (osNameLC.contains("windows")) {
            ff = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".dll");
                }
            };
        } else if (osNameLC.contains("mac os x")) {
            ff = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jnilib") || name.endsWith(".dylib");
                }
            };
        } else {
            ff = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".so");
                }
            };
        }
        // first search for native libraries in architecture specific directories
        Platform currentPlatform = Platform.getCurrentPlatform();
        if (currentPlatform != null) {
            String platformPath = Platform.getSourcePathPrefix(currentPlatform.getId(), currentPlatform.getBitCount());
            File platformDir = new File(dir, platformPath);
            if (platformDir.exists() && platformDir.isDirectory()) {
                String[] result = new DirScanner(platformDir, true, true).scan(ff);
                if (result.length > 0) {
                    String[] fullResult = new String[result.length];
                    for (int i = 0; i < fullResult.length; i++) {
                        fullResult[i] = platformPath + "/" + result[i];
                    }
                    return fullResult;
                }
            }
        }
        return new DirScanner(dir, true, true).scan(ff);
    }
}
