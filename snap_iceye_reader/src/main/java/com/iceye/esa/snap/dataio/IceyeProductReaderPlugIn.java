package com.iceye.esa.snap.dataio;

import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import org.esa.s1tbx.io.netcdf.NetCDFReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Ahmad Hamouda
 */
public class IceyeProductReaderPlugIn extends NetCDFReaderPlugIn {

    private AtomicBoolean isTiff = new AtomicBoolean();

    public IceyeProductReaderPlugIn() {
        FORMAT_NAMES = IceyeXConstants.getIceyeFormatNames();
        FORMAT_FILE_EXTENSIONS = IceyeXConstants.getIceyeFormatFileExtensions();
        PLUGIN_DESCRIPTION = IceyeXConstants.ICEYE_PLUGIN_DESCRIPTION;
    }

    /**
     * Validate file extension and start
     *
     * @param file
     * @return check result
     */
    @Override
    protected DecodeQualification checkProductQualification(final File file) {
        final String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".h5") && fileName.startsWith(IceyeXConstants.ICEYE_FILE_PREFIX.toLowerCase())) {
            isTiff.set(false);
            return DecodeQualification.INTENDED;
        } else if ((fileName.endsWith(".tif") || fileName.endsWith(".tiff")) && fileName.startsWith(IceyeXConstants.ICEYE_FILE_PREFIX.toLowerCase())) {
            isTiff.set(true);
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public DecodeQualification getDecodeQualification(final Object input) {
        File file = ReaderUtils.getFileFromInput(input);
        return file == null ? DecodeQualification.UNABLE : this.checkProductQualification(file);
    }

    /**
     * Creates an instance of the actual product reader class.
     *
     * @return a new reader instance
     */
    @Override
    public ProductReader createReaderInstance() {
        if (isTiff.get()) {
            return new IceyeGRDProductReader(this);
        }
        return new IceyeProductReader(this);
    }

}
