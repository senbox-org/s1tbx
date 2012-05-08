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

package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// todo - use template engine, e.g. apache velocity (nf)
public class OperatorHandlerHtml implements OperatorHandler {

    ArrayList<OperatorDesc> operatorDescs;
    private File baseDir;

    public OperatorHandlerHtml() {
        operatorDescs = new ArrayList<OperatorDesc>();
    }

    @Override
    public void start(RootDoc root) throws IOException, URISyntaxException {
        final URL location = OperatorHandlerHtml.class.getProtectionDomain().getCodeSource().getLocation();
        baseDir = new File(new File(location.toURI()), "doc/help/gpf");
        System.out.println("Output goes to " + baseDir);
        if (!baseDir.isDirectory()) {
            if (!baseDir.mkdirs()) {
                throw new IOException("Failed to create base directory " + baseDir);
            }
        }
    }

    @Override
    public void stop(RootDoc root) throws IOException {
        Collections.sort(operatorDescs, new Comparator<OperatorDesc>() {
            @Override
            public int compare(OperatorDesc od1, OperatorDesc od2) {
                return od1.getName().compareTo(od2.getName());
            }
        });

        File indexFile = new File(baseDir, "OperatorIndex.html");
        PrintWriter writer = new PrintWriter(new FileWriter(indexFile));
        try {
            writeIndex(writer);
        } finally {
            writer.close();
        }
    }

    @Override
    public void processOperator(OperatorDesc operatorDesc) throws IOException {
        File file = getOperatorPageFile(operatorDesc);
        if (file.exists()) {
            System.out.println("Warning: File exists and will be overwritten: " + file);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            writeOperatorPage(operatorDesc, writer);
            operatorDescs.add(operatorDesc);
        } finally {
            writer.close();
        }
    }

    private void writeIndex(PrintWriter writer) throws IOException {
        writeHeader("GPF Operator Index", writer);
        writer.println("<h1>GPF Operator Index</h1>");
        writer.println("<table>");
        for (OperatorDesc operatorDesc : operatorDescs) {
            writer.println("  <tr>");
            writer.println("    <td><b><code><a href=\"" + getOperatorPageName(operatorDesc) + "\">" + operatorDesc.getName() + "</a></code></b></td>");
            writer.println("    <td>" + operatorDesc.getShortDescription() + "</td>");
            writer.println("  </tr>");
        }
        writer.println("</table>");
        writeFooter(writer);
    }

    private static void writeOperatorPage(OperatorDesc operatorDesc, PrintWriter writer) {
        writeHeader(operatorDesc.getName() + " Operator", writer);

        writer.println("<h1>" + operatorDesc.getName() + " Operator Description</h1>");

        writer.println("<h2>Overview</h2>");
        writer.println("<table>");
        writer.println("  <tr><td><b>Name:</b></td><td><code>" + operatorDesc.getName() + "</code></td></tr>");
        writer.println("  <tr><td><b>Full name:</b></td><td><code>" + operatorDesc.getType().getName() + "</code></td></tr>");
        writer.println("  <tr><td><b>Purpose:</b></td><td>" + makeHtmlConform(operatorDesc.getShortDescription()) + "</td></tr>");
        writer.println("  <tr><td><b>Version:</b></td><td>" + operatorDesc.getVersion() + "</td></tr>");
        writer.println("</table>");

        writer.println("<h2>Description</h2>");
        String description = operatorDesc.getLongDescription();
        if (!description.isEmpty()) {
            writer.println(description);
        } else {
            writer.println("<i>No description available.</i>");
        }

        writer.println("<h2>Sources</h2>");
        SourceProductDesc[] sourceProducts = operatorDesc.getSourceProductList();
        SourceProductsDesc sourceProductsField = operatorDesc.getSourceProducts();
        if (sourceProducts.length > 0 || sourceProductsField != null) {
            writer.println("<table>");
            writer.println("<tr>");
            writer.println("  <th>Name</th>");
            writer.println("  <th>Description</th>");
            writer.println("</tr>");
            for (SourceProductDesc sourceProduct : sourceProducts) {
                writer.println("<tr>");
                writer.println("  <td><code>" + sourceProduct.getName() + "</code></td>");
                writer.println("  <td>" + getFullDescription(sourceProduct) + "</td>");
                writer.println("</tr>");
            }
            if(sourceProductsField != null) {
                writer.println("<tr>");
                writer.println("  <td><code>" + sourceProductsField.getName() + "</code></td>");
                writer.println("  <td>" + getFullDescription(sourceProductsField) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
        } else {
            writer.println("<p><i>This operator does not have any sources.</i></p>");
        }

        writer.println("<h2>Parameters</h2>");
        ParameterDesc[] parameterDescs = operatorDesc.getParameters();
        if (parameterDescs.length > 0) {
            writer.println("<table>");
            writer.println("<tr>");
            writer.println("  <th>Name</th>");
            writer.println("  <th>Data Type</th>");
            writer.println("  <th>Default</th>");
            writer.println("  <th>Description</th>");
            writer.println("  <th>Constraints</th>");
            writer.println("</tr>");
            for (ParameterDesc parameterDesc : parameterDescs) {
                writer.println("<tr>");
                writer.println("  <td><code>" + parameterDesc.getName() + "</code></td>");
                writer.println("  <td><code>" + parameterDesc.getType().getSimpleName() + "</code></td>");
                writer.println("  <td><code>" + parameterDesc.getDefaultValue() + "</code></td>");
                writer.println("  <td>" + getFullDescription(parameterDesc) + "</td>");
                writer.println("  <td>" + parameterDesc.getConstraints() + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
        } else {
            writer.println("<p><i>This operator does not have any parameters.</i></p>");
        }

        // todo - fix this (nf)
//        writer.println("<h2>Usage</h2>");
//        writer.println("<p><i>TODO</i></p>");

        writeFooter(writer);
    }

    private static String makeHtmlConform(String text) {
        return text.replace("\n", "<br/>");
    }

    private static void writeHeader(String title, PrintWriter writer) {
        writer.println("<!DOCTYPE HTML PUBLIC " +
                "\"-//W3C//DTD HTML 4.01//EN\" " +
                "\"http://www.w3.org/TR/html4/strict.dtd\">");
        writer.println("<html>");
        writer.println("" +
                "<head>\n" +
                "    <title>"+title+"</title>\n" +
                "    <link rel=\"stylesheet\" href=\"../style.css\">\n" +
                "</head>");
        writer.println("<body>");

        writer.println("" +
                "<table class=\"header\">\n" +
                "    <tr class=\"header\">\n" +
                "        <td class=\"header\">&nbsp;"+title+"</td>\n" +
                "        <td class=\"header\" align=\"right\">\n" +
                "           <a href=\"../general/BeamOverview.html\">" +
                "             <img src=\"images/BeamHeader.jpg\" border=\"0\"/></a>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "</table>");
    }

    private static void writeFooter(PrintWriter writer) {
        writer.println("<hr/>");
        writer.println("</body>");
        writer.println("</html>");
    }

    private static String getFullDescription(ElementDesc elementDesc) {
        String shortDescription = elementDesc.getShortDescription();
        String longDescription = elementDesc.getLongDescription();
        if (shortDescription.isEmpty()) {
            return longDescription;
        }
        if (longDescription.isEmpty()) {
            return shortDescription;
        }
        return shortDescription + "<br/>" + longDescription;
    }

    private File getOperatorPageFile(OperatorDesc operatorDesc) {
        return new File(baseDir, getOperatorPageName(operatorDesc));
    }

    private static String getOperatorPageName(OperatorDesc operatorDesc) {
        return operatorDesc.getType().getName().replace('.', '_') + ".html";
    }

}