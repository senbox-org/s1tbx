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

/*
 * HtmlModuleGenerator.java
 *
 * Created on 19. April 2007, 10:08
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.site.util.ExclusionListBuilder;
import com.bc.ceres.site.util.ModuleUtils;

import java.io.File;
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

    public void generate(PrintWriter out, Module[] allModules, String repositoryUrl) throws IOException {
        File exclusionList = retrieveExclusionList(repositoryUrl);
        Module[] modules = ModuleUtils.cleanModules(allModules, exclusionList);
        for (Module module : modules) {
            writerHeader(out, module);

            // description
            final Dependency[] dependencies = module.getDeclaredDependencies();
            out.println("<div class=\"description\">");
            out.println(module.getDescription());
            out.println("</div>");
            out.println("<p>");

            out.print("<div class=\"dependencies\">");
            out.println("Depends on:");
            out.println("<ul>");
            for (Dependency dependency : dependencies) {
                out.print("  <li>");
                final String symbolicName = dependency.getModuleSymbolicName();
                final String readableName = ModuleUtils.symbolicToReadableName(symbolicName, allModules);
                final boolean dependencyIncl = !ModuleUtils.isExcluded(symbolicName, exclusionList);
                if (dependencyIncl) {
                    out.print("<a href=\"#" + readableName.replaceAll(" ", "") + "\">");
                }
                final String depVersion = dependency.getVersion();
                String version = (depVersion != null) ? depVersion : "";
                out.print(readableName + " " + version);
                if (dependencyIncl) {
                    out.print("</a>");
                }
                out.println("</li>");
            }
            out.println("</ul>");
            out.println("</div>");

            writeFooter(out, module);
        }
    }

    private void writerHeader(PrintWriter out, Module module) {
        final String size = ModuleUtils.retrieveSize(module);
        final String moduleName = module.getName();

        out.print("<h2 class=\"heading\">" + moduleName + " ");
        out.println("<a name= " + moduleName.replaceAll(" ", "") + ">");
        out.print("<a href=\"" + module.getLocation().toExternalForm() + "\">");
        out.print(module.getVersion());
        if (size != null) {
            out.println("&nbsp;(" + size + ")");
        }
        out.print("</a>");
        out.println("</h2>");
    }

    private void writeFooter(PrintWriter out, Module module) {
        final String year = ModuleUtils.retrieveYear(module);
        out.print("<div class=\"footer\">");
        final String contactUrl = module.getUrl();
        if (contactUrl != null) {
            out.print("<a href=\"" + contactUrl + "\">");
        }
        out.print(module.getVendor());
        if (contactUrl != null) {
            out.print("</a>");
        }
        out.print(!year.equals("-1") ? ", " + year : "");
        final String licenceUrl = module.getLicenseUrl();
        if (licenceUrl != null) {
            out.print("&nbsp;&#8226;&nbsp;<a href=\"" + licenceUrl + "\">Licence</a>");
        }
        out.print("&nbsp;&#8226;&nbsp;<a href=\"#top\">top</a>");
        out.println("</div>");

        out.println("<br/>");
    }

    private static File retrieveExclusionList(String repositoryUrl) {
        String sep = "";
        if (!repositoryUrl.endsWith("/")) {
            sep = "/";
        }
        return new File(repositoryUrl + sep + ExclusionListBuilder.EXCLUSION_LIST_FILENAME);
    }

}
