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
package org.esa.snap.core.datamodel.quicklooks;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductVisitor;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Created by luis on 10/01/2016.
 */
public class Quicklook extends ProductNode {

    public final static String DEFAULT_QUICKLOOK_NAME = "Quicklook";
    public final static String SNAP_QUICKLOOK_FILE_PREFIX = "snapQL_";
    private final static String QUICKLOOK_EXT = ".jpg";

    private BufferedImage image = null;
    private final Product product;
    private final File browseFile;
    private final Path productQuicklookFolder;
    private final boolean productCanAppendFiles;
    private final boolean saveWithProduct;

    /**
     * Constructor when only a quicklook name is given. Quicklook will be generated using defaults
     *
     * @param product
     * @param name    the name of the quicklook
     */
    public Quicklook(final Product product, final String name) {
        this(product, name, null, false, null);
    }

    /**
     * Constructor when a browseFile is given. The quicklooks is generated from the browse file
     *
     * @param product
     * @param name       the name of the quicklook
     * @param browseFile the preview or browse image from a product
     */
    public Quicklook(final Product product, final String name, final File browseFile) {
        this(product, name, browseFile, false, null);
    }

    /**
     * Constructor when a browseFile is given. The quicklooks is generated from the browse file
     *
     * @param product
     * @param name                   the name of the quicklook
     * @param browseFile             the preview or browse image from a product
     * @param productCanAppendFiles  true when files may be written to the product
     * @param productQuicklookFolder where to write the quicklook files
     */
    public Quicklook(final Product product, final String name, final File browseFile,
                     final boolean productCanAppendFiles, final Path productQuicklookFolder) {
        super(name);
        this.product = product;
        this.browseFile = browseFile;
        this.productCanAppendFiles = productCanAppendFiles;
        this.productQuicklookFolder = productQuicklookFolder;

        final Preferences preferences = Config.instance().preferences();
        saveWithProduct = preferences.getBoolean(QuicklookGenerator.PREFERENCE_KEY_QUICKLOOKS_SAVE_WITH_PRODUCT,
                                                 QuicklookGenerator.DEFAULT_VALUE_QUICKLOOKS_SAVE_WITH_PRODUCT);
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

    public boolean hasImage() {
        return image != null;
    }

    public synchronized BufferedImage getImage() {
        if (image == null) {
            loadQuicklook();

            if (image == null) {
                final QuicklookGenerator qlGen = new QuicklookGenerator();
                try {
                    if (browseFile != null) {
                        final Product browseProduct = ProductIO.readProduct(browseFile);
                        image = qlGen.createQuickLookFromBrowseProduct(browseProduct, true);

                    } else {
                        image = qlGen.createQuickLookImage(product);
                    }
                    saveQuicklook(image);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to generate quicklook: " + e.getMessage());
                }
            }
        }
        return image;
    }

    private void loadQuicklook() {
        if (productQuicklookFolder != null) {
            // load from product

            final File quickLookFile = productQuicklookFolder.
                    resolve(SNAP_QUICKLOOK_FILE_PREFIX + getName() + QUICKLOOK_EXT).toFile();
            image = loadImage(quickLookFile);
        }
        if (image == null) {
            // load from database

            final File productFile = product.getFileLocation();
            if (productFile != null) {

                int id = QuicklookDB.instance().getQuicklookId(productFile);
                if (id != QuicklookDB.QL_NOT_FOUND) {
                    final File quickLookFile = QuicklookDB.getQuicklookCacheDir().
                            resolve(SNAP_QUICKLOOK_FILE_PREFIX + id + QUICKLOOK_EXT).toFile();
                    image = loadImage(quickLookFile);
                }
            }
        }
    }

    private BufferedImage loadImage(final File quickLookFile) {
        if (quickLookFile.exists()) {
            try {
                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(quickLookFile))) {
                    return ImageIO.read(fis);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe("Unable to load quicklook: " + quickLookFile);
            }
        }
        return null;
    }

    private void saveQuicklook(final BufferedImage bufferedImage) {
        if (saveWithProduct && productCanAppendFiles) {
            // save with product

            if (productQuicklookFolder != null) {
                final File quickLookFile = productQuicklookFolder.
                        resolve(SNAP_QUICKLOOK_FILE_PREFIX + getName() + QUICKLOOK_EXT).toFile();

                if (writeImage(bufferedImage, quickLookFile))
                    return;
            }
        }

        final File productFile = product.getFileLocation();
        if (productFile != null) {
            // save to database

            int id = QuicklookDB.instance().addQuickLookId(productFile);
            final File quickLookFile = QuicklookDB.getQuicklookCacheDir().
                    resolve(SNAP_QUICKLOOK_FILE_PREFIX + id + QUICKLOOK_EXT).toFile();
            writeImage(bufferedImage, quickLookFile);
        }
    }

    private boolean writeImage(final BufferedImage bufferedImage, final File quickLookFile) {
        try {
            if (quickLookFile.createNewFile()) {
                ImageIO.write(bufferedImage, "JPG", quickLookFile);
                return true;
            } else {
                SystemUtils.LOG.severe("Unable to save quicklook: " + quickLookFile);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to save quicklook: " + quickLookFile);
        }
        return false;
    }
}
