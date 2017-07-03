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
package org.esa.snap.core.gpf.operators.tooladapter;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

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
    private static final Attributes.Name typeKey;
    private static final String[] excludedClusters;

    static {
        logger = Logger.getLogger(ToolAdapterIO.class.getName());
        userSubfolders = new String[] { "tool-adapters" };
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
        typeKey = new Attributes.Name("OpenIDE-Module-Type");
        excludedClusters = new String[] { "bin", "etc", "platform", "ide", "java" };
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
            preferences.put(name, value);
            try {
                preferences.sync();
            } catch (BackingStoreException e) {
                logger.severe(String.format("Cannot set %s value in preferences: %s", name, e.getMessage()));
            }
        }
    }

    /**
     * Returns the value of the named variable.
     * If the variable doesn't exist, it will be created with the given default value.
     *
     * @param name          The name of the variable
     * @param defaultValue  Default value for the variable if not exists
     * @param isShared      If this is a shared variable (i.e. shared among adapters)
     * @return              The value (existing or default) of the variable
     */
    public static String getVariableValue(String name, String defaultValue, boolean isShared) {
        Preferences preferences = getPreferences();
        String retVal = preferences.get(name, null);
        if ((retVal == null || retVal.isEmpty()) && defaultValue != null) {
            if (isShared) {
                saveVariable(name, defaultValue);
            }
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
                    ToolAdapterIO.registerAdapter(moduleFolder.toPath());
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
    public static ToolAdapterOpSpi createOperatorSpi(Path operatorFolder) throws OperatorException {
        //Look for the descriptor
        ToolAdapterOperatorDescriptor operatorDescriptor;
        Path descriptorFile = operatorFolder.resolve(ToolAdapterConstants.DESCRIPTOR_FILE);
        if (Files.exists(descriptorFile)) {
            operatorDescriptor = ToolAdapterOperatorDescriptor.fromXml(descriptorFile.toFile(), ToolAdapterIO.class.getClassLoader());
            return new ToolAdapterOpSpi(operatorDescriptor) {
                @Override
                public Operator createOperator() throws OperatorException {
                    ToolAdapterOp toolOperator = (ToolAdapterOp) super.createOperator();
                    toolOperator.setAdapterFolder(operatorFolder.toFile());
                    toolOperator.setParameterDefaultValues();
                    return toolOperator;
                }
            };
        } else {
            throw new OperatorException(String.format("Missing operator metadata file '%s'", descriptorFile));
        }
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
     * Creates a copy of the adapter folder.
     *
     * @param operatorDescriptor    The operator descriptor for which to backup the folder
     * @return                      The path of the backup folder
     * @throws IOException
     */
    public static Path backupOperator(ToolAdapterOperatorDescriptor operatorDescriptor) throws IOException {
        Path root = getAdaptersPath();
        String alias = operatorDescriptor.getAlias();
        Path modulePath = root.resolve(alias);
        Path backupRoot = SystemUtils.getAuxDataPath();
        Path backupPath = backupRoot.resolve(alias + "_" + String.valueOf(System.currentTimeMillis()));
        copy(modulePath, backupPath);
        return backupPath;
    }

    /**
     * Restores the folder of a descriptor from a backup folder.
     *
     * @param operatorDescriptor    The operator descriptor for which to restore the folder
     * @param backupPath            The path from which to restore the folder
     * @return                      The path of the resored folder
     * @throws IOException
     */
    public static Path restoreOperator(ToolAdapterOperatorDescriptor operatorDescriptor, Path backupPath) throws IOException {
        Path root = getAdaptersPath();
        String alias = operatorDescriptor.getAlias();
        Path modulePath = root.resolve(alias);
        copy(backupPath, modulePath);
        return modulePath;
    }

    /**
     * Saves any changes to the operator and registers it (in case of newly created ones).
     *
     * @param operator          The operator descriptor
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void saveAndRegisterOperator(ToolAdapterOperatorDescriptor operator) throws IOException, URISyntaxException {
        Path rootFolder = getAdaptersPath();
        Path moduleFolder = rootFolder.resolve(operator.getAlias());
        removeOperator(operator, true);
        Files.createDirectories(moduleFolder);
        ToolAdapterOpSpi operatorSpi = new ToolAdapterOpSpi(operator) {
            @Override
            public Operator createOperator() throws OperatorException {
                ToolAdapterOp toolOperator = (ToolAdapterOp) super.createOperator();
                toolOperator.setAdapterFolder(moduleFolder.toFile());
                toolOperator.setParameterDefaultValues();
                return toolOperator;
            }
        };
        Path descriptorFile = moduleFolder.resolve(ToolAdapterConstants.DESCRIPTOR_FILE);
        Files.createDirectories(descriptorFile.getParent());
        String xmlContent = operator.toXml(ToolAdapterIO.class.getClassLoader());
        Files.write(descriptorFile, xmlContent.getBytes(), StandardOpenOption.CREATE);
        operator.getTemplate().save();
        operator.getToolParameterDescriptors().stream()
                .filter(ToolParameterDescriptor::isTemplateParameter)
                .map(p -> (TemplateParameterDescriptor)p)
                .forEach(p -> {
                    try {
                        p.getTemplate().save();
                    } catch (IOException e) {
                        logger.severe(String.format("Failed to save template for parameter %s [%s]", p.getName(), e.getMessage()));
                    }
                });
        ToolAdapterRegistry.INSTANCE.registerOperator(operatorSpi);
    }

    /**
     * Register a tool adapter as an operator.
     *
     * @param adapterFolder the folder of the tool adapter
     * @throws OperatorException in case of an error
     */
    public static ToolAdapterOpSpi registerAdapter(Path adapterFolder) throws OperatorException {
        ToolAdapterOpSpi operatorSpi = ToolAdapterIO.createOperatorSpi(adapterFolder);
        ToolAdapterRegistry.INSTANCE.registerOperator(operatorSpi);
        return operatorSpi;
    }

    public static ToolAdapterOpSpi registerAdapter(File adapterFolder) throws OperatorException {
        return registerAdapter(adapterFolder.toPath());
    }

    /**
     * Scans for adapter folders in the system and user paths.
     *
     * @return  A list of adapter folders.
     * @throws IOException
     */
    public static List<File> scanForAdapters() throws IOException {
        logger.info("Initializing external tool adapters");
        List<File> modules = new ArrayList<>();
        Set<Path> modulesPath = getClusterModulesPaths();
        for (Path clusterPath : modulesPath) {
            final Preferences preferences = Config.instance(clusterPath.getFileName().toString()).preferences();
            SystemVariable.addLookupReference(preferences::get);
        }
        modulesPath.add(getAdaptersPath());
        for (Path path : modulesPath) {
            logger.fine("Scanning for external tools adapters: " + path.toAbsolutePath().toString());
            try {
                modules.addAll(scanForAdapters(path));
            } catch (IOException ex) {
                logger.severe(String.format("Failed to scan %s [reason: %s]", path, ex.getMessage()));
            }
        }

        return modules;
    }

    /**
     * Returns the location of the user-defined adapters.
     * Also, in this location packed jar adapters may be found.
     *
     * @return  The location of user-defined modules.
     */
    public static Path getAdaptersPath() {
        String userPath = Config.instance().load().preferences().get(ToolAdapterConstants.USER_MODULE_PATH, null);
        Path userModulePath;
        if (userPath == null) {
            userModulePath = SystemUtils.getAuxDataPath();
            for (String subFolder : userSubfolders) {
                userModulePath = userModulePath.resolve(subFolder);
            }
        } else {
            userModulePath = Paths.get(userPath);
        }
        try {
            Files.createDirectories(userModulePath);
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
        }
        if (!Files.exists(userModulePath)) {
            logger.severe("Cannot create user folder for external tool adapter extensions");
        }
        return userModulePath;
    }

    public static File getUserAdapterPath() {
        return getAdaptersPath().toFile();
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
            Path rootFolder = getAdaptersPath();
            Path moduleFolder = rootFolder.resolve(operator.getAlias());
            if (Files.exists(moduleFolder)) {
                try {
                    deleteFolder(moduleFolder);
                } catch (IOException e) {
                    logger.warning(String.format("Folder %s cannot be deleted [%s]", moduleFolder.toAbsolutePath(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Returns the OS-dependend shell script extension.
     * @return  For Windows: .bat, for Linux and MacOSX: .sh, for other OS: empty string
     */
    public static String getShellExtension() {
        return shellExtensions.get(osFamily);
    }

    /**
     * Returns the current operating system.
     */
    public static String getOsFamily() { return osFamily; }

    /**
     * Converts adapter descriptor (prior to 4.0) to the new format
     * @param modulePath    The adapter path
     * @throws IOException
     */
    public static void convertAdapter(Path modulePath) throws IOException {
        Path descriptorPath = Files.isRegularFile(modulePath) ? modulePath : modulePath.resolve("META-INF").resolve("descriptor.xml");
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", 2);
            StreamSource xslStream = new StreamSource(ToolAdapterIO.class.getResourceAsStream("transform.xsl"));
            Transformer transformer = factory.newTransformer(xslStream);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamSource input = new StreamSource(new StringReader(new String(Files.readAllBytes(descriptorPath))));
            StringWriter writer = new StringWriter();
            StreamResult output = new StreamResult(writer);
            transformer.transform(input, output);
            Files.write(descriptorPath, writer.toString().getBytes());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Deletes the given folder and its content.
     * @param location      The folder to delete
     * @throws IOException
     */
    public static void deleteFolder(Path location) throws IOException {
        if (Files.exists(location)) {
            Files.walkFileTree(location, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Ensure that the given path has all the permissions set.
     * This is especially useful for Linux/MacOsX executables.
     *
     * @param path  The path to fix permissions for.
     */
    public static void fixPermissions(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Stream<Path> files = Files.list(path);
            files.forEach(p -> {
                try {
                    fixPermissions(p);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("OpenJPEG configuration error: failed to fix permissions on " + path);
                }
            });
        } else {
            if (IS_OS_UNIX) {
                Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE));
                try {
                    Files.setPosixFilePermissions(path, permissions);
                } catch (IOException e) {
                    // can't set the permissions for this file, eg. the file was installed as root
                    // send a warning message, user will have to do that by hand.
                    SystemUtils.LOG.severe("Can't set execution permissions for executable " + path.toString() +
                            ". If required, please ask an authorised user to make the file executable.");
                }
            }
        }
    }

    private static List<File> scanForAdapters(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new FileNotFoundException(path.toAbsolutePath().toString());
        }
        File[] jarFiles = path.toFile().listFiles(f -> f.getName().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                try {
                    unpackAdapterJar(jarFile, null);
                } catch (IOException ioEx) {
                    logger.warning(ioEx.getMessage());
                }
            }
        }
        File[] moduleFolders = path.toFile().listFiles();
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

    private static void unpackAdapterJar(File jarFile, File unpackFolder) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Manifest manifest = jar.getManifest();
        Attributes manifestEntries = manifest.getMainAttributes();
        if (manifestEntries.containsKey(typeKey) && "STA".equals(manifestEntries.getValue(typeKey.toString()))) {
            Enumeration enumEntries = jar.entries();
            if (unpackFolder == null) {
                unpackFolder = getAdaptersPath().resolve(jarFile.getName().replace(".jar", "")).toFile();
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
    }

    private static Preferences getPreferences() {
        Config instance = Config.instance();
        Path storagePath = instance.storagePath();
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
        try {
            instance = Config.instance().load();
        } catch (Exception e) {
            logger.severe("Cannot read preferences: " + e.getMessage());
        }
        return instance.preferences();
    }

    public static void copy(Path source, Path destination) throws IOException{
        Set<FileVisitOption> options = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        final CopyOption[] copyOptions = new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING };
        Files.walkFileTree(source, options, 3, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path newDirectory = destination.resolve(source.relativize(dir));
                try {
                    Files.copy(dir, newDirectory, copyOptions);
                } catch (FileAlreadyExistsException ignored) { }
                catch(IOException x){
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), copyOptions);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static Path zip(Path source, Path zipFile) throws IOException {
        if (source == null || zipFile == null) {
            throw new IllegalArgumentException("One of the arguments is null");
        }
        Files.deleteIfExists(zipFile);
        zipFile = Files.createFile(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            if (Files.isRegularFile(source)) {
                ZipEntry zipEntry = new ZipEntry(source.getFileName().toString());
                zos.putNextEntry(zipEntry);
                zos.write(Files.readAllBytes(source));
                zos.closeEntry();
            } else {
                Files.walk(source, 3)
                        //.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                if (Files.isDirectory(path)) {
                                    zipFolder(source, path, zos);
                                } else {
                                    ZipEntry zipEntry = new ZipEntry(source.relativize(path).toString());
                                    zos.putNextEntry(zipEntry);
                                    zos.write(Files.readAllBytes(path));
                                    zos.closeEntry();
                                }
                            } catch (Exception ex) {
                                SystemUtils.LOG.warning(ex.getMessage());
                            }
                        });
            }
        }
        return zipFile;
    }

    private static void zipFolder(Path root, Path folder, ZipOutputStream zipStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(root.relativize(folder).toString() + "/");
        zipStream.putNextEntry(zipEntry);
        zipStream.closeEntry();
    }

    /**
     * Uncompress the source zip file into the destination path.
     *
     * @param sourceFile    The zip file
     * @param destination   The destination directory
     */
    public static void unzip(Path sourceFile, Path destination, ProgressMonitor progressMonitor, int totalTasks) throws IOException {
        if (sourceFile == null || destination == null) {
            throw new IllegalArgumentException("One of the arguments is null");
        }
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }
        byte[] buffer;
        try (ZipFile zipFile = new ZipFile(sourceFile.toFile())) {
            ZipEntry entry;
            progressMonitor.setSubTaskName(String.format("Extracting %s...", zipFile.getName()));
            int size = zipFile.size();
            int workUnits = 100 / totalTasks;
            int count = 0;
            int progress;
            // Inspect all zip entries to see if there is a root zip folder.
            // If yes, it will be discarded so that the uncompression root folder is
            // the zip file name
            LinkedHashMap<String, String> fileNames = new LinkedHashMap<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                fileNames.put(entry.getName(), entry.getName());
            }
            String firstEntry = fileNames.values().iterator().next();
            String token = firstEntry.substring(0, firstEntry.indexOf("/"));
            if (fileNames.values().stream().allMatch(n -> n.startsWith(token))) {
                fileNames.values().forEach(n -> n = n.substring(n.indexOf("/") + 1));
            }
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                Path filePath = destination.resolve(fileNames.get(entry.getName()));
                if (!Files.exists(filePath)) {
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            try (BufferedOutputStream bos = new BufferedOutputStream(
                                    new FileOutputStream(filePath.toFile()))) {
                                buffer = new byte[4096];
                                int read;
                                while ((read = inputStream.read(buffer)) > 0) {
                                    bos.write(buffer, 0, read);
                                }
                            }
                        }
                    }
                }
                progress = 100 - workUnits + (int) ((double)count++ / (double)size * (double)workUnits);
                progressMonitor.worked(progress);
            }
        } catch (Exception ex){
            throw ex;
        }
    }

    static List<ProductReaderPlugIn> getReaderPlugInsByExtension(String extension) {
        List<ProductReaderPlugIn> plugIns = new ArrayList<>();
        final Iterator<ProductReaderPlugIn> readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        while (readerPlugIns.hasNext()) {
            final ProductReaderPlugIn plugIn = readerPlugIns.next();
            if (Arrays.stream(plugIn.getDefaultFileExtensions()).filter(e -> e.equalsIgnoreCase(extension)).count() > 0) {
                plugIns.add(plugIn);
            }
        }
        return plugIns;
    }

    private static Set<Path> getClusterModulesPaths() {
        Set<Path> clusterNames = new HashSet<>();
        Path installDir = Config.instance().installDir();
        Path clustersFile = installDir.resolve("etc").resolve("snap.clusters");
        if (Files.isRegularFile(clustersFile)) {
            try {
                clusterNames = Files.readAllLines(clustersFile).stream()
                        .filter(name -> !name.trim().isEmpty())
                        .map(installDir::resolve)
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                logger.severe(String.format("Failed to load clusters file from '%s'", clustersFile));
            }
        }
        for (String clusterName : excludedClusters) {
            clusterNames.remove(installDir.resolve(clusterName));
        }
        clusterNames.remove(installDir.resolve("snap"));
        return clusterNames;
    }
}
