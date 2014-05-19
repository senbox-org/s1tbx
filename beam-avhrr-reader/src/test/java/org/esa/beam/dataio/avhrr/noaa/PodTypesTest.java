package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ralf Quast
 */
public class PodTypesTest {

    @Test
    public void testSolarZenithAnglesScaleFactor() throws Exception {
        assertEquals(1.0 / 2, PodTypes.getSolarZenithAnglesMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testLatScaleFactor() throws Exception {
        assertEquals(1.0 / 128, PodTypes.getLatMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testLonScaleFactor() throws Exception {
        assertEquals(1.0 / 128, PodTypes.getLonMetadata().getScalingFactor(), 0.0);
    }

    @Ignore
    @Test
    public void testReadAvhrrHrptFile() throws Exception {

        final DataFormat dataFormat = new DataFormat(PodTypes.hrptType, ByteOrder.BIG_ENDIAN);

        final File file = new File("/Users/ralf/Desktop/ao11090194162709_044816.l1b");
        final DataContext context = dataFormat.createContext(file, "r");
        final CompoundData hrptData = context.getData();
        final CompoundData tbmHeaderData = hrptData.getCompound("TBM_HEADER_RECORD");

        assertNotNull(tbmHeaderData);
        assertEquals(14, tbmHeaderData.getMemberCount());

        assertEquals("NSS.HRPT.NH.D94244.S1627.E1650.B3059394.SF  ", getString(tbmHeaderData, 1));
        assertEquals("T", getString(tbmHeaderData, 2));
        assertEquals("Y", getString(tbmHeaderData, 10));

        final CompoundData datasetHeaderRecord = hrptData.getCompound("DATASET_HEADER_RECORD");
        assertEquals("3059394", getString(datasetHeaderRecord,
                                          datasetHeaderRecord.getMemberIndex("PROCESSING_BLOCK_ID")));

        assertEquals(1, hrptData.getUByte("SPACECRAFT_ID"));
        assertEquals(8422, hrptData.getUShort("NUMBER_OF_SCANS"));

        final SequenceData data = hrptData.getSequence("DATA_RECORDS");

        // first scan
        final CompoundData dataRecord = data.getCompound(0);
        assertEquals(1, dataRecord.getUShort("SCAN_LINE_NUMBER"));
        assertEquals(51, dataRecord.getUByte("NUMBER_OF_MEANINGFUL_ZENITH_ANGLES_AND_EARTH_LOCATION_POINTS"));

        final CompoundType solarZenithAnglesType = dataRecord.getType();
        final int memberIndex = solarZenithAnglesType.getMemberIndex("SOLAR_ZENITH_ANGLES");
        final FormatMetadata zenithAngleMetadata = (FormatMetadata) solarZenithAnglesType.getMember(
                memberIndex).getMetadata();
        assertEquals(0.5, zenithAngleMetadata.getScalingFactor(), 0.0);
        assertEquals("degree", zenithAngleMetadata.getUnits());
        final SequenceData solarZenithAngles = dataRecord.getSequence("SOLAR_ZENITH_ANGLES");
        assertEquals(-77, solarZenithAngles.getByte(0));

        final SequenceData earthLocations = dataRecord.getSequence("EARTH_LOCATION");
        final CompoundData earthLocation = earthLocations.getCompound(0);
        final FormatMetadata latMetadata = (FormatMetadata) earthLocation.getType().getMember(0).getMetadata();
        final double latScaleFactor = latMetadata.getScalingFactor();
        assertEquals(1.0 / 128, latScaleFactor, 0.0);
        assertEquals("degrees north", latMetadata.getUnits());
        final FormatMetadata lonMetadata = (FormatMetadata) earthLocation.getType().getMember(1).getMetadata();
        final double lonScaleFactor = lonMetadata.getScalingFactor();
        assertEquals(1.0 / 128, lonScaleFactor, 0.0);
        assertEquals("degrees east", lonMetadata.getUnits());
        assertEquals(-7.9375, earthLocation.getShort("LAT") * latScaleFactor, 0.0);
        assertEquals(22.0859375, earthLocation.getShort("LON") * lonScaleFactor, 0.0);

        final SequenceData videoData = dataRecord.getSequence("VIDEO_DATA");
        assertEquals(0, videoData.getUInt(0) & 0b11000000000000000000000000000000);


        // second scan
        assertEquals(2, data.getCompound(1).getUShort("SCAN_LINE_NUMBER"));

        // last scan
        assertEquals(8422, data.getCompound(8421).getUShort("SCAN_LINE_NUMBER"));

        // last scan
        assertEquals(0, data.getCompound(8422).getUShort("SCAN_LINE_NUMBER"));
    }

    static String getString(CompoundData data, int index) throws IOException {
        return toString(data.getSequence(index));
    }

    static String toString(SequenceData sequence) throws IOException {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sequence.getElementCount(); i++) {
            builder.append((char) sequence.getUByte(i));
        }
        return builder.toString();
    }
}
