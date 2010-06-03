/*
 * HtmlTocGenerator.java
 * Generates a table of content
 * Created on 19. April 2007, 10:08
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generate a HTML view of a module repositories Table Of Content.
 * This is only a fragment, not a complete page.
 * @see PageDecoratorGenerator for information on how to create a complete HTML page
 */
public class HtmlTocGenerator implements HtmlGenerator {

    /** Creates a new instance of HtmlTocGenerator */
    public HtmlTocGenerator() {
    }

    public void generate(PrintWriter out, Module[] modules, String repositoryUrl) throws IOException {
        String version = SiteCreator.retrieveVersion( repositoryUrl ); 
        out.println("<a name=\"top\"></a>");
        out.println("<h1>BEAM " + version + ".x modules</h1>");
//        printTocTable(out, modules);
    }
}
