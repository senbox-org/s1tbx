package com.bc.ceres.nbmgen;

import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Usage:
 * <pre>
 *    NbmGenMain &lt;project-dir&gt; &lt;dry-run&gt;
 * </pre>
 * <p>
 * For NbCodeGenMain:
 * </p>
 * <pre>
 *    NbCodeGenMain . true
 * </pre>
 * <p>Scans a ${project-dir} for Ceres modules ({@code pom.xml} + {@code src/main/resources/module.xml})
 * and generates NB Java code stubs for all Ceres extensions.
 * </p>
 *
 * @author Norman
 */
public class NbCodeGenMain implements CeresModuleProject.Processor {

    File projectDir;
    boolean dryRun;

    public NbCodeGenMain(File projectDir, boolean dryRun) {
        this.projectDir = projectDir;
        this.dryRun = dryRun;
    }

    public static void main(String[] args) throws JDOMException, IOException {

        File projectDir = new File(args[0]);
        boolean dryRun = args[1].equals("true");

        NbCodeGenMain processor = new NbCodeGenMain(projectDir, dryRun);

        CeresModuleProject.processParentDir(projectDir, processor);
    }

    @Override
    public void process(CeresModuleProject project) throws JDOMException, IOException {

        System.out.println("Project [" + project.projectDir.getName() + "]:");

        Element moduleElement = project.moduleDocument.getRootElement();


        List extensionPointElements = moduleElement.getChildren("extensionPoint");
        for (Object obj : extensionPointElements) {
            Element extensionPointElement = (Element) obj;
            String id = extensionPointElement.getAttributeValue("id");
            infoModuleDetail("Found extensionPoint.id = " + id);
        }

        List extensionElements = moduleElement.getChildren("extension");
        for (Object obj : extensionElements) {
            Element extensionElement = (Element) obj;
            String point = extensionElement.getAttributeValue("point");
            infoModuleDetail("Found extension.point = " + point);

            List<Element> extensionConfigs = (List<Element>) extensionElement.getChildren();
            if (!extensionConfigs.isEmpty()) {
                switch (point) {
                    case "snap-ceres-core:applications":
                        convertApplications(point, extensionConfigs);
                        break;
                    case "snap-ceres-core:serviceProviders":
                        convertServiceProviders(point, extensionConfigs);
                        break;
                    case "snap-ceres-core:adapters":
                        convertAdapters(point, extensionConfigs);
                        break;
                    case "snap-core:rgbProfiles":
                        convertRgbProfile(point, extensionConfigs);
                        break;
                    case "snap-ui:applicationDescriptors":
                        convertApplicationDescriptor(point, extensionConfigs);
                        break;
                    case "snap-ui:helpSets":
                        convertHelpSet(point, extensionConfigs);
                        break;
                    case "snap-ui:actions":
                        convertAction(point, extensionConfigs);
                        break;
                    case "snap-ui:actionGroups":
                        convertActionGroup(point, extensionConfigs);
                        break;
                    case "snap-ui:toolViews":
                        convertToolView(point, extensionConfigs);
                        break;
                    case "snap-ui:layerSources":
                        convertLayerSource(point, extensionConfigs);
                        break;
                    case "snap-ui:layerEditors":
                        convertLayerEditor(point, extensionConfigs);
                        break;
                    case "snap-graph-builder:OperatorUIs":
                        convertGraphBuilderOperatorUIs(point, extensionConfigs);
                        break;
                    default:
                        warnModuleDetail("Don't know what to do with this extension: " + point);
                        break;
                }
            }
        }
    }

    private void convertApplications(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertServiceProviders(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertAdapters(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertRgbProfile(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertApplicationDescriptor(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertHelpSet(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertAction(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertActionGroup(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertToolView(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertLayerSource(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertLayerEditor(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private void convertGraphBuilderOperatorUIs(String point, List<Element> extensionElements) {
        warnModuleDetail("Code generation not implemented yet: " + point);
    }

    private static void infoModuleDetail(String msg) {
        System.out.println("- " + msg);
    }

    private static void warnModuleDetail(String msg) {
        System.out.println("- WARNING: " + msg);
    }
}
