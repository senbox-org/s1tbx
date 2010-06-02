/*
 * HtmlModuleGenerator.java
 *
 * Created on 19. April 2007, 10:08
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.site.util.CsvReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generate a HTML view of a module repository. This is only a fragment, not a
 * complete page.
 *
 * @see PageDecoratorGenerator for information on how to create a complete HTML page
 */
public class HtmlModuleGenerator implements HtmlGenerator {

    private static final String INCLUSION_LIST_FILENAME = "plugins_list.csv";

    /**
     * Creates a new instance of HtmlModuleGenerator
     */
    public HtmlModuleGenerator() {
    }

    public void generate(PrintWriter out, Module[] modules, String repositoryUrl) throws IOException {
        modules = removeDoubles( modules );
        for (Module module : modules) {
            if (!isIncluded(module, retrieveInclusionList(repositoryUrl))) {
                continue;
            }

            final String copyright = module.getCopyright();
            int year = Integer.parseInt(copyright.replaceAll("\\D", ""));

            // heading and download-link
            out.print("<h2>" + module.getName() + ", Version " );
            out.print("<a href=\"" + module.getLocation().toExternalForm() + "\">" + module.getVersion() + "</a>");
            out.println( "</h2>" );

            // description
            out.println( "<div class=\"description\">" );
            out.println( module.getDescription() );
            out.println( "</div>" );

            // footer
            out.print( "<div class=\"footer\">" );
            out.print( module.getVendor() );
            out.print( ", " );
            out.print( year );
            out.print( "&nbsp;|&nbsp;<a href=\"" + module.getLicenseUrl() + "\">Licence</a>" );
            out.print( "&nbsp;|&nbsp;<a href=\"#top\">top</a>" );
            out.println( "</div>" );
        }
    }

    Module[] removeDoubles(Module[] modules) {

        //TODO ts remove remove list

        final ArrayList<Module> removeList = new ArrayList<Module>();
        for (Module module : modules ) {
            final String symbolicName = module.getSymbolicName();
            final List<Module> list = Arrays.asList(modules);
            for( Module testModule : list ) {
                if( testModule.getSymbolicName().equals( symbolicName ) ) {
                    final boolean isHigherVersion = module.getVersion().compareTo(testModule.getVersion()) == -1;
                    if( isHigherVersion ) {
                        removeList.add( module );
                    }
                }
            }
        }
        final ArrayList<Module> resultList = new ArrayList<Module>();
        for (Module module : modules) {
            if( !removeList.contains( module ) ) {
                resultList.add( module );
            }
        }
        return resultList.toArray( new Module[ resultList.size() ] );
    }

    private File retrieveInclusionList(String repositoryUrl) {
        String sep = "/";
        if (repositoryUrl.endsWith(sep)) {
            sep = "";
        }
        return new File(repositoryUrl + sep + INCLUSION_LIST_FILENAME);
    }

    boolean isIncluded(Module module, File inclusionList) {
        try {
            final InputStream stream = new FileInputStream(inclusionList);
            final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{','});
            final String[] allowedModules = csvReader.readRecord();
            return Arrays.asList(allowedModules).contains(module.getSymbolicName());
        } catch (IOException e) {
            return true;
        }
    }
}
