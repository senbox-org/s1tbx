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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <pre>
 * Usage: NbmGenMain &lt;project-dir&gt; &lt;cluster&gt;
 * </pre>
 * </p>
 * Scans a ${project-dir} for Ceres modules ({@code pom.xml} + {@code src/main/resources/module.xml})
 * and converts each module to a NetBeans module.
 * <p/>
 * <b>WARNING: This tool will overwrite all of your {@code pom.xml} files the given ${project-dir}.
 * It will also create or overwrite {@code src/main/nmb/manifest.mf} files. Make sure the originals
 * of these files are committed/pushed to git before applying this tool.</b>
 *
 * @author Norman
 */
public class NbmGenMain {

    public static final String ORIGINAL_POM_XML = "pom-original.xml";
    public static final String POM_XML = "pom.xml";

    public static void main(String[] args) throws JDOMException, IOException {

        String projectDirPath = args[0];
        String cluster = args[1];

        File[] moduleDirs = new File(projectDirPath).listFiles(file -> file.isDirectory() && getFile(file, "pom.xml").exists() && getFile(file, "src", "main", "resources", "module.xml").exists());
        if (moduleDirs == null) {
            System.err.print("No modules found in " + projectDirPath);
            System.exit(1);
        }

        for (File moduleDir : moduleDirs) {
            System.out.println("Module [" + moduleDir.getName() + "]:");

            File originalPomFile = getFile(moduleDir, ORIGINAL_POM_XML);
            File sourcePomFile;
            if (!originalPomFile.exists()) {
                sourcePomFile = getFile(moduleDir, POM_XML);
            } else {
                sourcePomFile = originalPomFile;
            }

            Document pomDocument = readXml(sourcePomFile);

            File moduleFile = getFile(moduleDir, "src", "main", "resources", "module.xml");
            Document moduleDocument = readXml(moduleFile);

            updatePomAndWriteManifest(moduleDir, sourcePomFile, originalPomFile, pomDocument, moduleDocument, cluster);
        }
    }

    private static void updatePomAndWriteManifest(File moduleDir, File sourcePomFile, File originalPomFile, Document pomDocument, Document moduleDocument, String cluster) throws IOException {
        File pomFile = getFile(moduleDir, "pom.xml");
        File manifestBaseFile = getFile(moduleDir, "src", "main", "nbm", "manifest.mf");

        Element projectElement = pomDocument.getRootElement();
        Namespace ns = projectElement.getNamespace();

        Element buildElement = getOrAddElement(projectElement, "build", ns);
        Element pluginsElement = getOrAddElement(buildElement, "plugins", ns);

        Map<String, String> nbmConfiguration = new LinkedHashMap<>();
        nbmConfiguration.put("moduleType", "normal");
        nbmConfiguration.put("cluster", cluster);
        nbmConfiguration.put("defaultCluster", cluster);
        nbmConfiguration.put("publicPackages", "");
        nbmConfiguration.put("requiresRestart", "true");
        addPluginElement(pluginsElement,
                         "org.codehaus.mojo", "nbm-maven-plugin", nbmConfiguration, ns);

        Map<String, String> jarConfiguration = new LinkedHashMap<>();
        jarConfiguration.put("useDefaultManifestFile", "true");
        addPluginElement(pluginsElement,
                         "org.apache.maven.plugins", "maven-jar-plugin", jarConfiguration, ns);

        Element moduleElement = moduleDocument.getRootElement();
        String moduleName = moduleElement.getChildTextTrim("name");
        if (moduleName != null) {
            Element nameElement = getOrAddElement(projectElement, "name", ns);
            nameElement.setText(moduleName);
        }
        String moduleDescription = moduleElement.getChildTextNormalize("description");
        if (moduleDescription != null) {
            Element descriptionElement = getOrAddElement(projectElement, "description", ns);
            descriptionElement.setText(moduleDescription);
        }

        String modulePackaging = moduleElement.getChildTextTrim("packaging");
        String moduleNative = moduleElement.getChildTextTrim("native");
        String moduleActivator = moduleElement.getChildTextTrim("activator");

        // todo - deal with the following content, e.g. add as HTML to OpenIDE-Module-Long-Description
        String moduleChangelog = moduleElement.getChildTextTrim("changelog");
        String moduleFunding = moduleElement.getChildTextTrim("funding");
        String moduleVendor = moduleElement.getChildTextTrim("vendor");
        String moduleContactAddress = moduleElement.getChildTextTrim("contactAddress");
        String moduleCopyright = moduleElement.getChildTextTrim("copyright");
        String moduleUrl = moduleElement.getChildTextTrim("url");
        String moduleAboutUrl = moduleElement.getChildTextTrim("aboutUrl");
        String moduleLicenseUrl = moduleElement.getChildTextTrim("licenseUrl");


        Map<String, String> manifestContent = new LinkedHashMap<>();
        manifestContent.put("Manifest-Version", "1.0");
        manifestContent.put("AutoUpdate-Show-In-Client", "false");
        manifestContent.put("AutoUpdate-Essential-Module", "true");
        manifestContent.put("OpenIDE-Module-Java-Dependencies", "Java > 1.8");
        manifestContent.put("OpenIDE-Module-Display-Category", "SNAP");
        if (moduleDescription != null) {
            manifestContent.put("OpenIDE-Module-Long-Description", moduleDescription);
        }
        if (moduleActivator != null) {
            warnModuleDetail("Activator may be reimplemented for NB: " + moduleActivator + " (consider using @OnStart, @OnStop, @OnShowing, or a ModuleInstall)");
            manifestContent.put("OpenIDE-Module-Install", moduleActivator);
        }
        if (modulePackaging != null && !"jar".equals( modulePackaging)) {
            warnModuleDetail("Unsupported module packaging: " + modulePackaging + " (provide a ModuleInstall that does the job on install/uninstall)");
        }

        if (!originalPomFile.exists()) {
            Files.copy(sourcePomFile.toPath(), originalPomFile.toPath());
            infoModuleDetail("Copied " + sourcePomFile + " to " + originalPomFile);
        }

        writeXml(pomFile, pomDocument);
        if (pomFile.equals(sourcePomFile)) {
            infoModuleDetail("Updated " + pomFile);
        } else {
            infoModuleDetail("Converted " + sourcePomFile + " to " + pomFile);
        }

        //noinspection ResultOfMethodCallIgnored
        manifestBaseFile.getParentFile().mkdirs();
        writeManifest(manifestBaseFile, manifestContent);
        infoModuleDetail("Written " + manifestBaseFile);
    }

    private static void infoModuleDetail(String msg) {
        System.out.println("- " + msg);
    }

    private static void warnModuleDetail(String msg) {
        System.err.println("- WARNING: " + msg);
    }

    private static void writeManifest(File manifestBaseFile, Map<String, String> nbmConfiguration) throws IOException {
        try (ManifestWriter manifestWriter = new ManifestWriter(new FileWriter(manifestBaseFile))) {
            for (Map.Entry<String, String> entry : nbmConfiguration.entrySet()) {
                manifestWriter.write(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void addPluginElement(Element pluginsElement, String groupId, String artifactId, Map<String, String> configuration, Namespace ns) {
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

    private static void writeXml(File file, Document document) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setIndent("    ");
        xmlOutput.setFormat(format);
        xmlOutput.output(document, new FileWriter(file));
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
