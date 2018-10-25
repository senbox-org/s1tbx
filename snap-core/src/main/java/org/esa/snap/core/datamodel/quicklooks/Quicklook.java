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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductVisitor;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Created by luis on 10/01/2016.
 */
public class Quicklook extends ProductNode implements Thumbnail {

    public final static String DEFAULT_QUICKLOOK_NAME = "Quicklook";
    public final static String SNAP_QUICKLOOK_FILE_PREFIX = "snapQL_";
    private final static String QUICKLOOK_EXT = ".jpg";

    private BufferedImage image;
    private Product product;
    private File productFile;
    private final File browseFile;
    private Band[] quicklookBands;
    private final Path productQuicklookFolder;
    private final boolean productCanAppendFiles;
    private final boolean saveWithProduct;

    private String quicklookLink = null;

    public Quicklook(final File productFile) {
        this(null, DEFAULT_QUICKLOOK_NAME);
        this.productFile = productFile;
    }

    /**
     * Constructor when only a quicklook name is given. Quicklook will be generated using defaults
     *
     * @param product the source product
     * @param name    the name of the quicklook
     */
    public Quicklook(final Product product, final String name) {
        this(product, name, null, false, null, null);
    }

    /**
     * Constructor when a browseFile is given. The quicklook is generated from the browse file
     *
     * @param product    the source product
     * @param name       the name of the quicklook
     * @param browseFile the preview or browse image from a product
     */
    public Quicklook(final Product product, final String name, final File browseFile) {
        this(product, name, browseFile, false, null, null);
    }

    /**
     * Constructor when a browseFile is given. The quicklook is generated from the browse file
     *
     * @param product    the source product
     * @param name       the name of the quicklook
     * @param quicklookBands   the bands to create an RGB quicklook from
     */
    public Quicklook(final Product product, final String name, final Band[] quicklookBands) {
        this(product, name, null, false, null, quicklookBands);
    }

    /**
     * Constructor when a browseFile is given. The quicklook is generated from the browse file
     *
     * @param product                the source product
     * @param name                   the name of the quicklook
     * @param browseFile             the preview or browse image from a product
     * @param productCanAppendFiles  true when files may be written to the product
     * @param productQuicklookFolder where to write the quicklook files
     */
    public Quicklook(final Product product, final String name, final File browseFile,
                     final boolean productCanAppendFiles, final Path productQuicklookFolder,
                     final Band[] quicklookBands) {
        super(name);
        this.browseFile = browseFile;
        this.productCanAppendFiles = productCanAppendFiles;
        this.productQuicklookFolder = productQuicklookFolder;
        this.quicklookBands = quicklookBands;

        setProduct(product);

        final Preferences preferences = Config.instance().preferences();
        saveWithProduct = preferences.getBoolean(QuicklookGenerator.PREFERENCE_KEY_QUICKLOOKS_SAVE_WITH_PRODUCT,
                                                 QuicklookGenerator.DEFAULT_VALUE_QUICKLOOKS_SAVE_WITH_PRODUCT);
    }

    public void setProduct(final Product product) {
        if(product != null) {
            this.product = product;
            this.productFile = product.getFileLocation();
        }
    }

    public boolean hasProduct() {
        return product != null;
    }

    public File getProductFile() {
        return productFile;
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

    public void setQuicklookLink(final String link) {
         this.quicklookLink = link;
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

    /*
     * Checks if a quicklook file is cached either with the product or in the database
     * @return true if a cached quicklook file is found
     */
    public boolean hasCachedImage() {
        if (productQuicklookFolder != null) {
            // load from product
            final File quickLookFile = productQuicklookFolder.resolve(getQLFileName(0)).toFile();
            if (quickLookFile.exists()) {
                return true;
            }
        }
        // load from database
        if (productFile != null) {

            int id = QuicklookDB.instance().getQuicklookId(productFile);
            if (id != QuicklookDB.QL_NOT_FOUND) {
                final File quickLookFile = QuicklookDB.getQuicklookCacheDir().resolve(getQLFileName(id)).toFile();
                return quickLookFile.exists();
            }
        }
        return false;
    }

    public boolean hasImage() {
        return image != null;
    }

    public synchronized BufferedImage getImage(final ProgressMonitor pm) {
        if (image == null) {
            loadQuicklook();

            if (image == null) {
                final QuicklookGenerator qlGen = new QuicklookGenerator();
                try {
                    if (browseFile != null) {
                        final Product browseProduct = readBrowseProduct(browseFile);
                        image = qlGen.createQuickLookFromBrowseProduct(browseProduct);

                    } else {
                        if(product != null) {
                            if(quicklookBands == null) {
                                quicklookBands = qlGen.findQuicklookBands(product);
                            }
                            if(quicklookBands != null) {
                                image = qlGen.createQuickLookImage(product, quicklookBands, pm);
                            }
                        } else {
                            throw new IOException("Quicklook: product not set");
                        }
                    }
                    if(image != null) {
                        saveQuicklook(image);
                        notifyImageUpdated();
                    }
                } catch (Throwable e) {
                    SystemUtils.LOG.severe("Quicklook: Unable to generate quicklook: " + e.getMessage());
                }
            }
        }
        return image;
    }

    private static Product readBrowseProduct(final File file) throws IOException {
        final String filename = file.getName().toLowerCase();
        ProductReader productReader = null;
        if (filename.endsWith("tif")) {
            productReader = ProductIO.getProductReader("GeoTIFF");
        } else if (filename.endsWith("png") || filename.endsWith("jpg")) {
            productReader = ProductIO.getProductReader("PNG");
        }
        if (productReader != null) {
            return productReader.readProductNodes(file, null);
        }
        return ProductIO.readProduct(file);
    }

    private void loadQuicklook() {
        //System.out.println("Quicklook.loadQuicklook: called");
        if (productQuicklookFolder != null) {
            // load from product

            final File quickLookFile = productQuicklookFolder.resolve(getQLFileName(0)).toFile();
            image = QuicklookGenerator.loadImage(quickLookFile);
        }
        if (quicklookLink != null) {
            try {
                URL url = new URL(quicklookLink);
                image = ImageIO.read(url);
            } catch (Exception e) {
                SystemUtils.LOG.warning("Quicklook URL ERROR " + e.getMessage() + "; link = " + quicklookLink);
            }
        }
        if (image == null) {
            // load from database
            if(productFile == null && product != null) {
                productFile = product.getFileLocation();
            }
            if (productFile != null) {

                int id = QuicklookDB.instance().getQuicklookId(productFile);
                if (id != QuicklookDB.QL_NOT_FOUND) {
                    final File quickLookFile = QuicklookDB.getQuicklookCacheDir().resolve(getQLFileName(id)).toFile();
                    if(quickLookFile.exists()) {
                        image = QuicklookGenerator.loadImage(quickLookFile);
                    }
                }
            }
        }
    }

    private void saveQuicklook(final BufferedImage bufferedImage) {
        if(bufferedImage == null)
            return;

        if (saveWithProduct && productCanAppendFiles) {
            // save with product

            if (productQuicklookFolder != null) {
                final File quickLookFile = productQuicklookFolder.resolve(getQLFileName(0)).toFile();

                if (QuicklookGenerator.writeImage(bufferedImage, quickLookFile))
                    return;
            }
        }

        if (productFile != null) {
            // save to database

            int id = QuicklookDB.instance().addQuickLookId(productFile);
            final File quickLookFile = QuicklookDB.getQuicklookCacheDir().resolve(getQLFileName(id)).toFile();
            QuicklookGenerator.writeImage(bufferedImage, quickLookFile);
        }
    }

    private String getQLFileName(final int id) {
        return SNAP_QUICKLOOK_FILE_PREFIX + id + '_' + getName() + QUICKLOOK_EXT;
    }
}
