/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core.runtime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static org.junit.Assert.*;

public abstract class AbstractRuntimeTest {
    private ArrayList<File> fileStack = new ArrayList<>();
    private String baseDirPath;
    private String dirPath;
    private String contextId;

    public String getBaseDirPath() {
        return baseDirPath;
    }

    public String getDirPath() {
        return dirPath;
    }

    public String getContextId() {
        return contextId;
    }

    @Before
    public void setUp() throws Exception {
        File targetDir = new File("target");
        File testdataDir = new File(targetDir, "test-data");
        mkdir0(targetDir);
        mkdir0(testdataDir);
        baseDirPath = testdataDir.getAbsolutePath();
    }

    @After
    public void tearDown() throws Exception {
        deleteFileStack();
    }

    protected void clearContextSystemProperties(String contextId) {
        System.clearProperty("ceres.context");
        System.clearProperty(contextId+".home");
        System.clearProperty(contextId+".app");
        System.clearProperty(contextId+".mainClass");
        System.clearProperty(contextId+".classpath");
        System.clearProperty(contextId+".modules");
        System.clearProperty(contextId+".config");
        System.clearProperty(contextId+".libDirs");
        System.clearProperty(contextId+".debug");
        System.clearProperty(contextId+".logLevel");
        System.clearProperty(contextId+".consoleLog");
    }

    protected void initContextHomeDir(String contextId, String dirPath, String configContent) throws IOException {
        this.contextId = contextId;
        this.dirPath = dirPath;

        String configFilename= contextId + ".config";

        mkdir(dirPath);

        mkdir(dirPath + "/config");
        touch(dirPath + "/config/" + configFilename, configContent.replace("\\", "/").getBytes());

        mkdir(dirPath + "/lib");
        touch(dirPath + "/lib/snap-launcher-0.5.jar");
        touch(dirPath + "/lib/xstream-1.2.jar");
        touch(dirPath + "/lib/xpp3-1.1.3.jar");
        touch(dirPath + "/lib/jdom-1.0.jar");
        mkdir(dirPath + "/lib/lib-jide-1.9");

        mkdir(dirPath + "/modules");
        touch(dirPath + "/modules/snap-ceres-core-0.5.jar");
        touch(dirPath + "/modules/snap-ceres-ui-0.5.jar");
        touch(dirPath + "/modules/beam-core-4.0.jar");
        touch(dirPath + "/modules/beam-ui-4.0.jar");
        mkdir(dirPath + "/modules/lib-netcdf");
        mkdir(dirPath + "/modules/lib-netcdf/lib");
        touch(dirPath + "/modules/lib-netcdf/lib/nc-core.jar");
        mkdir(dirPath + "/modules/lib-hdf");
        mkdir(dirPath + "/modules/lib-hdf/lib");
        touch(dirPath + "/modules/lib-hdf/lib/jhdf.jar");
        mkdir(dirPath + "/modules/lib-hdf/lib/unix");
        touch(dirPath + "/modules/lib-hdf/lib/unix/libhdf.so");
    }

    @Test
    protected void testConfigPaths(RuntimeConfig config) throws RuntimeConfigException {

        String configFilename= contextId + ".config";

        testPath(getBaseDirPath() + "/" + dirPath, config.getHomeDirPath());
        testPath(getBaseDirPath() + "/" + dirPath + "/config/" + configFilename, config.getConfigFilePath());
        testPath(getBaseDirPath() + "/" + dirPath + "/modules", config.getModulesDirPath());

        String[] libDirPaths = config.getLibDirPaths();
        assertNotNull(libDirPaths);
        assertEquals(1, libDirPaths.length);
        testPath(getBaseDirPath() + "/" + dirPath + "/lib", libDirPaths[0]);
    }

    protected static void testPath(String expectedPath, String actualPath) {
        assertNotNull(actualPath);
        try {
            assertEquals(new File(expectedPath).getCanonicalPath(), new File(actualPath).getCanonicalPath());
        } catch (IOException e) {
            fail("Cannot compare pathes: " + e.getMessage());
        }
    }

    protected void mkdir(String dirPath) throws IOException {
        mkdir0(new File(baseDirPath, dirPath));
    }

    protected void touch(String filePath) throws IOException {
        touch(filePath, new byte[0]);
    }

    protected void touch(String filePath, byte[] data) throws IOException {
        touch0(new File(baseDirPath, filePath), data);
    }

    protected URL toMainURL(String filePath) throws IOException {
        return new File(filePath).toURI().toURL();
    }

    protected URL toDefaultURL(String filePath) throws IOException {
        final File file = new File(filePath);
        if (file.isAbsolute()) {
            return file.toURI().toURL();
        } else {
            return new File(getBaseDirPath(), file.getPath()).toURI().toURL();
        }
    }

    private void mkdir0(File dir) throws IOException {
        if (dir.mkdir()) {
            if (!dir.isDirectory()) {
                throw new IOException("failed to create " + dir);
            }
            addFile(dir);
        }
    }

    private void touch0(File file, byte[] data) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        stream.write(data);
        stream.close();
        addFile(file);
    }

    private void addFile(File file) {
        fileStack.add(file);
        // System.out.println("added file to stack: " + file);
    }

    private void deleteFileStack() {
        // delete all files created, but not first 'target'
        for (int i = fileStack.size() - 1; i > 0; --i) {
            File file = fileStack.get(i);
            boolean b = file.delete();
            if (!b) {
                System.err.println("failed to delete " + file);
            }
        }
        fileStack.clear();
    }
}
