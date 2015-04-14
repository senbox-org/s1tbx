package org.esa.snap.dataio.envisat;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Represents the MERIS 'MER_RAC_AX' radiometric calibration auxiliary data product,
 * which is used for the MERIS Level 1 radiometric calibration.
 *
 * @author Ralf Quast
 */
public class MerisRacProductFile extends ForwardingProductFile {

    public MerisRacProductFile(ImageInputStream inputStream) throws IOException {
        super(null, inputStream);
    }

    @Override
    public String getProductType() {
        return getProductId().substring(0, EnvisatConstants.PRODUCT_TYPE_STRLEN);
    }
}
