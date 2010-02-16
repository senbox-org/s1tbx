package com.bc.ceres.deploy;

import com.bc.ceres.core.CoreException;
import static com.bc.ceres.core.runtime.Constants.MODULE_MANIFEST_NAME;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.JarFilenameFilter;
import com.bc.ceres.core.runtime.internal.ModuleReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Converts all module files (ZIPs and JARs) found in a given modules source directory to deployable
 * modules in a repository output directory.
 * <p/>The deployable modules are written to the repository output directory
 * in the following form:
 * <pre>
 *     `- <repositoryDir>
 *        `- <manifest.symbolicName>-<manifest.version>/
 *           |- <manifest.symbolicName>-<manifest.version>.jar
 *           |- module.xml
 *           |- about.html (optional)
 *           `- LICENSE.txt (optional)
 * </pre>
 * Output modules files are named after the manifest information,
 * independently of the name of the source modules.
 */
public class DeployMain {

    private String[] mandatoryFilePaths = new String[]{
            MODULE_MANIFEST_NAME,
    };

    private String[] optionalFilePaths = new String[]{
            "about.html",
            "LICENSE.txt"
    };

    private Logger logger;
    private int warningCount;
    private int errorCount;

    /**
     * Deploys a module to a target repository directory.
     *
     * @param args {@code args[0]} is a module directory or JAR file,
     *             {@code args[1]} is the target repository directory.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: deploy <modules-dir>|<module-jar> <repository-dir>");
            return;
        }
        File modulesFile = new File(args[0]);
        File repositoryDir = new File(args[1]);
        new DeployMain().run(modulesFile, repositoryDir);
    }

    private void run(File modulesFile, File repositoryDir) {
        logger = Logger.getAnonymousLogger();

        File[] moduleFiles;

        if (modulesFile.isDirectory()) {

            moduleFiles = modulesFile.listFiles();
            if (moduleFiles == null) {
                String msg = "No files found in " + modulesFile;
                warn(msg);
                return;
            }
            info(String.format("Deploying modules of [%s] to [%s]...", modulesFile, repositoryDir));
        } else {
            moduleFiles = new File[]{modulesFile};
            info(String.format("Deploying module [%s] to [%s]...", modulesFile, repositoryDir));
        }

        repositoryDir.mkdirs();

        int deployedModuleCount = 0;
        for (File moduleFile : moduleFiles) {
            if (moduleFile.isDirectory() || JarFilenameFilter.isJarName(moduleFile.getName())) {
                try {
                    deployModuleFile(moduleFile, repositoryDir);
                    deployedModuleCount++;
                } catch (Exception e) {
                    error("Failed to deploy " + moduleFile.getName() + ": " + e.getMessage());
                }
            } else {
                warn("Cannot deploy file of unknown type: " + moduleFile.getName());
            }
        }

        info(String.format("Deploying completed with %d error(s), %d warning(s).", errorCount, warningCount));
        info(String.format("%d module(s) deployed", deployedModuleCount));
    }

    private void info(String msg) {
        logger.info(msg);
    }

    private void error(String msg) {
        logger.severe(msg);
        errorCount++;
    }

    private void warn(String msg) {
        logger.warning(msg);
        warningCount++;
    }

    private void deployModuleFile(File moduleFile, File repositoryDir) throws CoreException, IOException {
        ModuleReader moduleReader = new ModuleReader(logger);
        Module module = moduleReader.readFromLocation(moduleFile);
        String moduleFileNameNE = module.getSymbolicName() + "-" + module.getVersion();
        File moduleRepositoryDir = new File(repositoryDir, moduleFileNameNE);
        String moduleFileName = moduleFile.getName();
        info("Deploying " + moduleFileName + " to " + moduleRepositoryDir);
        moduleRepositoryDir.mkdirs();
        if (!moduleRepositoryDir.isDirectory()) {
            throw new IOException("Failed to create " + moduleRepositoryDir.getPath());
        }
        if (moduleFile.isDirectory()) {
            copyFiles(moduleFile, moduleRepositoryDir);
            jarIntoDir(moduleFile, moduleRepositoryDir, moduleFileNameNE + ".jar");
        } else {
            extractFiles(moduleFile, moduleRepositoryDir);
            copyIntoDir(moduleFile, true, moduleRepositoryDir, moduleFileNameNE + getExtension(moduleFileName));
        }
    }

    private void jarIntoDir(File moduleDir, File moduleRepositoryDir, String jarName) throws IOException {
        File jarFile = new File(moduleRepositoryDir, jarName);
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
        try {
            copyIntoJar(moduleDir, "", jarOutputStream);
        } finally {
            jarOutputStream.close();
        }
    }

    private void copyIntoJar(File baseDir, String basePath, JarOutputStream jarOutputStream) throws IOException {
        String[] fileNames = baseDir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                String entryName = basePath + (basePath.length() > 0 ? "/" : "") + fileName;
                File file = new File(baseDir, fileName);
                if (file.isDirectory()) {
                    copyIntoJar(file, entryName, jarOutputStream);
                } else {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        JarEntry jarEntry = new JarEntry(entryName);
                        jarOutputStream.putNextEntry(jarEntry);
                        copy(fileInputStream, jarOutputStream);
                        jarOutputStream.closeEntry();
                    } finally {
                        fileInputStream.close();
                    }
                }
            }
        }
    }

    private String getExtension(String name) {
        return name.substring(name.lastIndexOf('.'));
    }

    private void extractFiles(File moduleFile, File moduleRepositoryDir) throws IOException {
        ZipFile zip = new ZipFile(moduleFile);
        try {
            for (String mandatoryFilePath : mandatoryFilePaths) {
                extractToDir(zip, mandatoryFilePath, true, moduleRepositoryDir);
            }
            for (String optionalFilePath : optionalFilePaths) {
                extractToDir(zip, optionalFilePath, false, moduleRepositoryDir);
            }
        } finally {
            zip.close();
        }
    }

    private void copyFiles(File moduleDir, File moduleRepositoryDir) throws IOException {
        for (String mandatoryFilePath : mandatoryFilePaths) {
            copyIntoDir(new File(moduleDir, mandatoryFilePath), true, moduleRepositoryDir);
        }
        for (String optionalFilePath : optionalFilePaths) {
            copyIntoDir(new File(moduleDir, optionalFilePath), false, moduleRepositoryDir);
        }
    }

    private void extractToDir(ZipFile zip, String entryName, boolean mustExists, File targetDir) throws IOException {
        File targetFile = new File(targetDir, entryName);
        targetFile.getParentFile().mkdirs();
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            String message = "ZIP entry not found: " + entryName;
            if (mustExists) {
                throw new IOException(message);
            } else {
                warn(message);
                return;
            }
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = zip.getInputStream(entry);
            outputStream = new FileOutputStream(targetFile);
            copy(inputStream, outputStream);
        } finally {
            closeBoth(inputStream, outputStream);
        }
    }

    private boolean copyIntoDir(File sourceFile, boolean mustExist, File targetDir) throws IOException {
        return copyIntoDir(sourceFile, mustExist, targetDir, sourceFile.getName());
    }

    private boolean copyIntoDir(File sourceFile, boolean mustExist, File targetDir, String targetName) throws
                                                                                                       IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
        } catch (FileNotFoundException e) {
            if (mustExist) {
                throw e;
            } else {
                return false;
            }
        }
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(new File(targetDir, targetName));
            copy(inputStream, outputStream);
        } finally {
            closeBoth(inputStream, outputStream);
        }
        return true;
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int numBytes;
        do {
            numBytes = inputStream.read(buffer);
            if (numBytes != -1) {
                outputStream.write(buffer, 0, numBytes);
            }
        } while (numBytes > 0);
    }

    private void closeBoth(InputStream inputStream, OutputStream outputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
