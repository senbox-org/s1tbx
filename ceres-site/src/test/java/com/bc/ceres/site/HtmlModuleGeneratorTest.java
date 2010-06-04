package com.bc.ceres.site;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.internal.RepositoryScanner;
import org.junit.Before;
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

/**
 * User: Thomas Storm
 * Date: 02.06.2010
 * Time: 11:13:25
 */
public class HtmlModuleGeneratorTest {

    private HtmlModuleGenerator htmlModuleGenerator;
    private static Logger log = Logger.getLogger(SiteCreator.class.getName());

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, CoreException, MalformedURLException {
        htmlModuleGenerator = new HtmlModuleGenerator();
    }

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
