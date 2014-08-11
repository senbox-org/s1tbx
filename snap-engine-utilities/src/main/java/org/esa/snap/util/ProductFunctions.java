package org.esa.snap.util;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;
import java.util.ArrayList;

/**

 */
public class ProductFunctions {

    private final static String[] validExtensions = {".dim", ".n1", ".e1", ".e2", ".h5"};
    private final static String[] xmlPrefix = {"product", "tsx1_sar", "tsx2_sar", "tdx1_sar", "tdx2_sar"};

    private static final String[] nonValidExtensions = {"xsd", "xsl", "xls", "pdf", "txt", "doc", "ps", "db", "ief", "ord",
            "tfw", "gif", "jpg", "jgw", "hdr", "self", "report", "raw", "tgz",
            "log", "html", "htm", "png", "bmp", "ps", "aux", "ovr", "brs", "kml", "kmz",
            "sav", "7z", "rrd", "lbl", "z", "gz", "exe", "so", "dll", "bat", "sh", "rtf",
            "prj", "dbf", "shx", "shp", "ace", "ace2", "tar", "tooldes", "metadata.xml"};
    private static final String[] nonValidprefixes = {"led", "trl", "tra_", "nul", "lea", "dat", "img", "imop", "sarl", "sart", "par_",
            "dfas", "dfdn", "lut",
            "readme", "l1b_iif", "dor_vor", "imagery_", "browse"};

    public static boolean isValidProduct(final File file) {
        final String name = file.getName().toLowerCase();
        for (String str : validExtensions) {
            if (name.endsWith(str)) {
                return true;
            }
        }
        if (name.endsWith("xml")) {

            for (String str : xmlPrefix) {
                if (name.startsWith(str)) {
                    return true;
                }
            }
            return false;
        }

        // test with readers
        final ProductReader reader = ProductIO.getProductReaderForInput(file);
        return reader != null;
    }

    /**
     * recursively scan a folder for valid files and put them in pathList
     *
     * @param inputFolder starting folder
     * @param pathList    list of products found
     */
    public static void scanForValidProducts(final File inputFolder, final ArrayList<String> pathList) {
        final ValidProductFileFilter dirFilter = new ValidProductFileFilter();
        final File[] files = inputFolder.listFiles(dirFilter);
        for (File file : files) {
            if (file.isDirectory()) {
                scanForValidProducts(file, pathList);
            } else if (isValidProduct(file)) {
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
            if (file.isDirectory()) return includeFolders;
            final String name = file.getName().toLowerCase();
            for (String ext : validExtensions) {
                if (name.endsWith(ext)) {
                    if (name.startsWith("asa_wss"))
                        return false;
                    return true;
                }
            }
            for (String pre : nonValidprefixes) {
                if (name.startsWith(pre))
                    return false;
            }
            for (String ext : nonValidExtensions) {
                if (name.endsWith(ext))
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

        final static String[] skip = {"annotation", "measurement", "auxraster", "auxfiles", "imagedata", "preview",
                "support", "quality", "source_images", "schemas"};

        public boolean accept(final File file) {
            if (!file.isDirectory()) return false;
            final String name = file.getName().toLowerCase();
            if (name.endsWith(DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION))
                return false;
            if (name.endsWith("safe"))
                return true;
            for (String ext : skip) {
                if (name.equalsIgnoreCase(ext))
                    return false;
            }
            return true;
        }
    }
}
