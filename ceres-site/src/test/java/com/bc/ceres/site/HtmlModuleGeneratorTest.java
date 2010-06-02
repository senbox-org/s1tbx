package com.bc.ceres.site;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 02.06.2010
 * Time: 11:13:25
 */
public class HtmlModuleGeneratorTest {

    @Test
    public void testIsIncluded() throws CoreException, URISyntaxException, IOException {
        final HtmlModuleGenerator htmlModuleGenerator = new HtmlModuleGenerator();

        final File inclusionList = new File( getClass().getResource( "test_inclusion_list.csv").toURI().getPath() );

        String xml1 = getClass().getResource( "test_glayer_module.xml" ).toURI().getPath();
        FileReader fileReader1 = new FileReader(xml1);
        Module module1 = new ModuleManifestParser().parse( fileReader1 );

        String xml2 = getClass().getResource( "test_jai_module.xml" ).toURI().getPath();
        FileReader fileReader2 = new FileReader(xml2);
        Module module2 = new ModuleManifestParser().parse( fileReader2 );

        String xml3 = getClass().getResource( "test_excluded_module.xml" ).toURI().getPath();
        FileReader fileReader3 = new FileReader(xml3);
        Module module3 = new ModuleManifestParser().parse( fileReader3 );

        assertEquals( true, htmlModuleGenerator.isIncluded( module1, inclusionList ) );
        assertEquals( true, htmlModuleGenerator.isIncluded( module2, inclusionList ) );
        assertEquals( false, htmlModuleGenerator.isIncluded( module3, inclusionList ) );
    }


}
