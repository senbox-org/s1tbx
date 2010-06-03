package com.bc.ceres.site.util;

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 03.06.2010
 * Time: 12:13:27
 */
public class ModuleUtilsTest {

    private List<Module> modules = new ArrayList<Module>();

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, CoreException, MalformedURLException {
        ModuleImpl module1 = generateModule("test_excluded_module.xml");
        ModuleImpl module2 = generateModule("test_glayer_module.xml");
        ModuleImpl module3 = generateModule("test_jai_module.xml");

        modules.add(module1);
        modules.add(module2);
        modules.add(module3);
    }

    @Test
    public void testPomParsing() throws Exception {
        final List<URL> poms = new ArrayList<URL>();
        poms.add(getClass().getResource("test_pom.xml"));
        File inclusionList = generateTestInclusionFile();
        InclusionListBuilder.parsePoms(inclusionList, poms);

        CsvReader csvReader = new CsvReader(new FileReader(inclusionList), new char[]{','});
        final String[] firstChunkOfModules = csvReader.readRecord();

        assertEquals( true, Arrays.asList( firstChunkOfModules ).contains( "ceres-launcher" ) );
        assertEquals( false, Arrays.asList( firstChunkOfModules ).contains( "beam-download-portlet" ) );

        int numberOfModules = firstChunkOfModules.length;
        assertEquals(9, numberOfModules);

        final URL pom2 = getClass().getResource("test_pom2.xml");
        InclusionListBuilder.addPomToInclusionList(inclusionList, pom2);

        csvReader = new CsvReader(new FileReader(inclusionList), new char[]{InclusionListBuilder.CSV_SEPARATOR});
        final String[] secondChunkOfModules = csvReader.readRecord();
        int newNumberOfModules = secondChunkOfModules.length;

        assertEquals( true, Arrays.asList( secondChunkOfModules ).contains( "ceres-launcher" ) );
        assertEquals( true, Arrays.asList( secondChunkOfModules ).contains( "beam-download-portlet" ) );
        assertEquals( 12, newNumberOfModules);
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

        assertEquals(3, moduleArray.length);
        assertEquals(moduleArray.length, resultModuleArray.length);

        moduleArray = addModule("test_jai_module2.xml");
        assertEquals(4, moduleArray.length);

        resultModuleArray = ModuleUtils.removeDoubles(moduleArray);
        assertEquals(moduleArray.length - 1, resultModuleArray.length);
    }

    private Module[] addModule(String name) throws URISyntaxException, FileNotFoundException, CoreException,
                                                   MalformedURLException {
        Module[] moduleArray;
        modules.add(generateModule(name));
        moduleArray = modules.toArray(new Module[modules.size()]);
        return moduleArray;
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

    private File generateTestInclusionFile() throws IOException {
        final String someResource = getClass().getResource("test_pom.xml").getFile();
        final String resourceDir = new File(someResource).getParent();
        final File inclusionList = new File(resourceDir + File.separator + "test_inclusion_list.csv");
        inclusionList.delete();
        inclusionList.createNewFile();
        return inclusionList;
    }

}
