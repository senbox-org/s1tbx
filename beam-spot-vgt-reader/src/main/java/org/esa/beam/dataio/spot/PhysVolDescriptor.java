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

package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

final class PhysVolDescriptor {
    private final PropertySet propertySet;
    private final int physVolNumber;
    private final String productId;
    private final String formatReference;
    private final String logVolDirName;
    private String logVolDescrFileName;

    public static PhysVolDescriptor create(File file) throws IOException {
        FileReader reader = new FileReader(file);
        try {
            return new PhysVolDescriptor(reader);
        } finally {
            reader.close();
        }
    }

    public PhysVolDescriptor(Reader reader) throws IOException {
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(reader);

        String value = getValue("PHYS_VOL_NUMBER");
        if (value != null) {
            try {
                physVolNumber = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid SPOT VGT volume descriptor. Missing value for 'PHYS_VOL_NUMBER'.");
            }
        } else {
            throw new IOException("Invalid SPOT VGT volume descriptor. Missing value for 'PHYS_VOL_NUMBER'.");
        }
        logVolDirName = String.format("%04d", physVolNumber);
        logVolDescrFileName = String.format("%s/%s_LOG.TXT", logVolDirName, logVolDirName);
        productId = getValue(String.format("PRODUCT_#%s_ID", logVolDirName));
        formatReference = getValue("FORMAT_REFERENCE");

    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public String getValue(String key) {
        return propertySet.getValue(key);
    }

    public int getPhysVolNumber() {
        return physVolNumber;
    }

    public String getLogVolDirName() {
        return logVolDirName;
    }

    public String getLogVolDescriptorFileName() {
        return logVolDescrFileName;
    }

    public String getProductId() {
        return productId;
    }

    public String getFormatReference() {
        return formatReference;
    }

}
