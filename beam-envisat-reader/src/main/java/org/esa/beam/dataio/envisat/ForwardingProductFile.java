package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

/**
 * To be reused by specializations of {@link ProductFile}.
 *
 * @author Ralf Quast
 */
class ForwardingProductFile extends ProductFile {

    protected ForwardingProductFile(File file) throws IOException {
        super(file, new FileImageInputStream(file));
    }

    protected ForwardingProductFile(File file, ImageInputStream iis) throws IOException {
        super(file, iis);
    }

    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    @Override
    public int getSceneRasterWidth() {
        return 0;
    }

    @Override
    public int getSceneRasterHeight() {
        return 0;
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
        return 0;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 0;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }

    @Override
    public BitmaskDef[] createDefaultBitmaskDefs(String flagDsName) {
        return new BitmaskDef[0];
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
}
