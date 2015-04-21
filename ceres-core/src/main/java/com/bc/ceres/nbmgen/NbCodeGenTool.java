package com.bc.ceres.nbmgen;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Usage:
 * <pre>
 *    NbmGenMain &lt;project-dir&gt; &lt;dry-run&gt; [&lt;output-dir&gt;]
 * </pre>
 * <p>
 * For NbCodeGenMain:
 * <pre>
 *    NbCodeGenMain . true
 * </pre>
 * <p>Scans a ${project-dir} for Ceres modules ({@code pom.xml} + {@code src/main/resources/module.xml})
 * and generates NB Java code stubs for all Ceres extensions.
 *
 * @author Norman
 */
public class NbCodeGenTool implements CeresModuleProject.Processor {

    interface Converter {
        void convert(CeresModuleProject project, String point, Element extensionElement) throws IOException;
    }

    final File projectDir;
    final boolean dryRun;
    final File outputDir;

    VelocityEngine velocityEngine;
    HashMap<String, Converter> converters;

    public NbCodeGenTool(File projectDir, boolean dryRun, File outputDir) {
        this.projectDir = projectDir;
        this.dryRun = dryRun;
        this.outputDir = outputDir;
        velocityEngine = createVelocityEngine();
        converters = new HashMap<>();
        initConverters();
    }

    private void initConverters() {
        Converter NULL = (project, point, extensionElement) -> warnNotImplemented(point);

        converters.put("snap-ui:actions", new ActionConverter());
        converters.put("snap-ceres-core:applications", NULL);
        converters.put("snap-ceres-core:serviceProviders", NULL);
        converters.put("snap-ceres-core:adapters", NULL);
        converters.put("snap-core:rgbProfiles", NULL);
        converters.put("snap-ui:applicationDescriptors", NULL);
        converters.put("snap-ui:helpSets", NULL);
        converters.put("snap-ui:actionGroups", NULL);
        converters.put("snap-ui:toolViews", NULL);
        converters.put("snap-ui:layerSources", NULL);
        converters.put("snap-ui:layerEditors", NULL);
        converters.put("snap-graph-builder:OperatorUIs", NULL);
    }

    private String getActionBaseName(String name) {
        String simpleName;
        int i = name.lastIndexOf('.');
        if (i > 0) {
            simpleName = name.substring(i + 1);
        } else {
            simpleName = name;
        }
        if (simpleName.endsWith("Action")) {
            return simpleName.substring(0, simpleName.length() - "Action".length());
        }
        if (simpleName.endsWith("ActionGroup")) {
            return simpleName.substring(0, simpleName.length() - "ActionGroup".length());
        }
        return simpleName;
    }


    public static void main(String[] args) throws JDOMException, IOException {

        File projectDir = new File(args[0]);
        boolean dryRun = args[1].equals("true");
        File outputDir = args.length > 2 ? new File(args[2]) : projectDir;

        NbCodeGenTool processor = new NbCodeGenTool(projectDir, dryRun, outputDir);

        CeresModuleProject.processParentDir(projectDir, processor);

        if (dryRun) {
            warnModuleDetail("dry run: file system not modified");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(CeresModuleProject project) throws JDOMException, IOException {

        System.out.println("Project [" + project.projectDir.getName() + "]:");

        Element moduleElement = project.moduleDocument.getRootElement();

        List<Element> extensionPointElements = (List<Element>) moduleElement.getChildren("extensionPoint");
        for (Element extensionPointElement : extensionPointElements) {
            String id = extensionPointElement.getAttributeValue("id");
            infoModuleDetail("Found extensionPoint.id = " + id);
        }

        List<Element> extensionElements = (List<Element>) moduleElement.getChildren("extension");
        for (Element extensionElement : extensionElements) {
            String point = extensionElement.getAttributeValue("point");
            infoModuleDetail("Found extension.point = " + point);

            List<Element> extensionConfigs = (List<Element>) extensionElement.getChildren();

            for (Element extensionConfig : extensionConfigs) {
                Converter converter = converters.get(point);
                if (converter != null) {
                    converter.convert(project, point, extensionConfig);
                } else {
                    warnModuleDetail("No converter for extension point: " + point);
                }
            }
        }
    }

    private void evaluate(VelocityContext velocityContext, String resourceName, Writer writer) throws IOException {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resourceName))) {
            evaluate(velocityContext, reader, writer);
        }
    }

    private void evaluate(VelocityContext velocityContext, Reader reader, Writer writer) {
        velocityEngine.evaluate(velocityContext, writer, getClass().getSimpleName(), reader);
    }

    private void warnNotImplemented(String point) {
        warnModuleDetail("converter not implemented for extension point: " + point);
    }

    private static void infoModuleDetail(String msg) {
        System.out.println("- " + msg);
    }

    private static void warnModuleDetail(String msg) {
        System.out.println("- WARNING: " + msg);
    }

    private static VelocityEngine createVelocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize velocity engine", e);
        }
        return velocityEngine;
    }

    private class ActionConverter implements Converter {

        Map<String, Integer> classNameBases = new HashMap<>();

        @Override
        public void convert(CeresModuleProject project, String point, Element extensionElement) throws IOException {

            Map<String, String> parentToPath = new HashMap<>();
            parentToPath.put("file", "Menu/File");
            parentToPath.put("importVectorData", "Menu/File/Import Vector Data");
            parentToPath.put("importRasterData", "Menu/File/Import Raster Data");
            parentToPath.put("importSAR", "Menu/File/Import Raster Data/SAR Data");
            parentToPath.put("importMultispectral", "Menu/File/Import Raster Data/Multispectral Data");
            parentToPath.put("importFileFormats", "Menu/File/Import Raster Data/Generic Formats");
            parentToPath.put("exportOther", "Menu/File/Other Exports");
            parentToPath.put("edit", "Menu/Edit");
            parentToPath.put("view", "Menu/View");
            parentToPath.put("tools", "Toolbars/Tools");  // ???
            parentToPath.put("layoutToolViews", "Menu/View/Tool Window Layout");  // ???
            parentToPath.put("toolBars", "Menu/Tools/Toolbars");  // ???
            parentToPath.put("Graphs", "Menu/Graphs");  // ???
            parentToPath.put("help", "Menu/Help");  // ???
            parentToPath.put("processing", "Menu/Processing");  // ???
            parentToPath.put("processing.preProcessing", "Menu/Processing/Pre-Processing");  // ???
            parentToPath.put("processing.thematicLand", "Menu/Processing/Thematic Land Processing");  // ???
            parentToPath.put("processing.thematicWater", "Menu/Processing/Thematic Water Processing");  // ???
            parentToPath.put("processing.imageAnalysis", "Menu/Processing/Image Analysis");  // ???
            parentToPath.put("processing.geomOperations", "Menu/Processing/Geometric Operations");  // ???

            String id = extensionElement.getChildTextTrim("id");
            String parent = extensionElement.getChildTextTrim("parent");
            String placeAfter = extensionElement.getChildTextTrim("placeAfter");
            String placeBefore = extensionElement.getChildTextTrim("placeBefore");
            String separatorAfter = extensionElement.getChildTextTrim("separatorAfter");
            String separatorBefore = extensionElement.getChildTextTrim("separatorBefore");
            String actionClassName = extensionElement.getChildTextTrim("class");
            String interactor = extensionElement.getChildTextTrim("interactor");
            String interactorListener = extensionElement.getChildTextTrim("interactorListener");
            String text = extensionElement.getChildTextTrim("text");
            String shortDescr = extensionElement.getChildTextTrim("shortDescr");
            String longDescr = extensionElement.getChildTextTrim("longDescr");
            String helpId = extensionElement.getChildTextTrim("helpId");
            String accelerator = extensionElement.getChildTextTrim("accelerator");
            String mnemonic = extensionElement.getChildTextTrim("mnemonic");
            String context = extensionElement.getChildTextTrim("context");
            String popuptext = extensionElement.getChildTextTrim("popuptext");
            String toggle = extensionElement.getChildTextTrim("toggle"); // true/false
            String selected = extensionElement.getChildTextTrim("selected"); // true/false
            String smallIcon = extensionElement.getChildTextTrim("smallIcon");
            String largeIcon = extensionElement.getChildTextTrim("largeIcon");
            String useAllFileFilter = extensionElement.getChildTextTrim("useAllFileFilter");
            String formatName = extensionElement.getChildTextTrim("formatName");
            String sortChildren = extensionElement.getChildTextTrim("sortChildren");
            String operatorName = extensionElement.getChildTextTrim("operatorName");
            String dialogTitle = extensionElement.getChildTextTrim("dialogTitle");
            String targetProductNameSuffix = extensionElement.getChildTextTrim("targetProductNameSuffix");

            if ("org.esa.snap.visat.actions.ActionGroup".equals(actionClassName)) {
                warnModuleDetail("ActionGroup not converted: id = " + id);
                return;
            }

            String classNameBase = NbCodeGenTool.this.getActionBaseName(actionClassName);
            String path = parent != null ? parentToPath.get(parent) : "Menu/Extras";
            String packageName = "org.esa.snap.rcp.action";
            String category = "SNAP"; // todo
            String baseClassName = "AbstractAction";
            int position = 100;

            switch (actionClassName) {
                case "org.esa.snap.visat.actions.ProductImportAction":
                    packageName += ".file.pimp";
                    classNameBase = "Import_" + formatName.replace('-', '_').replace(' ', '_').replace('.', '_').replace('/', '_') + "_";
                    baseClassName = packageName + ".ProductImportAction";
                    // check - do something with formatName
                    // check - do something with useAllFileFilter
                    break;
                case "org.esa.snap.visat.actions.ProductExportAction":
                    packageName += ".file.pexp";
                    classNameBase = "Export_" + formatName.replace('-', '_').replace(' ', '_').replace('.', '_').replace('/', '_') + "_";
                    baseClassName = packageName + ".ProductExportAction";
                    // check - do something with formatName
                    // check - do something with useAllFileFilter
                    break;
                case "org.esa.snap.visat.actions.DefaultOperatorAction":
                    packageName += ".op";
                    classNameBase = "Invoke_" + operatorName + "_";
                    baseClassName = packageName + ".DefaultOperatorAction";
                    break;
                case "org.esa.snap.visat.actions.ShowToolBarAction":
                    packageName += ".view";
                    classNameBase = Character.toUpperCase(id.charAt(0)) + id.substring(1);
                    baseClassName = packageName + ".ShowToolBarAction";
                    break;
                case "org.esa.snap.visat.actions.ToolAction":
                    packageName += ".tool";
                    classNameBase = Character.toUpperCase(id.charAt(0)) + id.substring(1);
                    baseClassName = packageName + ".ToolAction";
                    break;
                case "org.esa.snap.visat.actions.PlacemarkToolAction":
                    packageName += ".tool";
                    classNameBase = Character.toUpperCase(id.charAt(0)) + id.substring(1);
                    baseClassName = packageName + ".PlacemarkToolAction";
                    break;
            }

            Integer count = classNameBases.get(classNameBase);
            if (count != null) {
                count = count + 1;
                classNameBases.put(classNameBase, count);
                String classNameBaseOld = classNameBase;
                if (count > 1) {
                    classNameBase = classNameBase.endsWith("_") ? classNameBase + count + "_" : classNameBase + count;
                }
                String msg = String.format("Class base name already seen: %s, renamed to %s", classNameBaseOld, classNameBase);
                warnModuleDetail(msg);
                //throw new IllegalStateException(msg);
            } else {
                classNameBases.put(classNameBase, 1);
            }


            infoModuleDetail("Action template parameters:");
            infoModuleDetail("  package = " + packageName);
            infoModuleDetail("  classNameBase = " + classNameBase);
            infoModuleDetail("  path = " + path);
            infoModuleDetail("  text = " + text);
            infoModuleDetail("  position = " + position);
            infoModuleDetail("  category = " + category);

            if (path == null) {
                throw new IllegalStateException("path == null");
            }

            VelocityContext velocityContext = new VelocityContext();
            addProperty(velocityContext, "package", packageName);
            addProperty(velocityContext, "path", path);
            addProperty(velocityContext, "position", position);
            addProperty(velocityContext, "displayName", text);
            addProperty(velocityContext, "icon", smallIcon != null ? smallIcon : largeIcon);
            addProperty(velocityContext, "smallIcon", smallIcon);
            addProperty(velocityContext, "largeIcon", largeIcon);
            addProperty(velocityContext, "popupText", popuptext != null ? popuptext : text);
            addProperty(velocityContext, "category", category);
            addProperty(velocityContext, "classNameBase", classNameBase);
            addProperty(velocityContext, "baseClassName", baseClassName);
            addProperty(velocityContext, "separatorBefore", "true".equals(separatorBefore) ? position - 10 : null);
            addProperty(velocityContext, "separatorAfter", "true".equals(separatorAfter) ? position + 10 : null);
            addProperty(velocityContext, "shortDescription", shortDescr);
            addProperty(velocityContext, "longDescription", longDescr);
            addProperty(velocityContext, "selected", selected);
            addProperty(velocityContext, "mnemonic", mnemonic);
            addProperty(velocityContext, "accelerator", accelerator);

            // Collect unused action properties
            Map<String, String> unusedProperties = new LinkedHashMap<>();
            addProperty(unusedProperties, "placeBefore", placeBefore);
            addProperty(unusedProperties, "placeAfter", placeAfter);
            addProperty(unusedProperties, "interactor", interactor);
            addProperty(unusedProperties, "interactorListener", interactorListener);
            addProperty(unusedProperties, "helpId", helpId);
            addProperty(unusedProperties, "context", context);
            addProperty(unusedProperties, "toggle", toggle);
            addProperty(unusedProperties, "useAllFileFilter", useAllFileFilter);
            addProperty(unusedProperties, "sortChildren", sortChildren);
            addProperty(unusedProperties, "dialogTitle", dialogTitle);
            addProperty(unusedProperties, "targetProductNameSuffix", targetProductNameSuffix);
            addProperty(velocityContext, "properties", unusedProperties);

            File javaFile = CeresModuleProject.getFile(outputDir, project.projectDir.getName(), "src", "main", "java", packageName.replace('.', File.separatorChar), classNameBase + "Action.java");

            if (!dryRun) {
                //noinspection ResultOfMethodCallIgnored
                javaFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(javaFile)) {
                    NbCodeGenTool.this.evaluate(velocityContext, "Action.vm", writer);
                }
            }
            infoModuleDetail("Written Java source: " + javaFile);
        }

        private void addProperty(VelocityContext properties, String key, Object value) {
            if (value != null) {
                properties.put(key, value);
            }
        }

        private void addProperty(Map<String, String> properties, String key, String value) {
            if (value != null) {
                properties.put(key, value);
            }
        }
    }
}
