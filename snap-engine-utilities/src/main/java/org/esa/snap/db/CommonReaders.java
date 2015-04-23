package org.esa.snap.db;

import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Optimize getting readers for common data products
 */
public class CommonReaders {

    private final static ProductReaderPlugIn dimapReadPlugIn = getReaderPlugIn("BEAM-DIMAP");
    private final static ProductReaderPlugIn envisatReadPlugIn = getReaderPlugIn("ENVISAT");
    private final static ProductReaderPlugIn TSXReadPlugIn = getReaderPlugIn("TerraSarX");
    private final static ProductReaderPlugIn RS2ReadPlugIn = getReaderPlugIn("RADARSAT-2");
    private final static ProductReaderPlugIn S1ReadPlugIn = getReaderPlugIn("SENTINEL-1");
    private final static ProductReaderPlugIn GeoTiffReadPlugIn = getReaderPlugIn("GeoTIFF");

    private static ProductReaderPlugIn getReaderPlugIn(final String format) {
        final ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> itr = registry.getReaderPlugIns(format);
        return itr != null && itr.hasNext() ? itr.next() : null;
    }

    public static Product readProduct(final File file) throws IOException {
        Product product = CommonReaders.readCommonProductReader(file);
        if(product == null) {
            product = ProductIO.readProduct(file);
        }
        return product;
    }

    /**
     * Quickly return the product read by the right reader without testing many readers
     *
     * @param file input file
     * @return the product
     * @throws java.io.IOException if can't be read
     */
    public static Product readCommonProductReader(final File file) throws IOException {
        final String filename = file.getName().toLowerCase();
        if (filename.endsWith("dim")) {
            return read(file, dimapReadPlugIn);
        } else if (filename.endsWith("n1") || filename.endsWith("e1") || filename.endsWith("e2")) {
            return read(file, envisatReadPlugIn);
        } else if ((filename.startsWith("tsx") || filename.startsWith("tdx")) && filename.endsWith("xml")) {
            return read(file, TSXReadPlugIn);
        } else if (filename.equals("product.xml")) {
            return read(file, RS2ReadPlugIn);
        } else if (filename.endsWith("tif")) {
            return read(file, GeoTiffReadPlugIn);
        } else if (filename.endsWith("dbl")) {
            return ProductIO.readProduct(file, "SMOS-DBL");
        } else if (filename.endsWith("zip")) {
            if (filename.startsWith("asa")) {
                return read(file, envisatReadPlugIn);
            } else if (filename.startsWith("s1a")) {
                return read(file, S1ReadPlugIn);
            } else if (filename.startsWith("rs2")) {
                return read(file, RS2ReadPlugIn);
            }
        }
        return null;
    }

    private static Product read(final File file, final ProductReaderPlugIn selectedPlugIn) throws IOException {
        if(selectedPlugIn == null)
            return null;
        final ProductReader productReader = selectedPlugIn.createReaderInstance();
        return productReader == null ? null : productReader.readProductNodes(file, null);
    }

    /**
     * Quickly return the product reader without testing many readers
     *
     * @param file input file
     * @return the product reader or null
     */
    public static ProductReader findCommonProductReader(final File file) {
        final String filename = file.getName().toLowerCase();
        if (filename.endsWith("n1") || filename.endsWith("e1") || filename.endsWith("e2")) {
            return ProductIO.getProductReader("ENVISAT");
        } else if (filename.endsWith("dim")) {
            return ProductIO.getProductReader("BEAM-DIMAP");
        } else if ((filename.startsWith("tsx") || filename.startsWith("tdx")) && filename.endsWith("xml")) {
            return ProductIO.getProductReader("TerraSarX");
        } else if (filename.endsWith("tif")) {
            return ProductIO.getProductReader("GeoTIFF");
        } else if (filename.endsWith("dbl")) {
            return ProductIO.getProductReader("SMOS-DBL");
        } else if (filename.endsWith("zip")) {
            if (filename.startsWith("asa")) {
                return ProductIO.getProductReader("ENVISAT");
            } else if (filename.startsWith("s1a")) {
                return ProductIO.getProductReader("SENTINEL-1");
            } else if (filename.startsWith("rs2")) {
                return ProductIO.getProductReader("RADARSAT-2");
            }
        }
        return null;
    }
}
