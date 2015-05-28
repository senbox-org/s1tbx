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
package org.esa.snap.framework.gpf.operators.tooladapter;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;
import org.openide.modules.Places;
import org.openide.util.NbPreferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static org.esa.snap.utils.JarPackager.unpackAdapterJar;

/**
 * Utility class for performing various operations needed by ToolAdapterOp.
 *
 * @author Cosmin Cara
 */
public class ToolAdapterIO {

    private static final String[] SYS_SUBFOLDERS = { "modules", "extensions", "adapters" };
    private static final String[] USER_SUBFOLDERS = { "extensions", "adapters" };
    private static File systemModulePath;
    private static File userModulePath;
    private static Logger logger = Logger.getLogger(ToolAdapterIO.class.getName());

    /**
     * Scans for adapter folders in the system and user paths and registers all
     * the adapters that have been found by adding an OperatorSpi for each adapter into
     * the OperatorSpi registry.
     *
     * @return  A list of registered OperatorSPIs
     */
    public static Collection<ToolAdapterOpSpi> searchAndRegisterAdapters() {
        Logger logger = Logger.getLogger(ToolAdapterOpSpi.class.getName());
        List<File> moduleFolders = null;
        try {
            moduleFolders = ToolAdapterIO.scanForAdapters();
        } catch (IOException e) {
            logger.severe("Failed scan for Tools descriptors: I/O problem: " + e.getMessage());
        }
        if (moduleFolders != null) {
            for (File moduleFolder : moduleFolders) {
                try {
                    ToolAdapterIO.registerAdapter(moduleFolder);
                } catch (Exception ex) {
                    logger.severe(String.format("Failed to register module %s. Problem: %s", moduleFolder.getName(), ex.getMessage()));
                }
            }
        }
        return ToolAdapterRegistry.INSTANCE.getOperatorMap().values();
    }

    /**
     * Constructs an OperatorSpi from a given folder.
     *
     * @param operatorFolder    The path containing the file/folder operator structure
     * @return                  An SPI for the read operator.
     * @throws OperatorException
     */
    public static ToolAdapterOpSpi createOperatorSpi(File operatorFolder) throws OperatorException {
        //Look for the descriptor
        File descriptorFile = new File(operatorFolder, ToolAdapterConstants.DESCRIPTOR_FILE);
        ToolAdapterOperatorDescriptor operatorDescriptor;
        if (descriptorFile.exists()) {
            operatorDescriptor = ToolAdapterOperatorDescriptor.fromXml(descriptorFile, ToolAdapterIO.class.getClassLoader());
        } else {
            operatorDescriptor = new ToolAdapterOperatorDescriptor(operatorFolder.getName(), ToolAdapterOp.class);
            logger.warning(String.format("Missing operator metadata file '%s'", descriptorFile));
        }

        return new ToolAdapterOpSpi(operatorDescriptor, operatorFolder);
    }

    /**
     * Reads the content of the operator Velocity template
     *
     * @param adapterName      The name of the adapter
     * @return
     * @throws IOException
     */
    public static String readOperatorTemplate(String adapterName) throws IOException, OperatorException {
        File file = getTemplateFile(adapterName, true);
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return new String(encoded, Charset.defaultCharset());
    }

    /**
     * Writes the content of the operator Velocity template.
     *
     * @param adapterName   The name of the operator
     * @param content       The content of the template
     * @throws IOException
     */
    public static void writeOperatorTemplate(String adapterName, String content) throws IOException {
        File file = getTemplateFile(adapterName, false);
        saveFileContent(file, content);
    }

    /**
     * Removes the operator both from the SPI registry and also tries to remove the adapter
     * folder from disk.
     *
     * @param operator      The operator descriptor to be removed
     */
    public static void removeOperator(ToolAdapterOperatorDescriptor operator) {
        removeOperator(operator, true);
    }

    /**
     * Saves any changes to the operator and registers it (in case of newly created ones).
     *
     * @param operator          The operator descriptor
     * @param templateContent   The content of the Velocity template
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void saveAndRegisterOperator(ToolAdapterOperatorDescriptor operator, String templateContent) throws IOException, URISyntaxException {
        removeOperator(operator, false);
        File rootFolder = getUserAdapterPath();
        File moduleFolder = new File(rootFolder, operator.getAlias());
        if (!moduleFolder.exists()) {
            if (!moduleFolder.mkdir()) {
                throw new OperatorException("Operator folder " + moduleFolder + " could not be created!");
            }
        }
        ToolAdapterOpSpi operatorSpi = new ToolAdapterOpSpi(operator, moduleFolder);
        File descriptorFile = new File(moduleFolder, ToolAdapterConstants.DESCRIPTOR_FILE);
        if (!descriptorFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            descriptorFile.getParentFile().mkdirs();
            if (!descriptorFile.createNewFile()) {
                throw new OperatorException("Operator file " + descriptorFile + " could not be created!");
            }
        }
        String xmlContent = operator.toXml(ToolAdapterIO.class.getClassLoader());
        saveFileContent(descriptorFile, xmlContent);
        ToolAdapterRegistry.INSTANCE.registerOperator(operatorSpi);
        writeOperatorTemplate(operator.getName(), templateContent);
    }

    /**
     * Register a tool adapter as an operator.
     *
     * @param adapterFolder the folder of the tool adapter
     * @throws OperatorException in case of an error
     */
    public static void registerAdapter(File adapterFolder) throws OperatorException {
        ToolAdapterOpSpi operatorSpi = ToolAdapterIO.createOperatorSpi(adapterFolder);
        if (adapterFolder.getAbsolutePath().startsWith(systemModulePath.getAbsolutePath())) {
            ((ToolAdapterOperatorDescriptor) operatorSpi.getOperatorDescriptor()).setSystem(true);
        }
        ToolAdapterRegistry.INSTANCE.registerOperator(operatorSpi);
    }

    /**
     * Scans for adapter folders in the system and user paths.
     *
     * @return  A list of adapter folders.
     * @throws IOException
     */
    public static List<File> scanForAdapters() throws IOException {
        logger.log(Level.INFO, "Loading external tools...");
        List<File> modules = new ArrayList<>();
        File systemModulesPath = getSystemAdapterPath();
        logger.info("Scanning for external tools adapters: " + systemModulesPath.getAbsolutePath());
        modules.addAll(scanForAdapters(systemModulesPath));
        File userModulesPath = getUserAdapterPath();
        logger.info("Scanning for external tools adapters: " + userModulesPath.getAbsolutePath());
        modules.addAll(scanForAdapters(userModulesPath));
        return modules;
    }

    /**
     * Returns the location of the user-defined adapters.
     * An user-defined adapter is either a system tool adapter that was modified by the user
     * or a new tool adapter defined by the user.
     * Also, in this location packed jar adapters may be found.
     *
     * @return  The location of user-defined modules.
     */
    public static File getUserAdapterPath() {
        if (userModulePath == null) {
            String userPath = null;
            Preferences preferences = NbPreferences.forModule(ToolAdapterIO.class);
            if ((userPath = preferences.get("user.module.path", null)) == null) {
                userModulePath = new File(Places.getUserDirectory(), SystemUtils.getApplicationContextId());
                for (String subFolder : USER_SUBFOLDERS) {
                    userModulePath = new File(userModulePath, subFolder);
                }
            } else {
                userModulePath = new File(userPath);
            }
            if (!userModulePath.exists() && !userModulePath.mkdirs()) {
                logger.severe("Cannot create user folder for external tool adapter extensions");
            }
        }
        return userModulePath;
    }

    /**
     * Writes the given content in the specified file.
     *
     * @param file          The target file
     * @param content       The content to be written
     * @throws IOException
     */
    public static void saveFileContent(File file, String content) throws IOException{
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            writer.flush();
            writer.close();
        }
    }

    /**
     * Returns the location where the system adapter modules should be looked for.
     * A system adapter module is a module that is included in the distribution bundle.
     *
     * @return  The system modules location
     */
    public static File getSystemAdapterPath() {
        if (systemModulePath == null) {
            // Uncommented by Norman 28.05.2015, please review!
            /*
            String applicationHomePropertyName = SystemUtils.getApplicationHomePropertyName();
            if (applicationHomePropertyName == null) {
                applicationHomePropertyName = "user.dir";
            }
            String homeFolder = System.getProperty(applicationHomePropertyName);
            if (homeFolder == null) {
                homeFolder = System.getProperty("user.dir");
            }
            systemModulePath = new File(homeFolder);
            */
            systemModulePath = Config.instance().userDir().toFile();
            for (String subFolder : SYS_SUBFOLDERS) {
                systemModulePath = new File(systemModulePath, subFolder);
            }
            if (!systemModulePath.exists() && !systemModulePath.mkdirs()) {
                logger.severe("Cannot create system folder for external tool adapter extensions");
            }
        }
        return systemModulePath;
    }

    private static List<File> scanForAdapters(File path) throws IOException {
        if (!path.exists() || !path.isDirectory()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        File[] jarFiles = path.listFiles(f -> f.getName().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                unpackAdapterJar(jarFile);
            }
        }
        File[] moduleFolders = path.listFiles();
        List<File> modules = new ArrayList<>();
        if (moduleFolders != null) {
            for (File moduleFolder : moduleFolders) {
                File descriptorFile = new File(moduleFolder, ToolAdapterConstants.DESCRIPTOR_FILE);
                if (descriptorFile.exists()) {
                    modules.add(moduleFolder);
                }
            }
        }
        return modules;
    }

    private static File getTemplateFile(String adapterName, boolean forReading) throws IOException, OperatorException {
        OperatorSpi spi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(adapterName);
        if (spi == null) {
            throw new OperatorException("Cannot find the operator SPI");
        }
        ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) spi.getOperatorDescriptor();
        if (operatorDescriptor == null) {
            throw new OperatorException("Cannot read the operator template file");
        }
        String templateFile = operatorDescriptor.getTemplateFileLocation();
        File template = new File(forReading ? getSystemAdapterPath() : getUserAdapterPath(), spi.getOperatorAlias() + File.separator + templateFile);
        if (!template.exists()) {
            template = new File(getUserAdapterPath(), spi.getOperatorAlias() + File.separator + templateFile);
        }
        return template;
    }

    private static void removeOperator(ToolAdapterOperatorDescriptor operator, boolean removeOperatorFolder) {
        if (!operator.isSystem()) {
            ToolAdapterRegistry.INSTANCE.removeOperator(operator);
            if (removeOperatorFolder) {
                File rootFolder = getUserAdapterPath();
                File moduleFolder = new File(rootFolder, operator.getAlias());
                if (moduleFolder.exists()) {
                    if (!FileUtils.deleteTree(moduleFolder)) {
                        logger.warning(String.format("Folder %s cannot be deleted", moduleFolder.getAbsolutePath()));
                    }
                }
            }
        }
    }
}
