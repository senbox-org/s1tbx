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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Unit test for simple ModuleReader.
 */
public class RepositoryScannerTest {

    Logger NO_LOGGER = Logger.getAnonymousLogger();
    ProgressMonitor NO_PM = ProgressMonitor.NULL;
    ProxyConfig NO_PROXY = ProxyConfig.NULL;

    @Ignore
    public void testNullArgConvention() throws IOException, CoreException {
        URL NO_URL = new File("").getAbsoluteFile().toURI().toURL();

        try {
            new RepositoryScanner(null, NO_URL, NO_PROXY);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new RepositoryScanner(NO_LOGGER, null, NO_PROXY);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, null);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, NO_PROXY).scan(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    @Ignore
    public void testRepository() throws IOException, CoreException {
        File repositoryDir = Config.getRepositoryDir();

        RepositoryScanner rs = new RepositoryScanner(NO_LOGGER, repositoryDir.toURI().toURL(), NO_PROXY);
        Module[] repositoryModules = rs.scan(NO_PM);
        assertEquals(5, repositoryModules.length);

        Module rm;
        rm = findModule(repositoryModules, "module-a");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-b");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-c");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-d");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-e");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-f");
        assertNull(rm);

        rm = findModule(repositoryModules, "module-g");
        assertNull(rm);
    }

    @Ignore
    public void testDumpModuleNames() throws Exception {
        URL url = new URL("http://www.brockmann-consult.de/beam/software/repositories/4.10");

        final RepositoryScanner repositoryScanner = new RepositoryScanner(Logger.getAnonymousLogger(), url, ProxyConfig.NULL);

        final Module[] modules = repositoryScanner.scan(ProgressMonitor.NULL);
        for (Module module : modules) {
            System.out.println("module = " + module.getName());
        }
    }

    private Module findModule(Module[] modules, String name) {
        for (Module module : modules) {
            if (name.equals(module.getSymbolicName())) {
                return module;
            }
        }
        return null;
    }

}
