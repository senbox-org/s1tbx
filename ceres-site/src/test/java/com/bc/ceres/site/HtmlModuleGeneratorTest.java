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

package com.bc.ceres.site;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.internal.RepositoryScanner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HtmlModuleGeneratorTest {

    private HtmlModuleGenerator htmlModuleGenerator;
    private static Logger log = Logger.getLogger(SiteCreator.class.getName());

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, CoreException, MalformedURLException {
        htmlModuleGenerator = new HtmlModuleGenerator();
    }

    @Ignore
    @Test
    public void testParsing() throws IOException, CoreException, URISyntaxException, SAXException,
                                     ParserConfigurationException {

        final String someResource = getClass().getResource("dummy_resource").getFile();
        final String resourceDir = new File(someResource).getParent();

        URL repositoryUrl = new URL( "http://www.brockmann-consult.de/beam/software/repositories/4.7/" );

        final File dest = new File(resourceDir + File.separator + "testGeneration.html");
        PrintWriter pw = new PrintWriter(dest);

        log.setLevel( Level.OFF );
        RepositoryScanner moduleScanner = new RepositoryScanner(log, repositoryUrl, ProxyConfig.NULL);
        Module[] modules = moduleScanner.scan(ProgressMonitor.NULL);

        HtmlGenerator generator = new PageDecoratorGenerator(
                new MultiplePassGenerator(new HtmlGenerator[]{htmlModuleGenerator}));
        generator.generate(pw, modules, repositoryUrl.toString());
        pw.close();
    }
    
}
