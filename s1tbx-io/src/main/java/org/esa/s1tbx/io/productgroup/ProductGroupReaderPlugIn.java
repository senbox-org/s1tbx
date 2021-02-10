/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.productgroup;

import org.esa.s1tbx.io.productgroup.support.ProductGroupMetadataFile;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.dataio.envi.EnviProductReaderPlugIn;

import java.io.File;
import java.util.Locale;

public class ProductGroupReaderPlugIn extends EnviProductReaderPlugIn {

    public static final String FORMAT_NAME = "ProductGroup";

    @Override
    public ProductReader createReaderInstance() {
        return new ProductGroupReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String getDescription(Locale locale) {
        return "ProductGroup";
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof File) {
            final File selectedFile = (File) input;
            final String fileName = selectedFile.getName().toLowerCase();
            if (fileName.equals(ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE)) {
                return DecodeQualification.INTENDED;
            } else if(selectedFile.isDirectory()) {
                if(findMetadataFile(selectedFile) != null) {
                    return DecodeQualification.INTENDED;
                }
            }
        }

        return DecodeQualification.UNABLE;
    }

    public static File findMetadataFile(final File folder) {
        final File[] files = folder.listFiles();
        if(files != null) {
            for(File f : files) {
                if(f.getName().equals(ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE)) {
                    return f;
                }
            }
        }
        return null;
    }
}
