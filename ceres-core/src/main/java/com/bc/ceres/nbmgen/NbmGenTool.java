package com.bc.ceres.nbmgen;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.bc.ceres.nbmgen.CeresModuleProject.*;

/**
 * Usage:
 * <pre>
 *    NbmGenTool &lt;project-dir&gt; &lt;cluster&gt; &lt;dry-run&gt;
 * </pre>
 * <p>
 * For example:
 *
 * <pre>
 *    NbmGenTool . s1tbx true
 * </pre>
 * <p>Scans a ${project-dir} for Ceres modules ({@code pom.xml} + {@code src/main/resources/module.xml})
 * and converts each module into a NetBeans module.
 *
 * <p><b>WARNING: This tool will overwrite all of your {@code pom.xml} files the given ${project-dir}.
 * It will also create or overwrite {@code src/main/nmb/manifest.mf} files. Make sure the originals
 * of these files are committed/pushed to git before applying this tool.</b>
 *
 *
 * @author Norman
 */
public class NbmGenTool implements CeresModuleProject.Processor {

    File projectDir;
    String cluster;
    boolean dryRun;

    public NbmGenTool(File projectDir, String cluster, boolean dryRun) {
        this.projectDir = projectDir;
        this.cluster = cluster;
        this.dryRun = dryRun;
    }

    public static void main(String[] args) throws JDOMException, IOException {

        File projectDir = new File(args[0]);
        String cluster = args[1];
        boolean dryRun = args[2].equals("true");

        NbmGenTool processor = new NbmGenTool(projectDir, cluster, dryRun);

        CeresModuleProject.processParentDir(projectDir, processor);

        if (dryRun) {
            warnModuleDetail("dry run: file system not modified");
        }
    }

    @Override
    public void process(CeresModuleProject project) throws JDOMException, IOException {

        System.out.println("Project [" + project.projectDir.getName() + "]:");

        File originalPomFile = getFile(project.projectDir, CeresModuleProject.ORIGINAL_POM_XML);
        File pomFile = getFile(project.projectDir, CeresModuleProject.POM_XML);
        File manifestBaseFile = getFile(project.projectDir, "src", "main", "nbm", "manifest.mf");

        Element projectElement = project.pomDocument.getRootElement();
        Namespace ns = projectElement.getNamespace();

        Element moduleElement = project.moduleDocument.getRootElement();
        String moduleName = moduleElement.getChildTextTrim("name");
        String moduleDescription = moduleElement.getChildTextNormalize("description");
        String modulePackaging = moduleElement.getChildTextTrim("packaging");
        String moduleNative = moduleElement.getChildTextTrim("native");
        String moduleActivator = moduleElement.getChildTextTrim("activator");
        String moduleChangelog = moduleElement.getChildTextTrim("changelog");
        String moduleFunding = moduleElement.getChildTextTrim("funding");
        String moduleVendor = moduleElement.getChildTextTrim("vendor");
        String moduleContactAddress = moduleElement.getChildTextTrim("contactAddress");
        String moduleCopyright = moduleElement.getChildTextTrim("copyright");
        String moduleLicenseUrl = moduleElement.getChildTextTrim("licenseUrl");
        // Not used anymore:
        //String moduleUrl = moduleElement.getChildTextTrim("url");
        //String moduleAboutUrl = moduleElement.getChildTextTrim("aboutUrl");

        if (moduleName != null) {
            Element nameElement = getOrAddElement(projectElement, "name", ns);
            nameElement.setText(moduleName);
        }
        if (moduleDescription != null) {
            int nameIndex = projectElement.indexOf(projectElement.getChild("name"));
            Element descriptionElement = getOrAddElement(projectElement, "description", nameIndex + 1, ns);
            descriptionElement.setText(moduleDescription);
        }
        Element descriptionElement = getOrAddElement(projectElement, "packaging", ns);
        descriptionElement.setText("nbm");

        Element urlElement = getOrAddElement(projectElement, "url", ns);
        urlElement.setText("https://sentinel.esa.int/web/sentinel/toolboxes");

        Element buildElement = getOrAddElement(projectElement, "build", ns);
        Element pluginsElement = getOrAddElement(buildElement, "plugins", ns);

        Map<String, String> nbmConfiguration = new LinkedHashMap<>();
        // moduleType is actually a constant which can be put it into the <pluginManagement-element of the parent
        nbmConfiguration.put("moduleType", "normal");
        // licenseName/File should also be constant
        nbmConfiguration.put("licenseName", "GPL 3");
        nbmConfiguration.put("licenseFile", "../LICENSE.html");

        nbmConfiguration.put("cluster", cluster);
        nbmConfiguration.put("defaultCluster", cluster);
        nbmConfiguration.put("publicPackages", "");
        nbmConfiguration.put("requiresRestart", "true");

        addPluginElement(pluginsElement, "org.codehaus.mojo", "nbm-maven-plugin", nbmConfiguration, ns);

        Map<String, String> jarConfiguration = new LinkedHashMap<>();
        jarConfiguration.put("useDefaultManifestFile", "true");

        addPluginElement(pluginsElement, "org.apache.maven.plugins", "maven-jar-plugin", jarConfiguration, ns);


        StringBuilder longDescription = new StringBuilder();

        longDescription
                .append(moduleDescription != null ? "<p>" + moduleDescription + "" : "")
                .append(descriptionEntry("Funding", moduleFunding))
                .append(descriptionEntry("Vendor", moduleVendor))
                .append(descriptionEntry("Contact address", moduleContactAddress))
                .append(descriptionEntry("Copyright", moduleCopyright))
                .append(descriptionEntry("Vendor", moduleVendor))
                .append(descriptionEntry("License", moduleLicenseUrl))
                .append(descriptionEntry("Changelog", moduleChangelog));

        Map<String, String> manifestContent = new LinkedHashMap<>();
        manifestContent.put("Manifest-Version", "1.0");
        manifestContent.put("AutoUpdate-Show-In-Client", "false");
        manifestContent.put("AutoUpdate-Essential-Module", "true");
        manifestContent.put("OpenIDE-Module-Java-Dependencies", "Java > 1.8");
        manifestContent.put("OpenIDE-Module-Display-Category", "SNAP");
        if (longDescription.length() > 0) {
            manifestContent.put("OpenIDE-Module-Long-Description", longDescription.toString());
        }
        if (moduleActivator != null) {
            warnModuleDetail("Activator may be reimplemented for NB: " + moduleActivator + " (--> " +
                             "consider using @OnStart, @OnStop, @OnShowing, or a ModuleInstall)");
            manifestContent.put("OpenIDE-Module-Install", moduleActivator);
        }
        if (modulePackaging != null && !"jar".equals(modulePackaging)) {
            warnModuleDetail("Unsupported module packaging: " + modulePackaging + " (--> " +
                             "provide a ModuleInstall that does the job on install/uninstall)");
        }
        if (moduleNative != null && "true".equals(moduleNative)) {
            warnModuleDetail("Module contains native code: no auto-conversion possible (--> " +
                             "follow NB instructions see http://bits.netbeans.org/dev/javadoc/org-openide-modules/org/openide/modules/doc-files/api.html#how-layer");
        }

        if (!originalPomFile.exists()) {
            if (!dryRun) {
                Files.copy(project.pomFile.toPath(), originalPomFile.toPath());
            }
            infoModuleDetail("Copied " + project.pomFile + " to " + originalPomFile);
        }

        if (!dryRun) {
            writeXml(pomFile, project.pomDocument);
        }
        if (pomFile.equals(project.pomFile)) {
            infoModuleDetail("Updated " + pomFile);
        } else {
            infoModuleDetail("Converted " + project.pomFile + " to " + pomFile);
        }

        //noinspection ResultOfMethodCallIgnored
        if (!dryRun) {
            manifestBaseFile.getParentFile().mkdirs();
            writeManifest(manifestBaseFile, manifestContent);
        }
        infoModuleDetail("Written " + manifestBaseFile);
    }

    private String descriptionEntry(String name, String value) {
        return value != null ? String.format("<p><b>%s:</b> %s", name, value) : "";
    }

    private static void infoModuleDetail(String msg) {
        System.out.println("- " + msg);
    }

    private static void warnModuleDetail(String msg) {
        System.out.println("- WARNING: " + msg);
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

    private static Element getOrAddElement(Element parent, String name, int index, Namespace ns) {
        Element child = parent.getChild(name, ns);
        if (child == null) {
            child = new Element(name, ns);
            if (index >= 0) {
                parent.addContent(index, child);
            }else {
                parent.addContent(child);

            }
        }
        return child;
    }

    private static Element getOrAddElement(Element parent, String name, Namespace ns) {
        return getOrAddElement(parent, name, -1, ns);
    }

    private static void writeXml(File file, Document document) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setIndent("    ");
        xmlOutput.setFormat(format);
        xmlOutput.output(document, new FileWriter(file));
    }


}
