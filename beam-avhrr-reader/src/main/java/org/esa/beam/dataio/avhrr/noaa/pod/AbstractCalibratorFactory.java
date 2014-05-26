package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;

import java.io.IOException;

/**
* @author Ralf Quast
*/
abstract class AbstractCalibratorFactory implements CalibratorFactory {

    private final int channelIndex;
    private final CalibrationCoefficientsProvider provider;
    private final double interceptScaleFactor;
    private final double slopeScaleFactor;

    protected AbstractCalibratorFactory(int channelIndex, CalibrationCoefficientsProvider provider) {
        this.channelIndex = channelIndex;
        this.provider = provider;
        slopeScaleFactor = provider.getSlopeScaleFactor();
        interceptScaleFactor = provider.getInterceptScaleFactor();
    }

    @Override
    public final Calibrator createCalibrator(int i) throws IOException {
        final SequenceData coefficientsSequence = provider.getCalibrationCoefficients(i);
        final CompoundData coefficients = coefficientsSequence.getCompound(channelIndex);
        final double slope = coefficients.getInt(0) * slopeScaleFactor;
        final double intercept = coefficients.getInt(1) * interceptScaleFactor;

        return new CoefficientCalibrator(this, slope, intercept);
    }

    public final int getChannelIndex() {
        return channelIndex;
    }
}
