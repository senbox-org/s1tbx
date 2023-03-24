package org.esa.s1tbx.io.strix;

import org.esa.s1tbx.io.ceos.CEOSProductReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.engine_utilities.util.ZipUtils;

import java.io.File;
import java.nio.file.Path;

public class StriXProductReaderPlugIn extends CEOSProductReaderPlugIn {
    public StriXProductReaderPlugIn() {
        constants = new StriXConstants();
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new StriXProductReader(this);
    }

    @Override
    protected DecodeQualification checkProductQualification(final Path path) {
        final String name = path.getFileName().toString().toUpperCase();
        if(path.toFile().isDirectory()) {
            File folder = path.toFile();
            File[] files = folder.listFiles();
            if(files != null) {
                for (File file : files) {
                    if (isVolumeFile(file.getName())) {
                        return DecodeQualification.INTENDED;
                    }
                }
            }
        }
        if(name.contains("STRIX")) {
            for (String prefix : constants.getVolumeFilePrefix()) {
                if (name.startsWith(prefix)) {
                    final StriXProductReader reader = new StriXProductReader(this);
                    return reader.checkProductQualification(path);
                }
            }
        }
        if (name.endsWith(".ZIP") && (ZipUtils.findInZip(path.toFile(), "vol-strix", ""))) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }
}
