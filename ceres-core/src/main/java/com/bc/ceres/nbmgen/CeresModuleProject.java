package com.bc.ceres.nbmgen;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Norman
 */
class CeresModuleProject {

    public static final String ORIGINAL_POM_XML = "pom-original.xml";
    public static final String POM_XML = "pom.xml";

    final File projectDir;

    final File pomFile;
    final Document pomDocument;

    final File moduleFile;
    final Document moduleDocument;

    interface Processor {
        void process(CeresModuleProject project) throws JDOMException, IOException;
    }

    static void processParentDir(File parentDir, Processor processor) throws JDOMException, IOException {
        List<File> moduleDirs = CeresModuleProject.getCeresModuleDirs(parentDir);
        if (moduleDirs.isEmpty()) {
            throw new IOException("no modules found in " + parentDir);
        }
        for (File moduleDir : moduleDirs) {
            processor.process(CeresModuleProject.create(moduleDir));
        }
    }

    private CeresModuleProject(File projectDir, File pomFile, Document pomDocument, File moduleFile, Document moduleDocument) {
        this.projectDir = projectDir;
        this.pomFile = pomFile;
        this.pomDocument = pomDocument;
        this.moduleFile = moduleFile;
        this.moduleDocument = moduleDocument;
    }

    private static boolean isCeresModuleProjectDir(File file) {
        return file.isDirectory()
               && getPomFile(file).exists()
               && getCeresModuleFile(file).exists();
    }

    private static CeresModuleProject create(File projectDir) throws JDOMException, IOException {

        File originalPomFile = getFile(projectDir, ORIGINAL_POM_XML);
        File sourcePomFile;
        if (!originalPomFile.exists()) {
            sourcePomFile = getFile(projectDir, POM_XML);
        } else {
            sourcePomFile = originalPomFile;
        }
        Document pomDocument = readXml(sourcePomFile);

        File moduleFile = getCeresModuleFile(projectDir);
        Document moduleDocument = readXml(moduleFile);

        return new CeresModuleProject(projectDir, sourcePomFile, pomDocument, moduleFile, moduleDocument);
    }

    private static File getPomFile(File file) {
        return getFile(file, "pom.xml");
    }

    private static File getCeresModuleFile(File file) {
        return getFile(file, "src", "main", "resources", "module.xml");
    }

    static List<File> getCeresModuleDirs(File parentDir) {
        File[] moduleDirs = parentDir.listFiles(CeresModuleProject::isCeresModuleProjectDir);
        if (moduleDirs != null) {
            return Arrays.asList(moduleDirs);
        }
        return Collections.emptyList();
    }

    static File getFile(File dir, String... names) {
        File file = dir;
        for (String name : names) {
            file = new File(file, name);
        }
        return file;
    }

    private static Document readXml(File file) throws JDOMException, IOException {
        return new SAXBuilder().build(file);
    }
}
