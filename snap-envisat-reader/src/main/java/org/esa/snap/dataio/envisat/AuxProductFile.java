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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
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
        switch (getProductType()) {
            case "AUX_LSM_AX":
                return 360;
            case "AUX_DEM_AX":
                return 2160;
            default:
                return 0;
        }
    }

    @Override
    public int getSceneRasterHeight() {
        switch (getProductType()) {
            case "AUX_LSM_AX":
                return 180;
            case "AUX_DEM_AX":
                return 4320;
            default:
                return 0;
        }
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
    int getMappedMDSRIndex(int lineIndex) {
        if ("AUX_LSM_AX".equals(getProductType())) {
            return (getSceneRasterHeight() - 1) - lineIndex;
        } else {
            return super.getMappedMDSRIndex(lineIndex);
        }
    }

    @Override
    protected void addCustomMetadata(Product product) throws IOException {
        try {
            int width = getSceneRasterWidth();
            int height = getSceneRasterHeight();
            switch (getProductType()) {
                case "AUX_LSM_AX":
                    product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 1.0, 1.0));
                    break;
                case "AUX_DEM_AX":
                    product.setSceneGeoCoding(creatDemGeoCoding(width, height));
                    break;
            }
        } catch (FactoryException | TransformException e) {
            SystemUtils.LOG.log(Level.SEVERE, String.format("Could not create GeoCoding for product '%s'", product.getName()), e);
        }
    }

    static CrsGeoCoding creatDemGeoCoding(int width, int height) throws FactoryException, TransformException {
        AffineTransform i2m = new AffineTransform();
        i2m.rotate(Math.toRadians(90));
        final int easting = 180;
        final int northing = -90;
        i2m.translate(northing, easting);
        double sizeX = 360.0 / height;
        double sizeY = 180.0 / width;
        i2m.scale(sizeX, -sizeY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(width, height), i2m);
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
