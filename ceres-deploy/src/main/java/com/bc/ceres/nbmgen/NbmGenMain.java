package com.bc.ceres.nbmgen;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Norman
 */
public class NbmGenMain {

    public static final String POM_OLD_XML = "pom-old.xml";
    public static final String POM_XML = "pom.xml";

    public static void main(String[] args) throws JDOMException, IOException {

        String projectDirPath = args.length == 1 ? args[0] : ".";
        File[] moduleDirs = new File(projectDirPath).listFiles(file -> file.isDirectory() && getFile(file, "pom.xml").exists() && getFile(file, "src", "main", "resources", "module.xml").exists());
        if (moduleDirs == null) {
            System.err.print("No modules found in " + projectDirPath);
            System.exit(1);
        }

        for (File moduleDir : moduleDirs) {
            File oldPomFile = getFile(moduleDir, POM_OLD_XML);
            File pomFile;
            if (!oldPomFile.exists()) {
                pomFile = getFile(moduleDir, "pom.xml");
            } else {
                pomFile = oldPomFile;
            }

            Document pomDocument = readXml(pomFile);

            File moduleFile = getFile(moduleDir, "src", "main", "resources", "module.xml");
            Document moduleDocument = readXml(moduleFile);

            updatePomAndWriteManifest(moduleDir, pomFile, oldPomFile, pomDocument, moduleDocument);
        }
    }

    private static void updatePomAndWriteManifest(File moduleDir, File pomFile, File oldPomFile, Document pomDocument, Document moduleDocument) throws IOException {
        Element projectElement = pomDocument.getRootElement();
        Namespace ns = projectElement.getNamespace();

        System.out.println("name: " + projectElement.getName());
        System.out.println("groupId: " + projectElement.getChildText("groupId", ns));
        System.out.println("artifactId: " + projectElement.getChildText("artifactId", ns));
        System.out.println("version: " + projectElement.getChildText("version", ns));
        System.out.println("name: " + projectElement.getChildText("name", ns));
        System.out.println("description: " + projectElement.getChildText("description", ns));

        Element buildElement = getOrAddElement(projectElement, "build", ns);
        Element pluginsElement = getOrAddElement(buildElement, "plugins", ns);

        HashMap<String, String> nbmConfiguration = new HashMap<>();
        addPluginElement(pluginsElement,
                         "org.codehaus.mojo", "nbm-maven-plugin", nbmConfiguration, ns);

        HashMap<String, String> jarConfiguration = new HashMap<>();
        jarConfiguration.put("useDefaultManifestFile", "true");
        addPluginElement(pluginsElement,
                         "org.apache.maven.plugins", "maven-jar-plugin", jarConfiguration, ns);

        Element moduleElement = moduleDocument.getRootElement();
        String moduleName = moduleElement.getChildTextTrim("name");
        if (moduleName != null) {
            Element nameElement = getOrAddElement(projectElement, "name", ns);
            nameElement.setText(moduleName);
        }
        String moduleDescription = moduleElement.getChildTextTrim("description");
        if (moduleDescription != null) {
            Element descriptionElement = getOrAddElement(projectElement, "description", ns);
            descriptionElement.setText(moduleDescription);
        }

        // todo - deal with the following content.
        String moduleChangelog = moduleElement.getChildTextTrim("changelog");
        String moduleFunding = moduleElement.getChildTextTrim("funding");
        String moduleVendor = moduleElement.getChildTextTrim("vendor");
        String moduleContactAddress = moduleElement.getChildTextTrim("contactAddress");
        String moduleCopyright = moduleElement.getChildTextTrim("copyright");
        String moduleUrl = moduleElement.getChildTextTrim("url");

        HashMap<String, String> manifestContent = new LinkedHashMap<>();
        manifestContent.put("Manifest-Version", "1.0");
        manifestContent.put("AutoUpdate-Show-In-Client", "false");
        manifestContent.put("AutoUpdate-Essential-Module", "true");
        manifestContent.put("OpenIDE-Module-Display-Category", "SNAP");
        if (moduleDescription != null) {
            manifestContent.put("OpenIDE-Module-Long-Description", moduleDescription);
        }

        if (!oldPomFile.exists()) {
            Files.copy(pomFile.toPath(), oldPomFile.toPath());
        }

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(pomDocument, new FileWriter(getFile(moduleDir, "pom-new.xml")));

        File manifestBaseFile = getFile(moduleDir, "src", "main", "nbm", "manifest.mf");
        //noinspection ResultOfMethodCallIgnored
        manifestBaseFile.getParentFile().mkdirs();

        writeManifest(manifestBaseFile, manifestContent);
    }

    private static void writeManifest(File manifestBaseFile, HashMap<String, String> nbmConfiguration) throws IOException {
        try (ManifestWriter manifestWriter = new ManifestWriter(new FileWriter(manifestBaseFile))) {
            for (Map.Entry<String, String> entry : nbmConfiguration.entrySet()) {
                manifestWriter.write(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void addPluginElement(Element pluginsElement, String groupId, String artifactId, HashMap<String, String> configuration, Namespace ns) {
        Element pluginElement = new Element("plugin", ns);
        Element groupIdElement = new Element("groupId", ns);
        groupIdElement.setText(groupId);
        pluginElement.addContent(groupIdElement);
        Element artifactIdElement = new Element("artifactId", ns);
        artifactIdElement.setText(artifactId);
        pluginElement.addContent(artifactIdElement);
        Element configurationElement = new Element("configuration", ns);
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            Element keyElement = new Element(entry.getKey(), ns);
            keyElement.setText(entry.getValue());
            configurationElement.addContent(keyElement);
        }
        pluginElement.addContent(configurationElement);
        pluginsElement.addContent(pluginElement);
    }

    private static Element getOrAddElement(Element parent, String name, Namespace ns) {
        Element child = parent.getChild(name, ns);
        if (child == null) {
            child = new Element(name, ns);
            parent.addContent(child);
        }
        return child;
    }

    private static Document readXml(File file) throws JDOMException, IOException {
        return new SAXBuilder().build(file);
    }

    private static File getFile(File dir, String... names) {
        File file = dir;
        for (String name : names) {
            file = new File(file, name);
        }
        return file;
    }

    private static Element getChild(Element parent, String... names) {
        Element child = parent;
        for (String name : names) {
            child = child.getChild(name, parent.getNamespace());
            if (child == null) {
                return null;
            }
        }
        return child;
    }

    private static class ManifestWriter extends FilterWriter {
        private int col;

        ManifestWriter(Writer out) {
            super(out);
        }

        public void write(String key, String value) throws IOException {
            write(key);
            write(':');
            write(' ');
            write(value);
            write('\n');
        }

        @Override
        public void write(int c) throws IOException {
            if (col == 70 && c != '\n') {
                super.write('\n');
                super.write(' ');
                col = 1;
            }
            super.write(c);
            if (c == '\n') {
                col = 0;
            } else {
                col++;
            }
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            int n = Math.min(off + len, cbuf.length);
            for (int i = off; i < n; i++) {
                write(cbuf[i]);
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            int n = Math.min(off + len, str.length());
            for (int i = off; i < n; i++) {
                write(str.charAt(i));
            }
        }
    }
}
