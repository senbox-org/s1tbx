/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Writer par header
 */
public class HeaderWriter {

    private final File outputFile;
    private final Product srcProduct;
    private final MetadataElement absRoot;
    private boolean isComplex = false;
    private boolean isCoregistered = false;

    public HeaderWriter(final Product srcProduct, final File outputFile) {
        this.srcProduct = srcProduct;

        absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
        if(absRoot != null) {
            try {
                isComplex = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE).equals("COMPLEX");
                isCoregistered = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.coregistered_stack);
            } catch (Exception e) {
                SystemUtils.LOG.severe("Unable to read metadata "+e.getMessage());
            }
        }
        this.outputFile = createParFile(outputFile);
    }

    public void writeParFile() {
        PrintStream p = null;
        try {
            final FileOutputStream out = new FileOutputStream(outputFile);
            p = new PrintStream(out);

            p.println("Nrow");
            p.println(srcProduct.getSceneRasterHeight());
            p.println("---------");

            p.println("Ncol");
            p.println(srcProduct.getSceneRasterWidth());
            p.println("---------");

        } catch (Exception e) {
            System.out.println("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            if (p != null)
                p.close();
        }
    }

    private File createParFile(final File file) {
        String name = FileUtils.getFilenameWithoutExtension(file);
        String ext = FileUtils.getExtension(name);
        String newExt = GammaConstants.PAR_EXTENSION;
        if(ext != null && !ext.endsWith("slc")) {
            if(isComplex) {
                if(isCoregistered) {
                    newExt = ".rslc"+newExt;
                } else {
                    newExt = ".slc"+newExt;
                }
            }
        }
        name += newExt;

        return new File(file.getParent(), name);
    }
}
