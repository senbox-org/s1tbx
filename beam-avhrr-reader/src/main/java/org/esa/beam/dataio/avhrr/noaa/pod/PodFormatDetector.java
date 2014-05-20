package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

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
class PodFormatDetector {

    public boolean canDecode(File file) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            final DataFormat dataFormat = new DataFormat(PodTypes.TBM_HEADER_RECORD_TYPE, ByteOrder.BIG_ENDIAN);
            final DataContext context = dataFormat.createContext(raf);
            try {
                return isTbmHeaderRecord(context.getData());
            } catch (IOException e) {
                return false;
            } finally {
                context.dispose();
            }
        } catch (FileNotFoundException e) {
            return false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ignored) {
                }
            }
        }

    }

    // package public for testing only
    static boolean isTbmHeaderRecord(CompoundData data) throws IOException {
        final String totalOrSelectiveCopy = getString(data, 2);
        if ("T".equals(totalOrSelectiveCopy) || "S".equals(totalOrSelectiveCopy)) {
            final String appendedDataSelection = getString(data, 10);
            if ("Y".equals(appendedDataSelection) || "N".equals(appendedDataSelection)) {
                final String datasetName = getString(data, 1);
                if (datasetName.matches("[A-Z]{3}\\.HRPT\\..*")) {
                    return true;
                }
            }
        }
        return false;
    }

    // package public for testing only
    static String getString(CompoundData data, int index) throws IOException {
        return toString(data.getSequence(index));
    }

    // package public for testing only
    static String toString(SequenceData valueSequence) throws IOException {
        final byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data);
    }
}
