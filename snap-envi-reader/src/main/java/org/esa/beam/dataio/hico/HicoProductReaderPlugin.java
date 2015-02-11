package org.esa.beam.dataio.hico;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * Reader plugin for HICO data products.
 * http://hico.coas.oregonstate.edu/datasets/datacharacteristics.shtml
 */
public class HicoProductReaderPlugin implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File[] hdrFiles = findHdrFiles(input);
        if (hdrFiles.length > 0) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"HICO"};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new HicoProductReader(this);
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".hdr"};
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public String getDescription(Locale locale) {
        return "HICO Data Products";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null)) {
            @Override
            public boolean accept(File file) {
                // directories are accepted right away
                if (file.isDirectory()) {
                    return true;
                }
                String fileName = file.getName();
                return fileName.startsWith("iss.") && fileName.endsWith(".hico.hdr");
            }
        };
    }

    static File[] findHdrFiles(Object input) {
        if (input != null) {
            File file = new File(input.toString());
            String fileName = file.getName();
            HicoFilename hicoFilename = HicoFilename.create(fileName);
            if (hicoFilename != null) {
                File parentFile = file.getParentFile();
                if (parentFile == null) {
                    parentFile = new File(".");
                }
                return hicoFilename.findAllHdrs(parentFile);
            }
        }
        return new File[0];
    }
}
