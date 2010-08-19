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

    @Override
    public void generate(PrintWriter out, Module[] modules, String version) throws IOException {
        if (modules == null) {
            return;
        }
        for (int i = 0, modulesLength = modules.length; i < modulesLength; i++) {
            Module module = modules[i];
            writeHeader(out, module);

            // description
            out.println("<div class=\"description\">");
            out.println(module.getDescription());
            out.println("</div>");

            // dependencies
            final Dependency[] dependencies = module.getDeclaredDependencies();
            if (dependencies != null && dependencies.length > 0) {

                out.println("<p>");
                out.println(
                        "<a href=\"JavaScript:doMenu('main" + i + "');\" id=\"xmain" + i + "\">[+]</a> Depends on:<br>");
                out.print("<div id=\"main" + i + "\" style=\"display:none\">");
//                out.println("Depends on:");
                out.println("<ul>");

                for (Dependency dependency : dependencies) {
                    writeDependency(out, modules, dependency);
                }
                out.println("</ul>");
                out.println("</div>");

                out.println("</p>");
            }
            writeFooter(out, module);
        }
    }

    private void writeDependency(PrintWriter out, Module[] modules, Dependency dependency) {
        out.print("  <li>");
        final String symbolicName = dependency.getModuleSymbolicName();
        final String readableName = ModuleUtils.symbolicToReadableName(symbolicName, modules);
//                final boolean dependencyIncl = !ModuleUtils.isExcluded(symbolicName, exclusionList);
//                if (dependencyIncl) {
//                    out.print("<a href=\"#" + readableName.replaceAll(" ", "") + "\">");
//                }
        String depVersion = dependency.getVersion();
        depVersion = (depVersion != null) ? depVersion : "";
        out.print(readableName + " " + depVersion);
//                if (dependencyIncl) {
//                    out.print("</a>");
//                }
        out.println("</li>");
    }

    private void writeHeader(PrintWriter out, Module module) {
        final String size = ModuleUtils.retrieveSize(module);
        final String moduleName = module.getName();

        out.print("<h3 class=\"heading\">" + moduleName + " ");
        out.println("<a name= " + moduleName.replaceAll(" ", "") + ">");
        out.print("<a href=\"" + module.getLocation().toExternalForm() + "\">");
        out.print(module.getVersion());
        if (size != null) {
            out.println("&nbsp;(" + size + ")");
        }
        out.print("</a>");
        out.println("</h3>");
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
        out.println("</div>");

        out.println("<br/>");
    }
}
