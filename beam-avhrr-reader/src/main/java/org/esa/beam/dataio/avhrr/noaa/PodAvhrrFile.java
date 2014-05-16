package org.esa.beam.dataio.avhrr.noaa;

import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

/**
 * @author Ralf Quast
 */
public class PodAvhrrFile extends AvhrrFile {

    @Override
    public void readHeader() throws IOException {

    }

    @Override
    public String getProductName() throws IOException {
        return null;
    }

    @Override
    public ProductData.UTC getStartDate() throws IOException {
        return null;
    }

    @Override
    public ProductData.UTC getEndDate() throws IOException {
        return null;
    }

    @Override
    public void addMetaData(MetadataElement metadataRoot) throws IOException {

    }

    @Override
    public BandReader createVisibleRadianceBandReader(int channel) throws IOException {
        return null;
    }

    @Override
    public BandReader createIrRadianceBandReader(int channel) throws IOException {
        return null;
    }

    @Override
    public BandReader createIrTemperatureBandReader(int channel) throws IOException {
        return null;
    }

    @Override
    public BandReader createReflectanceFactorBandReader(int channel) {
        return null;
    }

    @Override
    public BandReader createFlagBandReader() {
        return null;
    }

    @Override
    public boolean hasCloudBand() {
        return false;
    }

    @Override
    public BandReader createCloudBandReader() {
        return null;
    }

    @Override
    public String[] getTiePointNames() {
        return new String[0];
    }

    @Override
    public float[][] getTiePointData() throws IOException {
        return new float[0][];
    }

    @Override
    public int getScanLineOffset(int rawY) {
        return 0;
    }

    @Override
    public int getFlagOffset(int rawY) {
        return 0;
    }

    @Override
    public int getTiePointTrimX() {
        return 0;
    }

    @Override
    public int getTiePointSupsampling() {
        return 0;
    }

    @Override
    public void dispose() throws IOException {

    }

    public static boolean canDecode(File file) {
        FormatDetector formatDetector = null;
        try {
            formatDetector = new PodFormatDetector(file);
            return formatDetector.canDecode();
        } catch (Throwable e) {
            return false;
        } finally {
            if (formatDetector != null) {
                formatDetector.dispose();
            }
        }
    }

}
