package org.esa.nest.util;

import org.esa.beam.util.io.FileUtils;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.dataio.dimap.DimapProductConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.bc.ceres.core.runtime.RuntimeContext;

/**

 */
public class ProductFunctions {

    private final static String[] validExtensions = {".dim",".n1",".e1",".e2",".h5"};
    private final static String[] xmlPrefix = { "product", "tsx1_sar", "tsx2_sar", "tdx1_sar", "tdx2_sar" };

    private static final String[] nonValidExtensions = { "xsd", "xsl", "xls", "pdf", "txt", "doc", "ps", "db", "ief", "ord",
                                                   "tfw","gif","jpg","jgw", "hdr", "self", "report", "raw", "tgz",
                                                   "log","html","htm","png","bmp","ps","aux","ovr","brs","kml", "kmz",
                                                   "sav","7z","rrd","lbl","z","gz","exe","so","dll","bat","sh","rtf",
                                                   "prj","dbf","shx","shp","ace","ace2","tar","tooldes", "metadata.xml"};
    private static final String[] nonValidprefixes = { "led","trl","tra_","nul","lea","dat","img","imop","sarl","sart","par_",
                                                 "dfas","dfdn","lut",
                                                 "readme", "l1b_iif", "dor_vor", "imagery_", "browse" };

    public static boolean isValidProduct(final File file) {
        final String name = file.getName().toLowerCase();
        for(String str : validExtensions) {
            if(name.endsWith(str)) {
                return true;
            }
        }
        if(name.endsWith("xml")) {

            for(String str : xmlPrefix) {
                if(name.startsWith(str)) {
                    return true;
                }
            }
            return false;
        }

        // test with readers
        final ProductReader reader = ProductIO.getProductReaderForFile(file);
        return reader != null;
    }

    /**
     * recursively scan a folder for valid files and put them in pathList
     * @param inputFolder starting folder
     * @param pathList list of products found
     */
    public static void scanForValidProducts(final File inputFolder, final ArrayList<String> pathList) {
        final ValidProductFileFilter dirFilter = new ValidProductFileFilter();
        final File[] files = inputFolder.listFiles(dirFilter);
        for(File file : files) {
            if(file.isDirectory()) {
                scanForValidProducts(file, pathList);
            } else if(isValidProduct(file)) {
                pathList.add(file.getAbsolutePath());
            }
        }
    }

    /**
     * any files (not folders) that could be products
     */
    public static class ValidProductFileFilter implements java.io.FileFilter {
        private final boolean includeFolders;

        public ValidProductFileFilter() {
            this.includeFolders = false;
        }

        public ValidProductFileFilter(final boolean includeFolders) {
            this.includeFolders = includeFolders;
        }

        public boolean accept(final File file) {
            if(file.isDirectory()) return includeFolders;
            final String name = file.getName().toLowerCase();
            for(String ext : validExtensions) {
                if(name.endsWith(ext)) {
                    return true;
                }
            }
            for(String pre : nonValidprefixes) {
                if(name.startsWith(pre))
                    return false;
            }
            for(String ext : nonValidExtensions) {
                if(name.endsWith(ext))
                    return false;
            }
            return true;
        }
    }

    public final static DirectoryFileFilter directoryFileFilter = new DirectoryFileFilter();

    /**
     * collect valid folders to scan
     */
    public static class DirectoryFileFilter implements java.io.FileFilter {

        final static String[] skip = { "annotation", "measurement", "auxraster", "auxfiles", "imagedata", "preview",
                                        "support", "quality", "source_images", "schemas" };

        public boolean accept(final File file) {
            if(!file.isDirectory()) return false;
            final String name = file.getName().toLowerCase();
            if(name.endsWith(DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION))
                return false;
            if(name.endsWith("safe"))
                return true;
            for(String ext : skip) {
                if(name.equalsIgnoreCase(ext))
                    return false;
            }
            return true;
        }
    }

    /**
     * Quickly return the product read by the right reader without testing many readers
     * @param file input file
     * @return the product
     * @throws IOException if can't be read
     */
    public static Product readCommonProductReader(final File file) throws IOException {
        final String filename = file.getName().toLowerCase();
        if(filename.endsWith("n1")) {
            return ProductIO.readProduct(file, "ENVISAT");
        } else if(filename.endsWith("e1") || filename.endsWith("e2")) {
            return ProductIO.readProduct(file, "ERS1/2");
        } else if(filename.endsWith("dim")) {
            return ProductIO.readProduct(file, "BEAM-DIMAP");
        } else if((filename.startsWith("TSX") || filename.startsWith("TDX")) && filename.endsWith("xml")) {
            return ProductIO.readProduct(file, "TerraSarX");
        } else if(filename.equals("product.xml")) {
            try {
                return ProductIO.readProduct(file, "RADARSAT-2");
            } catch(IOException e) {
                return ProductIO.readProduct(file, "RADARSAT-2 NITF");
            }
        } else if(filename.endsWith("tif")) {
            return ProductIO.readProduct(file, "GeoTIFF");
        } else if(file.isDirectory()) {
            return ProductIO.readProduct(file, "PolSARPro");
        } else if(filename.endsWith("dbl")) {
            return ProductIO.readProduct(file, "SMOS-DBL");
        }
        return null;
    }
}
