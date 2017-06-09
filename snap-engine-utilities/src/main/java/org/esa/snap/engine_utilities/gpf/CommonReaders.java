package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;

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
    private final static ProductReaderPlugIn ImageIOReadPlugIn = getReaderPlugIn("PNG");

    private static ProductReaderPlugIn getReaderPlugIn(final String format) {
        final ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> itr = registry.getReaderPlugIns(format);
        return itr != null && itr.hasNext() ? itr.next() : null;
    }

    public static Product readProduct(final File file) throws IOException {
        Product product = CommonReaders.readCommonProductReader(file);
        if(product == null) {
            //SystemUtils.LOG.warning("Reading uncommon "+file);
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
            } else if (filename.startsWith("s1")) {
                return read(file, S1ReadPlugIn);
            } else if (filename.startsWith("rs2")) {
                return read(file, RS2ReadPlugIn);
            }
        } else if (filename.endsWith("png")) {
            return read(file, ImageIOReadPlugIn);
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
     * Quickly return the product format without testing many readers
     *
     * @param file input file
     * @return the product format or null
     */
    public static String findCommonProductFormat(final File file) {
        final String filename = file.getName().toLowerCase();
        if (filename.endsWith("dim")) {
            return "BEAM-DIMAP";
        } else if (filename.endsWith("n1") || filename.endsWith("e1") || filename.endsWith("e2")) {
            return "ENVISAT";
        } else if ((filename.startsWith("tsx") || filename.startsWith("tdx")) && filename.endsWith("xml")) {
            return "TerraSarX";
        } else if (filename.endsWith("tif")) {
            return "GeoTIFF";
        } else if (filename.endsWith("dbl")) {
            return "SMOS-DBL";
        } else if (filename.endsWith("zip")) {
            if (filename.startsWith("asa")) {
                return "ENVISAT";
            } else if (filename.startsWith("s1")) {
                return "SENTINEL-1";
            } else if (filename.startsWith("rs2")) {
                return "RADARSAT-2";
            }
        }
        return null;
    }

    /**
     * Quickly return the product reader without testing many readers
     *
     * @param file input file
     * @return the product reader or null
     */
    public static ProductReader findCommonProductReader(final File file) {
        final String format = findCommonProductFormat(file);
        if (format != null) {
            return ProductIO.getProductReader(format);
        }
        return null;
    }
}
