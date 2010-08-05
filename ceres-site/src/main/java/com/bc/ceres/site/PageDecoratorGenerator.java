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
 * PageDecoratorGenerator.java
 *
 * Created on 19. April 2007, 10:07
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Decorate a Module-HTML in order to produce a complete HTML page.
 * The page should be styled with an accompanying css
 */
public class PageDecoratorGenerator implements HtmlGenerator {

    private HtmlGenerator chain;

    /**
     * Creates a new instance of PageDecoratorGenerator
     */
    public PageDecoratorGenerator(HtmlGenerator chain) {
        this.chain = chain;
    }

    public void generate(PrintWriter out, Module[] modules, String version) throws IOException {
        out.println("<html><head><title>Modules</title>\n");
        out.println(generateStyleTag());
        out.println(generateJavaScript());
        out.println("</head>\n");
        out.println("<body>\n");

        this.chain.generate(out, modules, version);

        out.println("</body></html>");
        out.flush();
    }

    private String generateJavaScript() {

        StringBuilder builder = new StringBuilder();
        builder.append("<script language=\"JavaScript\" type=\"text/javascript\">\n");
        builder.append("function doMenu(item) {\n");
        builder.append("obj=document.getElementById(item);\n");
        builder.append("col=document.getElementById(\"x\" + item);\n");
        builder.append("if (obj.style.display==\"none\") {\n");
        builder.append("obj.style.display=\"block\";\n");
        builder.append("col.innerHTML=\"[-]\";\n");
        builder.append("}");
        builder.append("else {\n");
        builder.append("obj.style.display=\"none\";\n");
        builder.append("col.innerHTML=\"[+]\";\n");
        builder.append("}");
        builder.append("}");
        builder.append("</script>\n");

        return builder.toString();
    }

    private String generateStyleTag() throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append("<style type=\"text/css\"> ");
        final URL modulesCss = getClass().getResource("modules.css");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(modulesCss.toURI().getPath()));
        } catch (URISyntaxException e) {
        }
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        builder.append("</style>");
        reader.close();
        return builder.toString();
    }

}
