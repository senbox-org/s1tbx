package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author Marco Peters
 */
public class AatsrAuxProductFile extends ProductFile {
    private static final String PARSE_SPH_ERR_MSG = "Failed to parse main header parameter '%s': %s";

    public AatsrAuxProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    public ProductData.UTC getSceneRasterStartTime() {
        try {
            return getMPH().getParamUTC(KEY_SENSING_START);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(String.format(PARSE_SPH_ERR_MSG, KEY_SENSING_START, e.getMessage()));
            return null;
        }
    }

    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        try {
            return getMPH().getParamUTC(KEY_SENSING_STOP);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(String.format(PARSE_SPH_ERR_MSG, KEY_SENSING_STOP, e.getMessage()));
            return null;
        }
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
        return 1;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 1;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return true;
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
}
