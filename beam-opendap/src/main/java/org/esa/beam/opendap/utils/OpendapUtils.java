/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.utils;

import org.esa.beam.opendap.datamodel.OpendapLeaf;
import thredds.catalog.InvDatasetImpl;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Contains some methods useful when dealing with OPeNDAP servers.
 *
 * @author Thomas Storm
 */
public class OpendapUtils {


    public static long getDataSizeInBytes(OpendapLeaf leaf) {
        return (long) ((InvDatasetImpl) leaf.getDataset()).getLocalMetadata().getDataSize();
    }

    public static String getResponse(String fileUri) throws IOException {
        HTTPSession session = new HTTPSession();
        final HTTPMethod httpMethod = HTTPMethod.Get(session, fileUri);
        httpMethod.execute();
        InputStream responseStream = httpMethod.getResponseAsStream();
        return convertStreamToString(responseStream, httpMethod.getCharSet());
    }

    public static String format(double value) {
        DecimalFormat format = new DecimalFormat("0.00");
        DecimalFormatSymbols instance = DecimalFormatSymbols.getInstance();
        instance.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(instance);
        return format.format(value);
    }

    private static String convertStreamToString(InputStream is, String charSet) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, charSet));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}
