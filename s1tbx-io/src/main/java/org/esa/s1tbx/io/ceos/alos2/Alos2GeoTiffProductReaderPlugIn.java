package org.esa.s1tbx.io.ceos.alos2;

import org.esa.s1tbx.io.gamma.GammaProductReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Alos2GeoTiffProductReaderPlugIn implements ProductReaderPlugIn {
    private static final String[] FORMAT_NAMES = new String[]{"ALOS2 GeoTIFF"};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        try{
            final Object imageIOInput;
            if (input instanceof String){
                imageIOInput = new File((String) input);
            } else if (input instanceof File || input instanceof InputStream){
                imageIOInput = input;
            } else{
                return DecodeQualification.UNABLE;
            }
            if(input instanceof String || input instanceof File){
                final String extension = FileUtils.getExtension((File)imageIOInput).toUpperCase();

                if (extension.equals(".ZIP")){
                    return checkZIPFile(imageIOInput);
                }
                final String name = ((File) imageIOInput).getAbsolutePath();
                return checkFileName(name);
            }


            return DecodeQualification.UNABLE;

        }catch (Exception e){
            e.printStackTrace();
            return DecodeQualification.UNABLE;
        }
    }

    // Additional helper functions for getDecodeQualification
    private DecodeQualification checkFileName(String name){
        final Object imageIOInput;
        imageIOInput = new File(name);
        if (!(name.toUpperCase().contains("ALOS2"))){
            return DecodeQualification.UNABLE;
        }
        if ((name.toUpperCase().endsWith("TIF") || name.toUpperCase().endsWith("TIFF")) &&
                (name.toUpperCase().contains("IMG-") &&
                        (name.toUpperCase().contains("-HH-") ||
                                name.toUpperCase().contains("-HV-") ||
                                name.toUpperCase().contains("-VH-") ||
                                name.toUpperCase().contains("-VV-")))){

            File parentFolder = ((File) imageIOInput).getParentFile();
            for (File f : parentFolder.listFiles()){
                String fname = f.getName().toLowerCase();
                if (fname.equals("summary.txt")){
                    // File name contains the right keywords, and the folder contains the metadata file.
                    return DecodeQualification.INTENDED;
                }
            }
            // No metadata file
            return DecodeQualification.UNABLE;


        }
        else{
            return DecodeQualification.UNABLE;
        }

    }


    private DecodeQualification checkZIPFile(Object input) throws IOException {
        final Object imageIOInput;
        if (input instanceof String){
            imageIOInput = new ZipFile((String) input);
        }else if (input instanceof File){
            imageIOInput = new ZipFile((File) input);
        } else if (input instanceof ZipFile) {
            imageIOInput = input;
        }
        else{
            return DecodeQualification.UNABLE;
        }
        ZipFile zipFile = (ZipFile) imageIOInput;
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        boolean hasValidImage = false;
        boolean hasMetadata = false;

        while (zipEntries.hasMoreElements()) {
            ZipEntry e = zipEntries.nextElement();
            String name = e.getName().toUpperCase();
            if ((name.endsWith("TIF") || name.endsWith("TIFF")) &&
                    (name.startsWith("IMG-") &&
                            (name.contains("-HH-") ||
                                    name.contains("-HV-") ||
                                    name.contains("-VH-") ||
                                    name.contains("-VV-")))) {
                hasValidImage = true;
            }
            if (name.contains("SUMMARY.TXT")){
                hasMetadata = true;
            }

        }
        if (hasMetadata && hasValidImage){
            return DecodeQualification.INTENDED;
        } else{
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        Class [] returnClass = new Class[2];

        returnClass[0] = String.class;
        returnClass[1] = File.class;
        return returnClass;
    }

    @Override
    public ProductReader createReaderInstance()  {
        return new Alos2GeoTiffProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        final String [] extensions = new String[3];
        extensions[0] = "tif";
        extensions[1] = "tiff";
        extensions[2] = "zip";
        return extensions;
    }

    @Override
    public String getDescription(Locale locale) {
        return "ALOS2 GeoTIFF data product.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));

    }
}
