package org.esa.snap.statistics.tools;

import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.TimeStampExtractor;

import java.io.File;

public class FilenameDateExtractor {

    private final TimeStampExtractor timeStampExtractor;

    public FilenameDateExtractor() {
        timeStampExtractor = new TimeStampExtractor("yyyyMMdd", "${startDate}_*.shp");
    }

    public boolean isValidFilename(File file) {
        try {
            timeStampExtractor.extractTimeStamps(file.getName());
            return true;
        } catch (ValidationException ignored) {
            return false;
        }
    }

    public ProductData.UTC getDate(File file) {
        try {
            if (isValidFilename(file)) {
                return timeStampExtractor.extractTimeStamps(file.getName())[0];
            }
        } catch (ValidationException ignored) {
        }
        return null;
    }
}
