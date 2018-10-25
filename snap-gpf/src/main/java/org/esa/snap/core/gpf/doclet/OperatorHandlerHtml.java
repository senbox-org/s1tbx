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

package org.esa.snap.core.gpf.doclet;

import com.sun.javadoc.RootDoc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

// todo - use template engine, e.g. apache velocity (nf)
class OperatorHandlerHtml implements OperatorHandler {

    private final Path outputDir;
    private ArrayList<OperatorDesc> operatorDescs;
    private Path baseDir;

    OperatorHandlerHtml(Path outputDir) {
        this.outputDir = outputDir;
        operatorDescs = new ArrayList<>();
    }

    @Override
    public void start(RootDoc root) throws IOException, URISyntaxException {
        baseDir = outputDir.resolve("org/esa/snap/core/gpf/docs/gpf");
        System.out.println("Output goes to " + baseDir);
        if (!Files.isDirectory(baseDir)) {
            Files.createDirectories(baseDir);
        }
    }

    @Override
    public void stop(RootDoc root) throws IOException {
        Collections.sort(operatorDescs, (od1, od2) -> od1.getName().compareTo(od2.getName()));

        Path indexFile = baseDir.resolve("OperatorIndex.html");
        try (PrintWriter writer = new PrintWriter(new FileWriter(indexFile.toFile()))) {
            writeIndex(writer);
        }
    }

    @Override
    public void processOperator(OperatorDesc operatorDesc) throws IOException {
        Path pageFile = getOperatorPageFile(operatorDesc);
        if (Files.exists(pageFile)) {
            System.out.println("Warning: File exists and will be overwritten: " + pageFile);
        }
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(pageFile))) {
            writeOperatorPage(operatorDesc, writer);
            operatorDescs.add(operatorDesc);
        }
    }

    private void writeIndex(PrintWriter writer) throws IOException {
        writeHeader("GPF Operator Index", writer);
        writer.println("<h1>GPF Operator Index</h1>");
        writer.println("<table>");
        for (OperatorDesc operatorDesc : operatorDescs) {
            writer.println("  <tr>");
            writer.println(
                    "    <td><b><code><a href=\"" + getOperatorPageName(operatorDesc) + "\">" + operatorDesc.getName() + "</a></code></b></td>");
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
            if (sourceProductsField != null) {
                writer.println("<tr>");
                writer.println("  <td><code>" + sourceProductsField.getName() + "</code></td>");
                writer.println("  <td>" + getFullDescription(sourceProductsField) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
        } else {
            writer.println("<p><i>This operator does not have any sources.</i>");
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
            writer.println("<p><i>This operator does not have any parameters.</i>");
        }

        // todo - fix this (nf)
//        writer.println("<h2>Usage</h2>");
//        writer.println("<p><i>TODO</i>");

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
                       "    <title>" + title + "</title>\n" +
                       "    <link rel=\"stylesheet\" href=\"../style.css\">\n" +
                       "</head>");
        writer.println("<body>");

        writer.println("" +
                       "<table class=\"header\">\n" +
                       "    <tr class=\"header\">\n" +
                       "        <td class=\"header\">&nbsp;" + title + "</td>\n" +
                       "        <td class=\"header\" align=\"right\">\n" +
                       "          <a href=\"../general/overview/SnapOverview.html\">" +
                       "             <img src=\"images/snap_header.jpg\" border=\"0\"/></a>\n" +
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

    private Path getOperatorPageFile(OperatorDesc operatorDesc) {
        return baseDir.resolve(getOperatorPageName(operatorDesc));
    }

    private static String getOperatorPageName(OperatorDesc operatorDesc) {
        return operatorDesc.getType().getName().replace('.', '_') + ".html";
    }

}
