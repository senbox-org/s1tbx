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
