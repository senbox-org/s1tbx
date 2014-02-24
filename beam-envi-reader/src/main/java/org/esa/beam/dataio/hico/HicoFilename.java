package org.esa.beam.dataio.hico;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the information encoded in a HICO filename.
 */
class HicoFilename {
    private static final DateFormat COLLECTION_DATE_FORMAT = ProductData.UTC.createDateFormat("yyyyDDD.MMdd.HHmmss");
    private static final DateFormat L0_DATE_FORMAT = ProductData.UTC.createDateFormat("yyyyMMddHHmmss");
    private static final Pattern PARTS_PATTERN = Pattern.compile(
            "iss\\.(\\d{7}\\.\\d{4}\\.\\d{6})\\." +
                    "(L[^\\.]+)\\.([^\\.]+)\\." +
                    "([^\\.]+)\\.([^\\.]+)\\." +
                    "(\\d{14})\\." +
                    "([^\\.]+)\\.([^\\.]+)\\..*");

    private final ProductData.UTC acquisitionUTC;
    private final ProductData.UTC l0UTC;
    private final String processingLevel;
    private final String processingVersion;
    private final String target;
    private final String sceneID;
    private final String spatialResolution;
    private final String fileType;
    private final String productBase;

    HicoFilename(ProductData.UTC acquisitionUTC, ProductData.UTC l0UTC,
                 String processingLevel, String processingVersion,
                 String target, String sceneID, String spatialResolution, String fileType, String productBase) {
        this.acquisitionUTC = acquisitionUTC;
        this.l0UTC = l0UTC;
        this.processingLevel = processingLevel;
        this.processingVersion = processingVersion;
        this.target = target;
        this.sceneID = sceneID;
        this.spatialResolution = spatialResolution;
        this.fileType = fileType;
        this.productBase = productBase;
    }

    public ProductData.UTC getAcquisitionUTC() {
        return acquisitionUTC;
    }

    public ProductData.UTC getL0UTC() {
        return l0UTC;
    }

    public String getProcessingLevel() {
        return processingLevel;
    }

    public String getProcessingVersion() {
        return processingVersion;
    }

    public String getTarget() {
        return target;
    }

    public String getSceneID() {
        return sceneID;
    }

    public String getSpatialResolution() {
        return spatialResolution;
    }

    public String getFileType() {
        return fileType;
    }

    public String getProductBase() {
        return productBase;
    }

    @Override
    public String toString() {
        return "HicoFilename{" +
                "acquisitionUTC=" + acquisitionUTC +
                ", l0UTC=" + l0UTC +
                ", processingLevel='" + processingLevel + '\'' +
                ", processingVersion='" + processingVersion + '\'' +
                ", target='" + target + '\'' +
                ", sceneID='" + sceneID + '\'' +
                ", spatialResolution='" + spatialResolution + '\'' +
                ", fileType='" + fileType + '\'' +
                '}';
    }


    static HicoFilename create(String filename) {
        Matcher partMatcher = PARTS_PATTERN.matcher(filename);
        if (partMatcher.matches()) {
            try {
                String collectionDateString = partMatcher.group(1);
                String processingLevel = partMatcher.group(2);
                String target = partMatcher.group(3);
                String processingVersion = partMatcher.group(4);
                String sceneID = partMatcher.group(5);
                String l0DateString = partMatcher.group(6);
                String spatialResolution = partMatcher.group(7);
                String fileType = partMatcher.group(8);

                Date collectionDate = COLLECTION_DATE_FORMAT.parse(collectionDateString);
                ProductData.UTC acquisitionUTC = ProductData.UTC.create(collectionDate, 0L);
                Date l0Date = L0_DATE_FORMAT.parse(l0DateString);
                ProductData.UTC l0UTC = ProductData.UTC.create(l0Date, 0L);

                String productBase = "iss." + collectionDateString + "." + processingLevel + "." + target + "." + processingVersion + "." + sceneID + "." + l0DateString + "." + spatialResolution + ".";

                return new HicoFilename(acquisitionUTC, l0UTC,
                                 processingLevel, processingVersion,
                                 target, sceneID, spatialResolution, fileType, productBase);
            } catch (ParseException ignore) {
                Debug.trace(ignore);
            }
        }
        return null;
    }

    public File[] findAllHdrs(File parentFile) {
        File[] hdrFiles = parentFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                System.out.println("dir = " + dir);
                System.out.println("name = " + name);
                boolean hdrExist = name.startsWith(getProductBase()) && name.endsWith(".hdr");
                System.out.println("hdrExist = " + hdrExist);
                boolean dataExist = false;
                if (hdrExist) {
                    int hdrIndex = name.lastIndexOf("hdr");
                    String bandBaseName = name.substring(0, hdrIndex);
                    File bsqFile = new File(dir, bandBaseName + "bsq");
                    File bilFile = new File(dir, bandBaseName + "bil");
                    dataExist = bsqFile.exists() || bilFile.exists();
                    System.out.println("dataExist = " + dataExist);
                }
                return hdrExist && dataExist;
            }
        });
        if (hdrFiles == null) {
            hdrFiles = new File[0];
        }
        return hdrFiles;
    }
}
