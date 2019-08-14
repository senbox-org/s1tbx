package org.esa.s1tbx.commons.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.SystemUtils;

public class BandUtils {

    public static Band createBandFromVirtualBand(final VirtualBand band){
        final Band trgBand = new Band(band.getName(), band.getDataType(), band.getRasterWidth(), band.getRasterHeight());

        final ProductData data = band.createCompatibleRasterData(band.getRasterWidth(), band.getRasterHeight());
        trgBand.setRasterData(data);

        try {
            for (int y = 0; y < band.getRasterHeight(); y++) {
                float [] line = new float[band.getRasterWidth()];
                line = band.readPixels(0, y, band.getRasterWidth(), 1, line);
                trgBand.setPixels(0, y, line.length, 1, line);
            }

        } catch (Exception e){
            SystemUtils.LOG.severe(e.getMessage());
            e.printStackTrace();
        }
        return trgBand;
    }
}
