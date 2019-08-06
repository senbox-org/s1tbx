
package org.csa.rstb.soilmoisture.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;

/**
 * Creates a dielectric model for soil moisture inversion.
 */
public class DielectricModelFactory {

    public static final String HALLIKAINEN = "Hallikainen";
    public static final String MIRONOV = "Mironov";

    public static DielectricModel createDielectricModel(final Operator op, final Product srcProduct,
                                                        final Product tgtProduct, final double invalidSMValue,
                                                        final double minSM, final double maxSM,
                                                        final Band smBand, final Band qualityIndexBand,
                                                        final String rdcBandName, final String modelName)
            throws OperatorException, IllegalArgumentException {

        switch (modelName) {

            case HALLIKAINEN:
                return new HallikainenDielectricModel(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);

            case MIRONOV:
                return new MironovDielectricModel(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);

            default:
                break;
        }

        return null;
    }
}


