package org.esa.snap.db;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;

/**
 * Optimize getting readers for common data products
 */
public class CommonReaders {

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
            return ProductIO.readProduct(file, "BEAM-DIMAP");
        } else if (filename.endsWith("n1") || filename.endsWith("e1") || filename.endsWith("e2")) {
            return ProductIO.readProduct(file, "ENVISAT");
        } else if ((filename.startsWith("tsx") || filename.startsWith("tdx")) && filename.endsWith("xml")) {
            return ProductIO.readProduct(file, "TerraSarX");
        } else if (filename.equals("product.xml")) {
            return ProductIO.readProduct(file, "RADARSAT-2");
        } else if (filename.endsWith("tif")) {
            return ProductIO.readProduct(file, "GeoTIFF");
        } else if (filename.endsWith("dbl")) {
            return ProductIO.readProduct(file, "SMOS-DBL");
        } else if (filename.endsWith("zip")) {
            if (filename.startsWith("asa")) {
                return ProductIO.readProduct(file, "ENVISAT");
            } else if (filename.startsWith("s1a")) {
                return ProductIO.readProduct(file, "SENTINEL-1");
            } else if (filename.startsWith("rs2")) {
                return ProductIO.readProduct(file, "RADARSAT-2");
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
