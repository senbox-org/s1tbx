package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class AuxProductFile extends ProductFile {

    public AuxProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    @Override
    public int getSceneRasterWidth() {
        return 360;
    }

    @Override
    public int getSceneRasterHeight() {
        return 180;
    }

    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return 1;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 1;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }

    @Override
    int getMappedMDSRIndex(int lineIndex) {
        return 179 - lineIndex;
    }

    @Override
    public Mask[] createDefaultMasks(String flagDsName) {
        return new Mask[0];
    }

    @Override
    void setInvalidPixelExpression(Band band) {
    }

    @Override
    public String getGADSName() {
        return null;
    }

    @Override
    public float[] getSpectralBandWavelengths() {
        return new float[0];
    }

    @Override
    public float[] getSpectralBandBandwidths() {
        return new float[0];
    }

    @Override
    public float[] getSpectralBandSolarFluxes() {
        return new float[0];
    }


    @Override
    protected void addCustomMetadata(Product product) throws IOException {
        try {
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 360, 180, -180, 90, 1.0, 1.0));
        } catch (FactoryException | TransformException e) {
            SystemUtils.LOG.log(Level.SEVERE, String.format("Could not create GeoCoding for product '%s'", product.getName()), e);
        }
    }

    @Override
    protected BandLineReader[] createBandLineReaders() {
        BandLineReader[] tempReaders = super.createBandLineReaders();
        BandLineReader[] readers = new BandLineReader[tempReaders.length];
        for (int i = 0; i < tempReaders.length; i++) {
            readers[i] = new AuxBandLineReader(tempReaders[i]);
        }
        return readers;
    }

    private static final class AuxBandLineReader extends BandLineReader {

        AuxBandLineReader(BandLineReader reader) {
            super(reader.getBandInfo(), reader.getPixelDataReader(), getFieldIndex(reader));

        }

        private static int getFieldIndex(BandLineReader reader) {
            RecordReader pixelDataReader = reader.getPixelDataReader();
            Record pixelDataRecord = pixelDataReader.createCompatibleRecord();
            Field pixelDatafield = reader.getPixelDataField();
            return pixelDataRecord.getFieldIndex(pixelDatafield.getName());
        }

        @Override
        public boolean isTiePointBased() {
            return false;
        }
    }
}
