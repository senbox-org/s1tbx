package org.esa.beam.statistics.tools;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.TimeStampExtractor;

import java.io.File;

public class FilenameDateExtractorImpl implements SummaryCSVTool.FilenameDateExtractor {

    private final TimeStampExtractor timeStampExtractor;

    public FilenameDateExtractorImpl() {
        timeStampExtractor = new TimeStampExtractor("yyyyMMdd", "${startDate}_*.shp");
    }

    @Override
    public boolean isValidFilename(File file) {
        try {
            timeStampExtractor.extractTimeStamps(file.getName());
            return true;
        } catch (ValidationException ignored) {
            return false;
        }
    }

    @Override
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
