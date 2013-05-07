/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.reports;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.datamodel.AbstractMetadata;

/**
 * Report to show metadata
 */
public class MetadataReport implements Report {

    private final MetadataElement absRoot;

    public MetadataReport(final Product product) {
        absRoot = AbstractMetadata.getAbstractedMetadata(product);
    }

    public String getAsHTML() {

        final String pattern = "<html>" +
                "<body bgcolor=\"#FFFFFF\">"+
                "<b>Product Metadata</b><br><br>" +
                getHtmlMetadataTable(absRoot) +
                "</body></html>";
        return pattern; /*I18N*/
    }

    private static String getHtmlMetadataTable(final MetadataElement elem) {

        final StringBuilder tableStr = new StringBuilder("<table border=1>");

        tableStr.append("<tr><th>Name</th><th>Value</th><th>Unit</th></tr>");

        final MetadataAttribute[] attribs = elem.getAttributes();
        for(MetadataAttribute attrib : attribs) {
            tableStr.append("<tr>");
            tableStr.append("<td><b>"+attrib.getName()+"</b></td>");
            tableStr.append("<td>"+attrib.getData().toString()+"</td>");
            tableStr.append("<td>"+attrib.getUnit()+"</td>");
            tableStr.append("</tr>");
        }
        tableStr.append("</tr>");
        tableStr.append("</table>");

        return tableStr.toString();
    }

}
