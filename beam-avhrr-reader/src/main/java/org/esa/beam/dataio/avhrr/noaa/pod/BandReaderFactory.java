package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.avhrr.BandReader;

/**
 * @author Ralf Quast
 */
class BandReaderFactory {

    static BandReader createCountBandReader(int channelIndex, VideoDataProvider videoDataProvider,
                                            Validator validator) {
        return new PodBandReader(channelIndex, videoDataProvider, validator, new CountsCalibratorFactory(channelIndex));
    }

    static BandReader createAlbedoBandReader(int channelIndex, VideoDataProvider videoDataProvider, Validator validator,
                                             CalibrationCoefficientsProvider calibrationCoefficientsProvider) {
        return new PodBandReader(channelIndex, videoDataProvider, validator,
                                 new AlbedoCalibratorFactory(channelIndex, calibrationCoefficientsProvider));
    }

    static BandReader createRadianceBandReader(int channelIndex, VideoDataProvider videoDataProvider,
                                               Validator validator,
                                               CalibrationCoefficientsProvider calibrationCoefficientsProvider) {
        return new PodBandReader(channelIndex, videoDataProvider, validator,
                                 new RadianceCalibratorFactory(channelIndex, calibrationCoefficientsProvider));
    }
}
