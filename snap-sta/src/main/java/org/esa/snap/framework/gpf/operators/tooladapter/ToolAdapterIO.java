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
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Utility class for performing various operations needed by ToolAdapterOp.
 *
 * @author Cosmin Cara
 */
public class ToolAdapterIO {

    private static final String[] userSubfolders;
    private static final Logger logger;
    private static final Map<String, String> shellExtensions;
    private static final String osFamily;

    static {
        logger = Logger.getLogger(ToolAdapterIO.class.getName());
        userSubfolders = new String[] { "adapters" };
        shellExtensions = new HashMap<>();
        shellExtensions.put("windows", ".bat");
        shellExtensions.put("linux", ".sh");
        shellExtensions.put("macosx", ".sh");
        shellExtensions.put("unsupported", "");
        String sysName = System.getProperty("os.name").toLowerCase();
        if (sysName.contains("windows")) {
            osFamily = "windows";
        } else if (sysName.contains("linux")) {
            osFamily = "linux";
        } else if (sysName.contains("mac")) {
            osFamily = "macosx";
        } else {
            osFamily = "unsupported";
        }
    }

    public static void setAdaptersPath(Path path) {
        Preferences preferences = getPreferences();
        preferences.put(ToolAdapterConstants.USER_MODULE_PATH, path.toFile().getAbsolutePath());
        try {
            preferences.sync();
        } catch (BackingStoreException e) {
            logger.severe("Cannot set adapters path in preferences: " + e.getMessage());
        }
    }

    public static void saveVariable(String name, String value) {
        if (value != null && value.length() > 0) {
            Preferences preferences = getPreferences();
            if (preferences.get(name, null) == null) {
                preferences.put(name, value);
                try {
                    preferences.sync();
                } catch (BackingStoreException e) {
                    logger.severe(String.format("Cannot set %s value in preferences: %s", name, e.getMessage()));
                }
            }
        }
    }

    public static String getVariableValue(String name, String defaultValue) {
        Preferences preferences = getPreferences();
        String retVal = preferences.get(name, null);
        if ((retVal == null || retVal.isEmpty()) && defaultValue != null) {
            saveVariable(name, defaultValue);
            retVal = defaultValue;
        }
        return retVal;
    }

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
            ToolAdapterRegistry.INSTANCE.clear();
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
        return Collections.unmodifiableCollection(ToolAdapterRegistry.INSTANCE.getOperatorMap().values());
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
        ToolAdapterOperatorDescriptor operatorDescriptor;
        File descriptorFile = new File(operatorFolder, ToolAdapterConstants.DESCRIPTOR_FILE);
        if (descriptorFile.exists()) {
            operatorDescriptor = ToolAdapterOperatorDescriptor.fromXml(descriptorFile, ToolAdapterIO.class.getClassLoader());
        } else {
            operatorDescriptor = new ToolAdapterOperatorDescriptor(operatorFolder.getName(), ToolAdapterOp.class);
            logger.warning(String.format("Missing operator metadata file '%s'", descriptorFile));
        }
        return new ToolAdapterOpSpi(operatorDescriptor) {
            @Override
            public Operator createOperator() throws OperatorException {
                ToolAdapterOp toolOperator = (ToolAdapterOp) super.createOperator();
                toolOperator.setAdapterFolder(operatorFolder);
                toolOperator.setParameterDefaultValues();
                return toolOperator;
            }
        };
        //return new ToolAdapterOpSpi(operatorDescriptor, operatorFolder);
    }

    /**
     * Reads the content of the operator Velocity template
     *
     * @param adapterName      The name of the adapter
     *
     * @throws IOException
     */
    public static String readOperatorTemplate(String adapterName) throws IOException, OperatorException {
        File file = getTemplateFile(adapterName);
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
        File file = getTemplateFile(adapterName);
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
        ToolAdapterOpSpi operatorSpi = new ToolAdapterOpSpi(operator) {
            @Override
            public Operator createOperator() throws OperatorException {
                ToolAdapterOp toolOperator = (ToolAdapterOp) super.createOperator();
                toolOperator.setAdapterFolder(moduleFolder);
                toolOperator.setParameterDefaultValues();
                return toolOperator;
            }
        };
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
    public static ToolAdapterOpSpi registerAdapter(File adapterFolder) throws OperatorException {
        ToolAdapterOpSpi operatorSpi = ToolAdapterIO.createOperatorSpi(adapterFolder);
        ToolAdapterRegistry.INSTANCE.registerOperator(operatorSpi);
        return operatorSpi;
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
        File userModulesPath = getUserAdapterPath();
        logger.info("Scanning for external tools adapters: " + userModulesPath.getAbsolutePath());
        modules.addAll(scanForAdapters(userModulesPath));
        return modules;
    }

    /**
     * Returns the location of the user-defined adapters.
     * Also, in this location packed jar adapters may be found.
     *
     * @return  The location of user-defined modules.
     */
    public static File getUserAdapterPath() {
        String userPath = Config.instance().load().preferences().get(ToolAdapterConstants.USER_MODULE_PATH, null);
        File userModulePath;
        if (userPath == null) {
            userModulePath = new File(Config.instance().userDir().toFile(), SystemUtils.getApplicationContextId());
            for (String subFolder : userSubfolders) {
                userModulePath = new File(userModulePath, subFolder);
            }
        } else {
            userModulePath = new File(userPath);
        }
        if (!userModulePath.exists() && !userModulePath.mkdirs()) {
            logger.severe("Cannot create user folder for external tool adapter extensions");
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
     * Unregisters an adapter operator and, optionally, removes its folder from file system.
     *
     * @param operator              The operator descriptor to be unregistered
     * @param removeOperatorFolder  If <code>true</code>, deletes the operator folder.
     */
    public static void removeOperator(ToolAdapterOperatorDescriptor operator, boolean removeOperatorFolder) {
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

    /**
     * In case of files that were selected via File Chooser Dialog, makes sure that a
     * copy of the file is placed in the adapter folder. If the file is already in the adapter folder,
     * nothing happens.
     *
     * @param file          The file to (potentially) copy.
     * @param adaptorAlias  The adapter alias, which is also the folder name.
     * @return              The file local to the adapter folder.
     */
    public static File ensureLocalCopy(File file, String adaptorAlias) {
        File newFile = null;
        File path = new File(getUserAdapterPath(), adaptorAlias);
        if (!file.isAbsolute()) {
            newFile = new File(path, file.getName());
        } else if (file.exists() && !file.getAbsolutePath().startsWith(path.getAbsolutePath())) {
            try {
                newFile = Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(path.getAbsolutePath(), file.getName()), StandardCopyOption.REPLACE_EXISTING).toFile();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        } else {
            newFile = file;
        }
        return newFile;
    }

    /**
     * Returns the OS-dependend shell script extension.
     * @return  For Windows: .bat, for Linux and MacOSX: .sh, for other OS: empty string
     */
    public static String getShellExtension() {
        return shellExtensions.get(osFamily);
    }

    public static String getOsFamily() { return osFamily; }

    private static List<File> scanForAdapters(File path) throws IOException {
        if (!path.exists() || !path.isDirectory()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        File[] jarFiles = path.listFiles(f -> f.getName().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                unpackAdapterJar(jarFile, null);
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

    private static File getTemplateFile(String adapterName) throws IOException, OperatorException {
        OperatorSpi spi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(adapterName);
        if (spi == null) {
            throw new OperatorException("Cannot find the operator SPI");
        }
        ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) spi.getOperatorDescriptor();
        if (operatorDescriptor == null) {
            throw new OperatorException("Cannot read the operator template file");
        }
        String templateFile = operatorDescriptor.getTemplateFileLocation();
        return new File(getUserAdapterPath(), spi.getOperatorAlias() + File.separator + templateFile);
    }

    private static void unpackAdapterJar(File jarFile, File unpackFolder) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Enumeration enumEntries = jar.entries();
        if (unpackFolder == null) {
            unpackFolder = new File(getUserAdapterPath(), jarFile.getName().replace(".jar", ""));
        }
        if (!unpackFolder.exists())
            if (!unpackFolder.mkdir()) {
                logger.warning(String.format("Cannot create folder %s", unpackFolder.getAbsolutePath()));
            }
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            File f = new File(unpackFolder, file.getName());
            if (file.isDirectory()) {
                if (!f.mkdir()) {
                    logger.warning(String.format("Cannot create folder %s", f.getAbsolutePath()));
                }
                continue;
            } else {
                if (!f.getParentFile().mkdirs()) {
                    logger.warning(String.format("Cannot create folder %s", f.getParentFile().getAbsolutePath()));
                }
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

    private static Preferences getPreferences() {
        Path storagePath = Config.instance().storagePath();
        File file = storagePath.toFile();
        if (!file.exists()) {
            try {
                if (!(file.getParentFile().mkdirs() && file.createNewFile())) {
                    logger.warning("Cannot create module preferences");
                }
            } catch (IOException e) {
                logger.severe("Error while creating module preferences: " + e.getMessage());
            }
        }
        Config instance = Config.instance().load();
        return instance.preferences();
    }
}
