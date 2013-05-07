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

import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.util.ResourceUtils;

/**
 * Report to show metadata
 */
public class SummaryReport implements Report {

    public SummaryReport(final Product product) {

    }

    public String getAsHTML() {

        final String ver = System.getProperty(ResourceUtils.getContextID()+".version");
        final String pattern =
                "<html>" +
                        "<b>NEST  Version "+ver+"</b>" +
                        "<br>(c) Copyright 2007-2013 by Array Systems Computing Inc. and contributors. All rights reserved." +

                        "</html>";
        return pattern; /*I18N*/
    }
}
