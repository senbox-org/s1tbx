/*
 * HtmlModuleGenerator.java
 *
 * Created on 19. April 2007, 10:08
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.site.util.InclusionListBuilder;
import com.bc.ceres.site.util.ModuleUtils;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generate a HTML view of a module repository. This is only a fragment, not a
 * complete page.
 *
 * @see PageDecoratorGenerator for information on how to create a complete HTML page
 */
public class HtmlModuleGenerator implements HtmlGenerator {

    /**
     * Creates a new instance of HtmlModuleGenerator
     */
    public HtmlModuleGenerator() {
    }

    public void generate(PrintWriter out, Module[] modules, String repositoryUrl) throws IOException {
        modules = ModuleUtils.removeDoubles(modules);
        for (Module module : modules) {
            if (!ModuleUtils.isIncluded(module, InclusionListBuilder.retrieveInclusionList(repositoryUrl))) {
                continue;
            }

            String year = ModuleUtils.retrieveYear(module);

            // heading and download-link
            out.print("<h2 class=\"heading\">" + module.getName() + " ");
            out.print("<a href=\"" + module.getLocation().toExternalForm() + "\">");
            out.print(module.getVersion() + "</a>");
            out.println("</h2>");

            // description
            out.println("<div class=\"description\">");
            out.println(module.getDescription());
            out.println("</div>");

            out.println( "<p>" );

            // footer
            out.print("<div class=\"footer\">");
            final String contactUrl = module.getUrl();
            if( contactUrl != null ) {
                out.print( "<a href=\"" + contactUrl + "\">" );
            }
            out.print(module.getVendor());
            if( contactUrl != null ) {
                out.print( "</a>" );
            }
            out.print(!year.equals("-1") ? ", " + year : "");
            final String licenceUrl = module.getLicenseUrl();
            if( licenceUrl != null ) {
                out.print("&nbsp;&#8226;&nbsp;<a href=\"" + licenceUrl + "\">Licence</a>");
            }
            out.print("&nbsp;&#8226;&nbsp;<a href=\"#top\">top</a>");
            out.println("</div>");

            out.println( "<br/>" );
        }
    }

}
