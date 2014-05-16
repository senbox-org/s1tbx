package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import org.esa.beam.framework.dataio.DecodeQualification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

/**
 * For detecting NOAA AVHRR HRPT L1B data in POD format.
 *
 * @author Ralf Quast
 */
class PodFormatDetector implements FormatDetector {

    private final RandomAccessFile raf;
    private final DecodeQualification decodeQualification;

    PodFormatDetector(File file) throws FileNotFoundException {
        raf = new RandomAccessFile(file, "r");

        if (hasTbmHeader(raf)) {
            decodeQualification = DecodeQualification.INTENDED;
        } else {
            decodeQualification = DecodeQualification.UNABLE;
        }
    }

    @Override
    public boolean canDecode() {
        return decodeQualification == DecodeQualification.INTENDED;
    }

    @Override
    public void dispose() {
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }

    private static boolean hasTbmHeader(RandomAccessFile raf) {
        final DataFormat dataFormat = new DataFormat(PodTypes.tbmHeaderRecordType, ByteOrder.BIG_ENDIAN);
        final DataContext context = dataFormat.createContext(raf);
        final CompoundData tbmHeaderData = context.getData();

        try {
            return isTbmHeader(tbmHeaderData);
        } catch (IOException e) {
            return false;
        } finally {
            context.dispose();
        }
    }

    // package public for testing only
    static boolean isTbmHeader(CompoundData tbmHeaderData) throws IOException {
        final String totalOrSelectiveCopy = getString(tbmHeaderData, 2);
        if ("T".equals(totalOrSelectiveCopy) || "S".equals(totalOrSelectiveCopy)) {
            final String appendedDataSelection = getString(tbmHeaderData, 10);
            if ("Y".equals(appendedDataSelection) || "N".equals(appendedDataSelection)) {
                final String datasetName = getString(tbmHeaderData, 1);
                if (datasetName.matches("[A-Z]{3}\\.HRPT\\..*")) {
                    return true;
                }
            }
        }
        return false;
    }

    // package public for testing only
    static String getString(CompoundData data, int index) throws IOException {
        return HeaderWrapper.getAsString(data.getSequence(index));
    }

}
