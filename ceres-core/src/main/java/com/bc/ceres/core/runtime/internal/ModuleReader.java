package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import static com.bc.ceres.core.runtime.Constants.MODULE_MANIFEST_NAME;
import com.bc.ceres.core.runtime.ProxyConfig;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        initModule(module, FileHelper.fileToUrl(locationFile), locationFile);
        return module;
    }

    public ModuleImpl readFromLocation(URL locationUrl) throws CoreException {
        final URL manifestUrl = FileHelper.locationToManifestUrl(locationUrl);
        if (manifestUrl == null) {
            throw new CoreException("Not a module URL: [" + locationUrl + "]");
        }
        final ModuleImpl module = readFromManifest(manifestUrl, ProxyConfig.NULL);
        initModule(module, locationUrl, FileHelper.urlToFile(locationUrl));
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
                String[] libs = scanImpliciteLibs(locationFile);
                module.setImpliciteLibs(libs);
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
        if (osNameLC.indexOf("windows") >= 0) {
            ff = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".dll");
                }
            };
        } else if (osNameLC.indexOf("mac os x") >= 0) {
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
        return new DirScanner(dir, true, true).scan(ff);
    }
}
