/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.utils;

import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.utils.module.ModuleInstaller;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating at runtime a jar module
 * for a tool adapter, so that it can be independently deployed.
 *
 * @author Cosmin Cara
 */
public final class ModulePackager {

    private static final Manifest _manifest;
    private static final Attributes.Name ATTR_DESCRIPTION_NAME;
    private static final Attributes.Name ATTR_MODULE_NAME;
    private static final Attributes.Name ATTR_MODULE_TYPE;
    private static final File modulesPath;
    private static final String layerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.1//EN\" \"http://www.netbeans.org/dtds/filesystem-1_1.dtd\">\n" +
            "<filesystem>\n" +
            "    <folder name=\"Actions\">\n" +
            "        <folder name=\"Tools\">\n" +
            "            <file name=\"org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.instance\"/>\n" +
            "            <attr name=\"displayName\" stringvalue=\"#NAME#\"/>\n" +
            "            <attr name=\"instanceCreate\" methodvalue=\"org.openide.awt.Actions.alwaysEnabled\"/>\n" +
            "        </folder>\n" +
            "    </folder>\n" +
            "    <folder name=\"Menu\">\n" +
            "        <folder name=\"Tools\">\n" +
            "            <folder name=\"External Tools\">\n" +
            "                <file name=\"org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.shadow\">\n" +
            "                    <attr name=\"originalFile\" stringvalue=\"Actions/Tools/org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.instance\"/>\n" +
            "                    <attr name=\"position\" intvalue=\"1000\"/>\n" +
            "                </file>\n" +
            "            </folder>\n" +
            "        </folder>\n" +
            "    </folder>\n" +
            "</filesystem>";
    private static final String LAYER_XML_PATH = "org/esa/snap/ui/tooladapter/layer.xml";

    static {
        _manifest = new Manifest();
        Attributes attributes = _manifest.getMainAttributes();
        ATTR_DESCRIPTION_NAME = new Attributes.Name("OpenIDE-Module-Short-Description");
        ATTR_MODULE_NAME = new Attributes.Name("OpenIDE-Module");
        ATTR_MODULE_TYPE = new Attributes.Name("OpenIDE-Module-Type");
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("OpenIDE-Module-Java-Dependencies"), "Java > 1.8");
        attributes.put(new Attributes.Name("OpenIDE-Module-Module-Dependencies"), "org.esa.snap.snap.sta, org.esa.snap.snap.sta.ui");
        attributes.put(new Attributes.Name("OpenIDE-Module-Display-Category"), "SNAP");
        attributes.put(ATTR_MODULE_TYPE, "STA");
        //attributes.put(new Attributes.Name("OpenIDE-Module-Layer"), LAYER_XML_PATH);
        attributes.put(ATTR_DESCRIPTION_NAME, "External tool adapter");

        modulesPath = ToolAdapterIO.getUserAdapterPath();
    }

    /**
     * Packs the files associated with the given tool adapter operator descriptor into
     * a NetBeans module file (nbm)
     *
     * @param descriptor    The tool adapter descriptor
     * @param nbmFile       The target module file
     * @throws IOException
     */
    public static void packModule(ToolAdapterOperatorDescriptor descriptor, File nbmFile) throws IOException {
        StringBuilder xmlBuilder = new StringBuilder();
        byte[] byteBuffer = null;
        try (final ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(nbmFile))) {
            // create Info section
            ZipEntry entry = new ZipEntry("Info/info.xml");
            zipStream.putNextEntry(entry);
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                      .append("<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Autoupdate Module Info 2.5//EN\" \"http://www.netbeans.org/dtds/autoupdate-info-2_5.dtd\">");
            xmlBuilder.append("<module codenamebase=\"")
                      .append(descriptor.getName().toLowerCase())
                      .append("\" distribution=\"")
                      .append(nbmFile.getName())
                      .append("\" downloadsize=\"0\" homepage=\"https://github.com/senbox-org/s2tbx\" needsrestart=\"true\" releasedate=\"")
                      .append(new SimpleDateFormat("yyyy/MM/dd").format(new Date()))
                      .append("\">\n")
                      .append("<manifest AutoUpdate-Essential-Module=\"true\" AutoUpdate-Show-In-Client=\"false\" OpenIDE-Module=\"")
                      .append(descriptor.getName())
                      .append("\" OpenIDE-Module-Display-Category=\"SNAP\" OpenIDE-Module-Implementation-Version=\"2.0.0-")
                      .append(new SimpleDateFormat("yyyyMMdd").format(new Date()))
                      .append("\" OpenIDE-Module-Java-Dependencies=\"Java &gt; 1.8\" OpenIDE-Module-Long-Description=\"&lt;p&gt;")
                      .append(descriptor.getDescription())
                      .append("&lt;/p&gt;\" OpenIDE-Module-Module-Dependencies=\"org.esa.snap.snap.sta &gt; 2.0.0, org.esa.snap.snap.sta.ui &gt; 2.0.0, org.esa.snap.snap.rcp &gt; 2.0.0\" OpenIDE-Module-Name=\"")
                      .append(descriptor.getName())
                      .append("\" OpenIDE-Module-Requires=\"org.openide.modules.ModuleFormat1\" OpenIDE-Module-Short-Description=\"")
                      .append(descriptor.getDescription())
                      .append("\" OpenIDE-Module-Specification-Version=\"2.0.0\"/>\n</module>");
            byteBuffer = xmlBuilder.toString().getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            // create META-INF section
            xmlBuilder.setLength(0);
            entry = new ZipEntry("META-INF/MANIFEST.MF");
            zipStream.putNextEntry(entry);
            xmlBuilder.append("Manifest-Version: 1.0\nCreated-By: 1.8.0_31-b13 (Oracle Corporation)\n");
            byteBuffer = xmlBuilder.toString().getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            String jarName = descriptor.getName().replace(".", "-") + ".jar";

            // create config section
            xmlBuilder.setLength(0);
            entry = new ZipEntry("netbeans/config/Modules/" + descriptor.getName().replace(".", "-") + ".xml");
            zipStream.putNextEntry(entry);
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .append("<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Module Status 1.0//EN\"\n\"http://www.netbeans.org/dtds/module-status-1_0.dtd\">\n")
                    .append("<module name=\"")
                    .append(descriptor.getName())
                    .append("\">\n<param name=\"autoload\">false</param><param name=\"eager\">false</param><param name=\"enabled\">true</param>\n")
                    .append("<param name=\"jar\">modules/")
                    .append(jarName)
                    .append("</param><param name=\"reloadable\">false</param>\n</module>");
            byteBuffer = xmlBuilder.toString().getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();
            // create modules section
            xmlBuilder.setLength(0);
            entry = new ZipEntry("netbeans/modules/ext/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
            entry = new ZipEntry("netbeans/modules/" + jarName);
            zipStream.putNextEntry(entry);
            zipStream.write(packAdapterJar(descriptor));
            zipStream.closeEntry();
            // create update_tracking section
            entry = new ZipEntry("netbeans/update_tracking/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
        }
    }

    /**
     * Unpacks a jar file into the user modules location.
     *
     * @param jarFile   The jar file to be unpacked
     * @param unpackFolder  The destination folder. If null, then the jar name will be used
     * @throws IOException
     */
    public static void unpackAdapterJar(File jarFile, File unpackFolder) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Enumeration enumEntries = jar.entries();
        if (unpackFolder == null) {
            unpackFolder = new File(modulesPath, jarFile.getName().replace(".jar", ""));
        }
        if (!unpackFolder.exists())
            unpackFolder.mkdir();
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            File f = new File(unpackFolder, file.getName());
            if (file.isDirectory()) {
                f.mkdir();
                continue;
            } else {
                f.getParentFile().mkdirs();
            }
            try (InputStream is = jar.getInputStream(file)) {
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    while (is.available() > 0) {
                        fos.write(is.read());
                    }
                    fos.close();
                }
                is.close();
            }
        }
    }

    private static byte[] packAdapterJar(ToolAdapterOperatorDescriptor descriptor) throws IOException {
        _manifest.getMainAttributes().put(ATTR_DESCRIPTION_NAME, descriptor.getAlias());
        _manifest.getMainAttributes().put(ATTR_MODULE_NAME, descriptor.getName());
        File moduleFolder = new File(modulesPath, descriptor.getAlias());
        ByteArrayOutputStream fOut = new ByteArrayOutputStream();
        _manifest.getMainAttributes().put(new Attributes.Name("OpenIDE-Module-Install"), ModuleInstaller.class.getName().replace('.', '/') + ".class");
        try (JarOutputStream jarOut = new JarOutputStream(fOut, _manifest)) {
            File[] files = moduleFolder.listFiles();
            if (files != null) {
                for (File child : files) {
                    try {
                        addFile(child, jarOut);
                    } catch (Exception ignored) {
                    }
                }
                addFile(ModuleInstaller.class, jarOut);
            }
            try {
                String contents = layerXml.replace("#NAME#", descriptor.getLabel());
                JarEntry entry = new JarEntry(LAYER_XML_PATH);
                jarOut.putNextEntry(entry);
                byte[] buffer = contents.getBytes();
                jarOut.write(buffer, 0, buffer.length);
                jarOut.closeEntry();
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            jarOut.close();
        }
        return fOut.toByteArray();
    }

    /**
     * Adds a file to the target jar stream.
     *
     * @param source    The file to be added
     * @param target    The target jar stream
     * @throws IOException
     */
    private static void addFile(File source, JarOutputStream target) throws IOException {
        String entryName = source.getPath().replace(modulesPath.getAbsolutePath(), "").replace("\\", "/").substring(1);
        entryName = entryName.substring(entryName.indexOf("/") + 1);
        if (!entryName.toLowerCase().endsWith("manifest.mf")) {
            if (source.isDirectory()) {
                if (!entryName.isEmpty()) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    JarEntry entry = new JarEntry(entryName);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                File[] files = source.listFiles();
                if (files != null) {
                    for (File nestedFile : files) {
                        addFile(nestedFile, target);
                    }
                }
                return;
            }
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            writeBytes(source, target);
            target.closeEntry();
        }
    }

    /**
     * Adds a compiled class file to the target jar stream.
     *
     * @param fromClass     The class to be added
     * @param target        The target jar stream
     * @throws IOException
     */
    private static void addFile(Class fromClass, JarOutputStream target) throws IOException {
        String classEntry = fromClass.getName().replace('.', '/') + ".class";
        URL classURL = fromClass.getClassLoader().getResource(classEntry);
        if (classURL != null) {
            JarEntry entry = new JarEntry(classEntry);
            target.putNextEntry(entry);
            if (!classURL.toString().contains("!")) {
                String fileName = classURL.getFile();
                writeBytes(fileName, target);
            } else {
                try (InputStream stream = fromClass.getClassLoader().getResourceAsStream(classEntry)) {
                    writeBytes(stream, target);
                }
            }
            target.closeEntry();
        }
    }

    private static void writeBytes(String fileName, JarOutputStream target) throws IOException {
        writeBytes(new File(fileName), target);
    }

    private static void writeBytes(File file, JarOutputStream target) throws IOException {
        try (FileInputStream fileStream = new FileInputStream(file)) {
            try (BufferedInputStream inputStream = new BufferedInputStream(fileStream)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = inputStream.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    target.write(buffer, 0, count);
                }
            }
        }
    }

    private static void writeBytes(InputStream stream, JarOutputStream target) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int count = stream.read(buffer);
            if (count == -1) {
                break;
            }
            target.write(buffer, 0, count);
        }
    }

}
