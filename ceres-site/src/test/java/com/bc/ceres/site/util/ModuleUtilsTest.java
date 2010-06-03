package com.bc.ceres.site.util;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import com.bc.ceres.site.HtmlModuleGenerator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 03.06.2010
 * Time: 12:13:27
 */
public class ModuleUtilsTest {

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
    public void testIsIncluded() throws CoreException, URISyntaxException, IOException {

        final File inclusionList = new File(getClass().getResource("test_inclusion_list.csv").toURI().getPath());

        assertEquals(false, ModuleUtils.isIncluded(modules.get(0), inclusionList));
        assertEquals(true, ModuleUtils.isIncluded(modules.get(1), inclusionList));
        assertEquals(true, ModuleUtils.isIncluded(modules.get(2), inclusionList));
    }

    @Test
    public void testRemoveDoubles() throws Exception {
        Module[] moduleArray = modules.toArray(new Module[modules.size()]);
        Module[] resultModuleArray = ModuleUtils.removeDoubles(moduleArray);

        assertEquals( 3, moduleArray.length );
        assertEquals( moduleArray.length, resultModuleArray.length );

        moduleArray = addModule( "test_jai_module2.xml" );
        assertEquals( 4, moduleArray.length );

        resultModuleArray = ModuleUtils.removeDoubles(moduleArray);
        assertEquals( moduleArray.length - 1, resultModuleArray.length );
    }

    private Module[] addModule( String name ) throws URISyntaxException, FileNotFoundException, CoreException,
                                                     MalformedURLException {
        Module[] moduleArray;
        modules.add( generateModule( name ) );
        moduleArray = modules.toArray(new Module[modules.size()]);
        return moduleArray;
    }

    private ModuleImpl generateModule(String resource) throws URISyntaxException, FileNotFoundException, CoreException,
                                                              MalformedURLException {
        final URI uri1 = getClass().getResource(resource).toURI();
        String xml1 = uri1.getPath();
        FileReader fileReader1 = new FileReader(xml1);
        ModuleImpl module1 = new ModuleManifestParser().parse(fileReader1);
        module1.setLocation(uri1.toURL());
        return module1;
    }

}
