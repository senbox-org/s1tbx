package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.avhrr.BandReader;

/**
 * @author Ralf Quast
 */
class BandReaderFactory {

    static BandReader createCountBandReader(int channelIndex, VideoDataProvider videoDataProvider) {
        return new PodBandReader(channelIndex, videoDataProvider, new CountsCalibratorFactory(channelIndex));
    }

    static BandReader createAlbedoBandReader(int channelIndex, VideoDataProvider videoDataProvider,
                                             CalibrationCoefficientsProvider calibrationCoefficientsProvider) {
        return new PodBandReader(channelIndex, videoDataProvider,
                                 new AlbedoCalibratorFactory(channelIndex, calibrationCoefficientsProvider));
    }

    static BandReader createRadianceBandReader(int channelIndex, VideoDataProvider videoDataProvider,
                                               CalibrationCoefficientsProvider calibrationCoefficientsProvider) {
        return new PodBandReader(channelIndex, videoDataProvider,
                                 new RadianceCalibratorFactory(channelIndex, calibrationCoefficientsProvider));
    }
}
