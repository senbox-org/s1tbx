package com.bc.ceres.site;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 02.06.2010
 * Time: 11:13:25
 */
public class HtmlModuleGeneratorTest {

    private List<Module> modules = new ArrayList<Module>();
    private HtmlModuleGenerator htmlModuleGenerator;

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, CoreException, MalformedURLException {
        ModuleImpl module1 = generateModule( "test_excluded_module.xml" );
        ModuleImpl module2 = generateModule( "test_glayer_module.xml" );
        ModuleImpl module3 = generateModule( "test_jai_module.xml" );

        modules.add(module1);
        modules.add(module2);
        modules.add(module3);

        htmlModuleGenerator = new HtmlModuleGenerator();
    }

    @Test
    public void testParsing() throws IOException, CoreException, URISyntaxException {

        final String someResource = getClass().getResource("test_glayer_module.xml").getFile();
        final String resourceDir = new File(someResource).getParent();
        final URL repositoryUrl = new File(resourceDir).toURI().toURL();

        final File dest = new File(resourceDir + File.separator + "testGeneration.html");
        PrintWriter pw = new PrintWriter(dest);

        modules.add( generateModule( "test_jai_module2.xml" ) );

        HtmlGenerator generator = new PageDecoratorGenerator(
                new MultiplePassGenerator(new HtmlGenerator[]{htmlModuleGenerator}));
        generator.generate(pw, modules.toArray(new Module[modules.size()]), repositoryUrl.toString());
        pw.close();
    }
    
    private ModuleImpl generateModule(String resource) throws URISyntaxException, FileNotFoundException, CoreException,
                                                              MalformedURLException {
        final URI uri = getClass().getResource(resource).toURI();
        String xml = uri.getPath();
        FileReader fileReader = new FileReader(xml);
        ModuleImpl module = new ModuleManifestParser().parse(fileReader);
        module.setLocation(uri.toURL());
        return module;
    }
}
