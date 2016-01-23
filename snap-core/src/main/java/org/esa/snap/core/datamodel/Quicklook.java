/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by luis on 10/01/2016.
 */
public class Quicklook extends ProductNode {

    private BufferedImage image = null;
    private final File browseFile;

    public Quicklook(final String name) {
        this(name, null);
    }

    public Quicklook(final String name, final File browseFile) {
        super(name);
        this.browseFile = browseFile;
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        return 0;
    }

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p>The method simply calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor, must not be <code>null</code>
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    public BufferedImage getImage() {
        if (image == null) {
            final QuicklookGenerator qlGen = new QuicklookGenerator();
            if (browseFile != null) {
                try {
                    final Product browseProduct = ProductIO.readProduct(browseFile);
                    image = qlGen.createQuickLookFromBrowseProduct(browseProduct, true);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to load quicklook: "+e.getMessage());
                }
            } else {

            }
        }
        return image;
    }
}
