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

package org.esa.beam.dataio.avhrr.binio;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.util.DataPrinter;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 29.11.2010
 * Time: 17:26:15
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String filename = args[0];
        System.out.println("filename = " + filename);
        DataFormat dataFormat = new DataFormat(NoaaTypes.ALL,  ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(new File(filename), "r");
        CompoundData data = context.getData();
        HeaderWrapper wrapper = new HeaderWrapper(data);
        MetadataElement rootElem = wrapper.getAsMetadataElement();
        print(rootElem, "");
//        DataPrinter dp = new DataPrinter(System.out, false);
//        dp.print(data);
//        System.out.println("data = " + data);
    }

    private static void print(MetadataElement metadataElement, String indent) {
        System.out.println(indent + "-------------------");
        System.out.println(indent + "Element = " + metadataElement.getName());
        String description = metadataElement.getDescription();
        if (description != null && !description.isEmpty()) {
            System.out.print(" | description = " + description);
        }
        MetadataAttribute[] metadataAttributes = metadataElement.getAttributes();
        for (MetadataAttribute metadataAttribute : metadataAttributes) {
            print(metadataAttribute, indent);
        }
        MetadataElement[] metadataElements = metadataElement.getElements();
        for (MetadataElement element : metadataElements) {
            print(element, indent + "  ");
        }

    }

    private static void print(MetadataAttribute attribute, String indent) {
        String name = attribute.getName();
        System.out.print(indent + "name = " + name);
        String description = attribute.getDescription();
        if (description != null && !description.isEmpty()) {
            System.out.print(" | description = " + description);
        }
        ProductData data = attribute.getData();
        System.out.print(" | data = " + data);
        System.out.println();
    }
}
